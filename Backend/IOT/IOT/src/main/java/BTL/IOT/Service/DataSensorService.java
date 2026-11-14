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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DataSensorService {

    private final DataSensorRepository dataSensorRepository;

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String formatDisplayTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DISPLAY_FMT);
    }

    private String getSensorTypeName(Integer sensorId) {
        if (sensorId == null) return "Unknown";
        return switch (sensorId) {
            case 1 -> "Temperature";
            case 2 -> "Humidity";
            case 3 -> "Light";
            case 4 -> "CO2";
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
            case "co2" -> 4;
            default -> null;
        };
    }

    public record ParsedTimeFilter(LocalDateTime startTime, LocalDateTime endTime, String timeStr) {}

    public ParsedTimeFilter parseTimeFilter(String timeInput) {
        if (timeInput == null || timeInput.isBlank()) {
            return new ParsedTimeFilter(null, null, null);
        }

        String raw = timeInput.trim();

        // 1. Check full datetime with second
        DateTimeFormatter[] secondFormatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };

        for (DateTimeFormatter fmt : secondFormatters) {
            try {
                LocalDateTime dt = LocalDateTime.parse(raw, fmt);
                LocalDateTime start = dt.withNano(0);
                return new ParsedTimeFilter(start, start.plusSeconds(1), null);
            } catch (Exception ignored) {}
        }

        // 2. Check datetime down to minute
        DateTimeFormatter[] minuteFormatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
        };

        for (DateTimeFormatter fmt : minuteFormatters) {
            try {
                LocalDateTime dt = LocalDateTime.parse(raw, fmt);
                LocalDateTime start = dt.withSecond(0).withNano(0);
                return new ParsedTimeFilter(start, start.plusMinutes(1), null);
            } catch (Exception ignored) {}
        }

        // 3. Check date only
        DateTimeFormatter[] dateFormatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };

        for (DateTimeFormatter fmt : dateFormatters) {
            try {
                LocalDate d = LocalDate.parse(raw, fmt);
                LocalDateTime start = d.atStartOfDay();
                return new ParsedTimeFilter(start, start.plusDays(1), null);
            } catch (Exception ignored) {}
        }

        // 4. Fallback pattern for partial or time-only (e.g. HH:mm:ss)
        return new ParsedTimeFilter(null, null, raw.replace('T', ' '));
    }

    public PageResult<SensorDataRecordDto> searchDataSensors(String sensorType, String keyword, int page, int pageSize) {
        return searchDataSensors(sensorType, keyword, null, page, pageSize);
    }

    public PageResult<SensorDataRecordDto> searchDataSensors(String sensorType, String keyword, String time, int page, int pageSize) {
        Integer sensorId = mapTypeNameToId(sensorType);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize);

        String cleanKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String cleanTime = (time == null || time.isBlank()) ? null : time.trim();

        // If time is not explicitly specified, but keyword is a parsed datetime, treat keyword as time
        if (cleanTime == null && cleanKeyword != null) {
            ParsedTimeFilter tryParse = parseTimeFilter(cleanKeyword);
            if (tryParse.startTime() != null) {
                cleanTime = cleanKeyword;
                cleanKeyword = null;
            }
        }

        ParsedTimeFilter timeFilter = parseTimeFilter(cleanTime);

        Page<DataSensor> pageData = dataSensorRepository.searchDataSensors(
                sensorId,
                timeFilter.startTime(),
                timeFilter.endTime(),
                timeFilter.timeStr(),
                cleanKeyword,
                pageable
        );

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
