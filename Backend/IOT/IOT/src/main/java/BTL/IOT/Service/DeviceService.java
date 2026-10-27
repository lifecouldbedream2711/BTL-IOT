package BTL.IOT.Service;

import BTL.IOT.Dto.response.DeviceDto;
import BTL.IOT.Entity.Action;
import BTL.IOT.Entity.Device;
import BTL.IOT.Event.EspOnlineEvent;
import BTL.IOT.Exception.AppException;
import BTL.IOT.Exception.ErrorCode;
import BTL.IOT.Repository.ActionRepository;
import BTL.IOT.Repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final ActionRepository actionRepository;
    private final MqttService mqttService;

    public List<DeviceDto> getAllDevices() {
        List<Device> devices = deviceRepository.findAll();
        List<DeviceDto> dtoList = new ArrayList<>();
        boolean isOnline = mqttService.isEspOnline();

        for (Device d : devices) {
            Optional<Action> lastAction = actionRepository.findTopByDeviceIdOrderByTimeDesc(d.getId());

            boolean isOn = false;
            String status = "OFF";

            if (lastAction.isPresent()) {
                String actStatus = lastAction.get().getStatus();
                if ("PENDING".equalsIgnoreCase(actStatus)) {
                    isOn = false;
                    status = "PENDING";
                } else if (isOnline && "ON".equalsIgnoreCase(actStatus)) {
                    isOn = true;
                    status = "ON";
                } else {
                    isOn = false;
                    status = "OFF";
                }
            }

            String strId = switch (d.getId()) {
                case 1 -> "fan";
                case 2 -> "light";
                case 3 -> "ac";
                default -> String.valueOf(d.getId());
            };

            dtoList.add(DeviceDto.builder()
                    .id(strId)
                    .deviceId(d.getId())
                    .name(d.getName())
                    .on(isOn)
                    .status(status)
                    .time(lastAction.map(Action::getTime).orElse(d.getCreatedAt()))
                    .build());
        }

        return dtoList;
    }

    public DeviceDto controlDevice(String idOrName, String requestedAction) {
        Integer deviceId = resolveDeviceId(idOrName);
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));

        Optional<Action> lastAction = actionRepository.findTopByDeviceIdOrderByTimeDesc(deviceId);
        boolean currentlyOn = mqttService.isEspOnline() && lastAction.map(a -> "ON".equalsIgnoreCase(a.getStatus())).orElse(false);

        String newStatus;
        if (requestedAction == null || requestedAction.isBlank() || "TOGGLE".equalsIgnoreCase(requestedAction)) {
            newStatus = currentlyOn ? "OFF" : "ON";
        } else if ("ON".equalsIgnoreCase(requestedAction)) {
            newStatus = "ON";
        } else {
            newStatus = "OFF";
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Luôn lưu bản ghi với Status = "PENDING" lúc gửi lệnh
        Action newAction = actionRepository.save(Action.builder()
                .deviceId(deviceId)
                .action(newStatus)
                .status("PENDING")
                .time(now)
                .build());

        String strId = switch (deviceId) {
            case 1 -> "fan";
            case 2 -> "light";
            case 3 -> "ac";
            default -> String.valueOf(deviceId);
        };

        // Nếu ESP đang mất kết nối / offline:
        // Lệnh được giữ lại ở trạng thái PENDING trong Database để khi ESP online sẽ tự động tiếp tục
        if (!mqttService.isEspOnline()) {
            log.warn("ESP is offline! Command for device {} ({}) saved as PENDING in DB. Will execute automatically when ESP reconnects.", device.getName(), deviceId);
            return DeviceDto.builder()
                    .id(strId)
                    .deviceId(deviceId)
                    .name(device.getName())
                    .on(false)
                    .status("PENDING")
                    .time(now)
                    .build();
        }

        // 2. Nếu ESP online: gửi lệnh qua MQTT và chờ ESP phản hồi xác nhận (tối đa 2.5s)
        String topic = switch (deviceId) {
            case 1 -> "esp8266/led1";
            case 2 -> "esp8266/led2";
            case 3 -> "esp8266/led3";
            default -> "esp8266/led" + deviceId;
        };

        String ackKey = "led" + deviceId;
        CompletableFuture<String> ackFuture = mqttService.registerAck(ackKey);

        mqttService.publish(topic, newStatus);
        log.info("Sent control command for {} ({}) to {}. Waiting for ESP feedback...", device.getName(), deviceId, newStatus);

        boolean success = false;
        try {
            String ackMsg = ackFuture.get(2500, TimeUnit.MILLISECONDS);
            if (ackMsg != null && ackMsg.toLowerCase().contains("successfully")) {
                success = true;
            }
        } catch (TimeoutException e) {
            log.warn("ESP did not respond within 2.5s for device {}! Status marked as FAILED.", device.getName());
        } catch (Exception e) {
            log.error("Error waiting for ESP ack: {}", e.getMessage());
        } finally {
            mqttService.removeAck(ackKey);
        }

        if (success) {
            newAction.setStatus(newStatus);
        } else {
            newAction.setStatus("FAILED");
        }
        actionRepository.save(newAction);

        return DeviceDto.builder()
                .id(strId)
                .deviceId(deviceId)
                .name(device.getName())
                .on(success && "ON".equalsIgnoreCase(newStatus))
                .status(success ? newStatus : (newAction.getStatus()))
                .time(newAction.getTime())
                .build();
    }

    @EventListener
    public void onEspOnline(EspOnlineEvent event) {
        CompletableFuture.runAsync(this::syncOnEspConnected);
    }

    public synchronized void syncOnEspConnected() {
        log.info("ESP came online! Synchronizing device states and executing pending actions...");

        // 1. Lấy tất cả các lệnh PENDING đã lưu trong thời gian ESP offline
        List<Action> pendingActions = actionRepository.findByStatusOrderByTimeAsc("PENDING");
        Set<Integer> processedDeviceIds = new HashSet<>();

        if (!pendingActions.isEmpty()) {
            log.info("Found {} pending action(s) to process.", pendingActions.size());
            for (Action a : pendingActions) {
                Integer deviceId = a.getDeviceId();
                String targetAction = a.getAction() != null ? a.getAction().toUpperCase() : "OFF";
                String topic = switch (deviceId) {
                    case 1 -> "esp8266/led1";
                    case 2 -> "esp8266/led2";
                    case 3 -> "esp8266/led3";
                    default -> "esp8266/led" + deviceId;
                };

                String ackKey = "led" + deviceId;
                CompletableFuture<String> ackFuture = mqttService.registerAck(ackKey);

                mqttService.publish(topic, targetAction);
                log.info("Executing pending action #{} for device {} -> {}", a.getId(), deviceId, targetAction);

                boolean success = false;
                try {
                    String ack = ackFuture.get(2500, TimeUnit.MILLISECONDS);
                    if (ack != null && ack.toLowerCase().contains("successfully")) {
                        success = true;
                    }
                } catch (Exception e) {
                    log.warn("Timeout/Error waiting for ACK on pending action #{}: {}", a.getId(), e.getMessage());
                } finally {
                    mqttService.removeAck(ackKey);
                }

                if (success) {
                    a.setStatus(targetAction);
                    log.info("Pending action #{} for device {} completed successfully -> {}", a.getId(), deviceId, targetAction);
                } else {
                    a.setStatus("FAILED");
                    log.warn("Pending action #{} for device {} marked as FAILED", a.getId());
                }
                actionRepository.save(a);
                processedDeviceIds.add(deviceId);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
        }

        // 2. Với các thiết bị không có pending action:
        // Khôi phục lại trạng thái cũ từ Database (để thiết bị về trạng thái cũ trước khi bị tắt)
        for (int devId = 1; devId <= 3; devId++) {
            if (!processedDeviceIds.contains(devId)) {
                Optional<Action> lastAction = actionRepository.findTopByDeviceIdAndStatusNotOrderByTimeDesc(devId, "PENDING");
                String stateToRestore = "OFF";
                if (lastAction.isPresent()) {
                    String s = lastAction.get().getStatus();
                    if ("ON".equalsIgnoreCase(s)) {
                        stateToRestore = "ON";
                    }
                }

                String topic = switch (devId) {
                    case 1 -> "esp8266/led1";
                    case 2 -> "esp8266/led2";
                    case 3 -> "esp8266/led3";
                    default -> "esp8266/led" + devId;
                };

                mqttService.publish(topic, stateToRestore);
                log.info("Restored device {} ({}) to previous state: {}", devId, topic, stateToRestore);
            }
        }
    }

    private Integer resolveDeviceId(String idOrName) {
        if (idOrName == null || idOrName.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        String clean = idOrName.trim().toLowerCase();
        return switch (clean) {
            case "fan", "1" -> 1;
            case "light", "2" -> 2;
            case "ac", "air conditioner", "3" -> 3;
            default -> {
                try {
                    yield Integer.parseInt(clean);
                } catch (NumberFormatException e) {
                    throw new AppException(ErrorCode.DEVICE_NOT_FOUND);
                }
            }
        };
    }
}
