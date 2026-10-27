package BTL.IOT.Service;

import BTL.IOT.Dto.response.PageResult;
import BTL.IOT.Dto.response.SensorDataRecordDto;
import BTL.IOT.Entity.DataSensor;
import BTL.IOT.Repository.DataSensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DataSensorService {

    private final DataSensorRepository dataSensorRepository;

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"};
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private String formatDisplayTime(LocalDateTime dt) {
        if (dt == null) return "";
        int monthIdx = dt.getMonthValue() - 1;
        String month = (monthIdx >= 0 && monthIdx < 12) ? MONTHS[monthIdx] : dt.getMonth().name();
        return String.format("%d %s %d, %s", dt.getDayOfMonth(), month, dt.getYear(), dt.format(TIME_FMT));
    }

    private String getSensorTypeName(Integer sensorId) {
        if (sensorId == null) return "Unknown";
        return switch (sensorId) {
            case 1 -> "Temperature";
            case 2 -> "Humidity";
            case 3 -> "Light";
            default -> "Sensor #" + sensorId;
        };
    }

    private Integer mapTypeNameToId(String type) {
        if (type == null || type.equalsIgnoreCase("All") || type.isBlank()) {
            return null;
        }
        return switch (type.toLowerCase()) {
            case "temperature" -> 1;
            case "humidity" -> 2;
            case "light" -> 3;
            default -> null;
        };
    }

    public PageResult<SensorDataRecordDto> searchDataSensors(String sensorType, String keyword, int page, int pageSize) {
        Integer sensorId = mapTypeNameToId(sensorType);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize);

        Page<DataSensor> pageData = dataSensorRepository.searchDataSensors(sensorId, keyword != null ? keyword.trim() : null, pageable);

        var list = pageData.getContent().stream()
                .map(d -> SensorDataRecordDto.builder()
                        .id(d.getId())
                        .type(getSensorTypeName(d.getSensorId()))
                        .value(d.getValue())
                        .time(formatDisplayTime(d.getTime()))
                        .rawTime(d.getTime())
                        .build())
                .toList();

        return PageResult.<SensorDataRecordDto>builder()
                .items(list)
                .total(pageData.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .totalPages(pageData.getTotalPages())
                .build();
    }
}
