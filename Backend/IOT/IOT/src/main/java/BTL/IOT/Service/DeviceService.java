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
        LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);

        for (Device d : devices) {
            // 1. Lấy trạng thái thành công gần nhất (ON hoặc OFF) trước đó của thiết bị
            Optional<Action> lastConfirmed = actionRepository.findTopByDeviceIdAndStatusInOrderByTimeDesc(
                    d.getId(), List.of("ON", "OFF")
            );
            boolean previousIsOn = lastConfirmed.map(a -> "ON".equalsIgnoreCase(a.getStatus())).orElse(false);

            // 2. Lấy bản ghi mới nhất của thiết bị
            Optional<Action> lastAction = actionRepository.findTopByDeviceIdOrderByTimeDesc(d.getId());

            boolean currentIsOn = previousIsOn;
            String currentStatus = previousIsOn ? "ON" : "OFF";

            if (lastAction.isPresent()) {
                Action a = lastAction.get();
                String actStatus = a.getStatus();

                if ("PENDING".equalsIgnoreCase(actStatus)) {
                    // Chờ đúng 30 giây: nếu quá 30 giây chưa update được trạng thái thì mới đánh FAILED
                    if (a.getTime() != null && a.getTime().isBefore(thirtySecondsAgo)) {
                        a.setStatus("FAILED");
                        actionRepository.save(a);
                        // Khi bị fail: giữ nguyên status trước đó trên FE
                        currentIsOn = previousIsOn;
                        currentStatus = previousIsOn ? "ON" : "OFF";
                    } else {
                        // Vẫn trong 30 giây chờ: giữ nguyên status trước đó trên FE
                        currentIsOn = previousIsOn;
                        currentStatus = "PENDING";
                    }
                } else if ("FAILED".equalsIgnoreCase(actStatus)) {
                    // Khi bị fail: giữ nguyên status trước đó trên FE
                    currentIsOn = previousIsOn;
                    currentStatus = previousIsOn ? "ON" : "OFF";
                } else if ("ON".equalsIgnoreCase(actStatus)) {
                    currentIsOn = true;
                    currentStatus = "ON";
                } else if ("OFF".equalsIgnoreCase(actStatus)) {
                    currentIsOn = false;
                    currentStatus = "OFF";
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
                    .on(currentIsOn)
                    .status(currentStatus)
                    .time(lastAction.map(Action::getTime).orElse(d.getCreatedAt()))
                    .build());
        }

        return dtoList;
    }

    public DeviceDto controlDevice(String idOrName, String requestedAction) {
        Integer deviceId = resolveDeviceId(idOrName);
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));

        // Lấy trạng thái thành công gần nhất (ON/OFF) đã xác nhận trước đó
        Optional<Action> lastConfirmed = actionRepository.findTopByDeviceIdAndStatusInOrderByTimeDesc(
                deviceId, List.of("ON", "OFF")
        );
        boolean currentlyOn = lastConfirmed.map(a -> "ON".equalsIgnoreCase(a.getStatus())).orElse(false);

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
        // Lệnh được giữ lại ở trạng thái PENDING trong Database tối đa 30 giây.
        // Trên FE: Giữ nguyên status trước đó (currentlyOn)
        if (!mqttService.isEspOnline()) {
            log.warn("ESP is offline! Command for device {} ({}) saved as PENDING in DB. Will wait 30s. Retaining previous status ({}) on FE.",
                    device.getName(), deviceId, currentlyOn ? "ON" : "OFF");
            return DeviceDto.builder()
                    .id(strId)
                    .deviceId(deviceId)
                    .name(device.getName())
                    .on(currentlyOn) // GIỮ NGUYÊN STATUS TRƯỚC ĐÓ TRÊN FE!
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
            log.warn("ESP did not respond within 2.5s for device {}. Command remains PENDING for up to 30 seconds.", device.getName());
        } catch (Exception e) {
            log.error("Error waiting for ESP ack: {}", e.getMessage());
        } finally {
            mqttService.removeAck(ackKey);
        }

        if (success) {
            newAction.setStatus(newStatus);
            actionRepository.save(newAction);
            return DeviceDto.builder()
                    .id(strId)
                    .deviceId(deviceId)
                    .name(device.getName())
                    .on("ON".equalsIgnoreCase(newStatus))
                    .status(newStatus)
                    .time(newAction.getTime())
                    .build();
        } else {
            // Chưa có phản hồi: giữ trạng thái PENDING tối đa 30 giây.
            // Trên FE: GIỮ NGUYÊN STATUS TRƯỚC ĐÓ!
            log.info("Action #{} for device {} remaining PENDING (waiting 30 seconds). Retaining previous status ({}) on FE.",
                    newAction.getId(), deviceId, currentlyOn ? "ON" : "OFF");
            return DeviceDto.builder()
                    .id(strId)
                    .deviceId(deviceId)
                    .name(device.getName())
                    .on(currentlyOn) // GIỮ NGUYÊN STATUS TRƯỚC ĐÓ TRÊN FE!
                    .status("PENDING")
                    .time(newAction.getTime())
                    .build();
        }
    }

    /**
     * Tự động quét các lệnh PENDING định kỳ:
     * Chờ 30 giây, nếu chưa update được trạng thái thì mới đánh FAILED.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 2000)
    public void checkPendingActionsTimeout() {
        LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);
        List<Action> expiredPending = actionRepository.findByStatusAndTimeBefore("PENDING", thirtySecondsAgo);

        if (!expiredPending.isEmpty()) {
            for (Action a : expiredPending) {
                a.setStatus("FAILED");
                actionRepository.save(a);
                log.warn("Action #{} (device {}) exceeded 30 seconds timeout without state update -> marked as FAILED (retaining previous confirmed state on FE).",
                        a.getId(), a.getDeviceId());
            }
        }
    }

    @EventListener
    public void onEspOnline(EspOnlineEvent event) {
        CompletableFuture.runAsync(this::syncOnEspConnected);
    }

    public synchronized void syncOnEspConnected() {
        log.info("ESP came online! Synchronizing device states and executing pending actions...");

        LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);
        // 1. Lấy tất cả các lệnh PENDING đã lưu trong thời gian ESP offline
        List<Action> pendingActions = actionRepository.findByStatusOrderByTimeAsc("PENDING");
        Set<Integer> processedDeviceIds = new HashSet<>();

        if (!pendingActions.isEmpty()) {
            log.info("Found {} pending action(s) to process.", pendingActions.size());
            for (Action a : pendingActions) {
                // Nếu lệnh PENDING đã quá 30 giây mà chưa update được trạng thái -> đánh dấu FAILED
                if (a.getTime() != null && a.getTime().isBefore(thirtySecondsAgo)) {
                    a.setStatus("FAILED");
                    actionRepository.save(a);
                    log.warn("Pending action #{} expired (> 30 seconds) -> marked FAILED.", a.getId());
                    continue;
                }

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
