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
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final ActionRepository actionRepository;
    private final MqttService mqttService;
    private final AtomicBoolean isProcessingPending = new AtomicBoolean(false);

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
     * Chạy định kỳ mỗi 1 giây:
     * 1. Hủy các lệnh PENDING đã quá 30 giây -> đánh dấu FAILED (thất bại).
     * 2. Nếu ESP đang Online và có lệnh PENDING trong vòng 30 giây -> tự động gửi xuống ESP thực thi!
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 1000)
    public void processPendingQueueAndTimeout() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtySecondsAgo = now.minusSeconds(30);

        // 1. Quét và hủy các lệnh quá 30s
        List<Action> expiredPending = actionRepository.findByStatusAndTimeBefore("PENDING", thirtySecondsAgo);
        if (!expiredPending.isEmpty()) {
            for (Action a : expiredPending) {
                a.setStatus("FAILED");
                actionRepository.save(a);
                log.warn("Action #{} (device {}) exceeded 30s timeout without ESP confirmation -> marked as FAILED (retaining previous state on FE).",
                        a.getId(), a.getDeviceId());
            }
        }

        // 2. Nếu ESP đang Online và có lệnh PENDING (dưới 30s) -> thực thi ngay!
        if (mqttService.isEspOnline()) {
            List<Action> validPending = actionRepository.findByStatusOrderByTimeAsc("PENDING");
            if (!validPending.isEmpty() && isProcessingPending.compareAndSet(false, true)) {
                CompletableFuture.runAsync(() -> {
                    try {
                        executePendingActions(validPending);
                    } finally {
                        isProcessingPending.set(false);
                    }
                });
            }
        }
    }

    @EventListener
    public void onEspOnline(EspOnlineEvent event) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(600); // Đợi 600ms để ESP hoàn tất subscribe các topic
            } catch (InterruptedException ignored) {}
            syncOnEspConnected();
        });
    }

    public synchronized void syncOnEspConnected() {
        log.info("ESP came online! Synchronizing device states and processing pending actions...");
        List<Action> pendingActions = actionRepository.findByStatusOrderByTimeAsc("PENDING");
        Set<Integer> processedDeviceIds = new HashSet<>();

        if (!pendingActions.isEmpty()) {
            executePendingActions(pendingActions);
            for (Action a : pendingActions) {
                processedDeviceIds.add(a.getDeviceId());
            }
        }

        // Với các thiết bị không có pending action:
        // Khôi phục lại trạng thái cũ đã xác nhận từ Database
        for (int devId = 1; devId <= 3; devId++) {
            if (!processedDeviceIds.contains(devId)) {
                Optional<Action> lastConfirmed = actionRepository.findTopByDeviceIdAndStatusInOrderByTimeDesc(
                        devId, List.of("ON", "OFF")
                );
                String stateToRestore = "OFF";
                if (lastConfirmed.isPresent() && "ON".equalsIgnoreCase(lastConfirmed.get().getStatus())) {
                    stateToRestore = "ON";
                }

                String topic = switch (devId) {
                    case 1 -> "esp8266/led1";
                    case 2 -> "esp8266/led2";
                    case 3 -> "esp8266/led3";
                    default -> "esp8266/led" + devId;
                };

                mqttService.publish(topic, stateToRestore);
                log.info("Restored device {} ({}) to last confirmed state: {}", devId, topic, stateToRestore);
            }
        }
    }

    private void executePendingActions(List<Action> actions) {
        LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);

        for (Action a : actions) {
            // Kiểm tra lại thời gian: nếu đã quá 30s thì hủy
            if (a.getTime() != null && a.getTime().isBefore(thirtySecondsAgo)) {
                a.setStatus("FAILED");
                actionRepository.save(a);
                log.warn("Action #{} expired (>30s) -> marked FAILED.", a.getId());
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
            log.info("Sending pending action #{} to device {} ({}) -> {}", a.getId(), deviceId, topic, targetAction);

            boolean success = false;
            try {
                String ack = ackFuture.get(2500, TimeUnit.MILLISECONDS);
                if (ack != null && ack.toLowerCase().contains("successfully")) {
                    success = true;
                }
            } catch (Exception e) {
                log.warn("Pending action #{} did not receive ACK in 2.5s: {}", a.getId(), e.getMessage());
            } finally {
                mqttService.removeAck(ackKey);
            }

            if (success) {
                a.setStatus(targetAction);
                actionRepository.save(a);
                log.info("Pending action #{} executed successfully on ESP! Status updated to {}", a.getId(), targetAction);
            } else {
                // TUYỆT ĐỐI KHÔNG ĐÁNH DẤU FAILED KHI CHƯA QUÁ 30S!
                // Giữ nguyên PENDING để chờ tiếp đến khi đủ 30s mới hủy.
                log.info("Pending action #{} awaiting confirmation, remaining PENDING until 30s timeout.", a.getId());
            }

            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {}
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
