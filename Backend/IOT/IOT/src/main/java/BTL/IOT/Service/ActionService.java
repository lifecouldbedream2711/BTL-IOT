package BTL.IOT.Service;

import BTL.IOT.Dto.response.ActionRecordDto;
import BTL.IOT.Dto.response.PageResult;
import BTL.IOT.Entity.Action;
import BTL.IOT.Repository.ActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ActionService {

    private final ActionRepository actionRepository;

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"};
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String formatDisplayTime(LocalDateTime dt) {
        if (dt == null) return "";
        int monthIdx = dt.getMonthValue() - 1;
        String month = (monthIdx >= 0 && monthIdx < 12) ? MONTHS[monthIdx] : dt.getMonth().name();
        return String.format("%d %s %d, %s", dt.getDayOfMonth(), month, dt.getYear(), dt.format(TIME_FMT));
    }

    private String getDeviceName(Integer deviceId) {
        if (deviceId == null) return "Unknown";
        return switch (deviceId) {
            case 1 -> "Fan";
            case 2 -> "Light";
            case 3 -> "Air Conditioner";
            default -> "Device #" + deviceId;
        };
    }

    private Integer mapDeviceNameToId(String deviceName) {
        if (deviceName == null || deviceName.equalsIgnoreCase("All Devices") || deviceName.isBlank()) {
            return null;
        }
        return switch (deviceName.toLowerCase()) {
            case "fan" -> 1;
            case "light" -> 2;
            case "air conditioner", "ac" -> 3;
            default -> null;
        };
    }

    public PageResult<ActionRecordDto> searchActions(String device, String status, String actionStatus, String timeStr, int page, int pageSize) {
        Integer deviceId = mapDeviceNameToId(device);
        String cleanStatus = (status == null || status.equalsIgnoreCase("All Status") || status.isBlank()) ? null : status;
        
        String cleanActionStatus = (actionStatus == null || actionStatus.equalsIgnoreCase("All Action Status") || actionStatus.isBlank()) ? null : actionStatus.trim();
        String cleanTime = (timeStr == null || timeStr.isBlank()) ? null : timeStr.trim();

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize);
        Page<Action> pageData = actionRepository.searchActions(deviceId, cleanStatus, cleanActionStatus, cleanTime, pageable);

        var list = pageData.getContent().stream()
                .map(a -> {
                    long epochMilli = a.getTime() != null ? a.getTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L;
                    String act = "Success";
                    String displayStatus = a.getStatus() != null ? a.getStatus().toUpperCase() : "OFF";

                    if ("PENDING".equalsIgnoreCase(a.getStatus())) {
                        act = "Pending";
                        displayStatus = "PENDING";
                    } else if ("FAILED".equalsIgnoreCase(a.getStatus())) {
                        act = "Failed";
                        displayStatus = a.getAction() != null ? a.getAction().toUpperCase() : "OFF";
                    }

                    return ActionRecordDto.builder()
                            .id(a.getId())
                            .device(getDeviceName(a.getDeviceId()))
                            .deviceId(a.getDeviceId())
                            .status(displayStatus)
                            .action(act)
                            .ts(epochMilli)
                            .time(formatDisplayTime(a.getTime()))
                            .rawTime(a.getTime())
                            .build();
                })
                .toList();

        return PageResult.<ActionRecordDto>builder()
                .items(list)
                .total(pageData.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .totalPages(pageData.getTotalPages())
                .build();
    }
}
