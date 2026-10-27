package BTL.IOT.Service;

import BTL.IOT.Dto.response.ChartPointResponse;
import BTL.IOT.Dto.response.SensorStatusResponse;
import BTL.IOT.Entity.DataSensor;
import BTL.IOT.Repository.DataSensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SensorService {

    private final DataSensorRepository dataSensorRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH);

    public SensorStatusResponse getLatestSensorStatus() {
        Optional<DataSensor> tempSensor = dataSensorRepository.findTopBySensorIdOrderByTimeDesc(1);
        Optional<DataSensor> humSensor = dataSensorRepository.findTopBySensorIdOrderByTimeDesc(2);
        Optional<DataSensor> lightSensor = dataSensorRepository.findTopBySensorIdOrderByTimeDesc(3);

        Double temp = tempSensor.map(s -> s.getValue().doubleValue()).orElse(28.0);
        Double hum = humSensor.map(s -> s.getValue().doubleValue()).orElse(65.0);
        Integer light = lightSensor.map(s -> s.getValue().intValue()).orElse(450);

        LocalDateTime latestTime = tempSensor.map(DataSensor::getTime)
                .orElse(LocalDateTime.now());

        return SensorStatusResponse.builder()
                .temp(temp)
                .humidity(hum)
                .light(light)
                .time(latestTime)
                .build();
    }

    public List<ChartPointResponse> getChartPoints(int limit) {
        List<DataSensor> temps = dataSensorRepository.findTop15BySensorIdOrderByTimeDesc(1);
        List<DataSensor> hums = dataSensorRepository.findTop15BySensorIdOrderByTimeDesc(2);
        List<DataSensor> lights = dataSensorRepository.findTop15BySensorIdOrderByTimeDesc(3);

        Map<String, ChartPointResponse> pointMap = new LinkedHashMap<>();

        // Map by formatted time
        for (DataSensor ds : temps) {
            String timeStr = ds.getTime().format(TIME_FORMATTER);
            ChartPointResponse point = pointMap.computeIfAbsent(timeStr, t -> ChartPointResponse.builder().time(t).build());
            point.setTemp(ds.getValue().doubleValue());
        }

        for (DataSensor ds : hums) {
            String timeStr = ds.getTime().format(TIME_FORMATTER);
            ChartPointResponse point = pointMap.computeIfAbsent(timeStr, t -> ChartPointResponse.builder().time(t).build());
            point.setHumidity(ds.getValue().doubleValue());
        }

        for (DataSensor ds : lights) {
            String timeStr = ds.getTime().format(TIME_FORMATTER);
            ChartPointResponse point = pointMap.computeIfAbsent(timeStr, t -> ChartPointResponse.builder().time(t).build());
            point.setLight(ds.getValue().intValue());
        }

        List<ChartPointResponse> result = new ArrayList<>(pointMap.values());
        // Reverse so chronological order (oldest to newest)
        Collections.reverse(result);

        // Fill any nulls with default/previous
        double lastTemp = 28.0;
        double lastHum = 65.0;
        int lastLight = 450;

        for (ChartPointResponse p : result) {
            if (p.getTemp() != null) lastTemp = p.getTemp(); else p.setTemp(lastTemp);
            if (p.getHumidity() != null) lastHum = p.getHumidity(); else p.setHumidity(lastHum);
            if (p.getLight() != null) lastLight = p.getLight(); else p.setLight(lastLight);
        }

        return result;
    }
}
