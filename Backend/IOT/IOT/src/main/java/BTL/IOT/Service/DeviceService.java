package BTL.IOT.Service;

import BTL.IOT.Dto.response.DeviceDto;
import BTL.IOT.Entity.Action;
import BTL.IOT.Entity.Device;
import BTL.IOT.Exception.AppException;
import BTL.IOT.Exception.ErrorCode;
import BTL.IOT.Repository.ActionRepository;
import BTL.IOT.Repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

            // Nếu ESP mất kết nối / reset, các đèn trên thực tế đều tắt, nên luôn set isOn = false
            boolean isOn = false;
            String status = "OFF";

            if (isOnline && lastAction.isPresent()) {
                String actStatus = lastAction.get().getStatus();
                if ("ON".equalsIgnoreCase(actStatus)) {
                    isOn = true;
                    status = "ON";
                } else if ("PENDING".equalsIgnoreCase(actStatus)) {
                    isOn = false;
                    status = "PENDING";
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
        if (!mqttService.isEspOnline()) {
            log.warn("ESP is offline/reset! Command for device {} ({}) will remain PENDING and state stays OFF (false).", device.getName(), deviceId);
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
            log.warn("ESP did not respond within 2.5s for device {}! Status remains PENDING.", device.getName());
        } catch (Exception e) {
            log.error("Error waiting for ESP ack: {}", e.getMessage());
        } finally {
            mqttService.removeAck(ackKey);
        }

        if (success) {
            // ESP phản hồi thành công -> cập nhật Action thành ON hoặc OFF thực tế
            newAction.setStatus(newStatus);
            actionRepository.save(newAction);
        }

        return DeviceDto.builder()
                .id(strId)
                .deviceId(deviceId)
                .name(device.getName())
                .on(success && "ON".equalsIgnoreCase(newStatus))
                .status(success ? newStatus : "PENDING")
                .time(newAction.getTime())
                .build();
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
