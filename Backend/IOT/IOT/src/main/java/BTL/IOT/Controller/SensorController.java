package BTL.IOT.Controller;

import BTL.IOT.Dto.response.APIResponse;
import BTL.IOT.Dto.response.ChartPointResponse;
import BTL.IOT.Dto.response.SensorStatusResponse;
import BTL.IOT.Service.MqttService;
import BTL.IOT.Service.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
@Tag(name = "Sensor API", description = "Các API giám sát dữ liệu cảm biến thời gian thực")
public class SensorController {

    private final SensorService sensorService;
    private final MqttService mqttService;

    @GetMapping("/latest")
    @Operation(summary = "Lấy giá trị cảm biến mới nhất (Nhiệt độ, Độ ẩm, Ánh sáng)")
    public APIResponse<SensorStatusResponse> getLatestSensor() {
        return APIResponse.<SensorStatusResponse>builder()
                .code(200)
                .message("Lấy dữ liệu cảm biến mới nhất thành công")
                .result(sensorService.getLatestSensorStatus())
                .build();
    }

    @GetMapping("/chart")
    @Operation(summary = "Lấy dữ liệu 15 điểm gần nhất để vẽ biểu đồ Sensor Trends")
    public APIResponse<List<ChartPointResponse>> getChartData(
            @RequestParam(name = "limit", defaultValue = "15") int limit
    ) {
        return APIResponse.<List<ChartPointResponse>>builder()
                .code(200)
                .message("Lấy dữ liệu biểu đồ thành công")
                .result(sensorService.getChartPoints(limit))
                .build();
    }

    @GetMapping("/mqtt-status")
    @Operation(summary = "Kiểm tra trạng thái kết nối MQTT Broker")
    public APIResponse<Map<String, Object>> getMqttStatus() {
        boolean isConnected = mqttService.isConnected();
        return APIResponse.<Map<String, Object>>builder()
                .code(200)
                .message("Lấy trạng thái kết nối MQTT thành công")
                .result(Map.of("connected", isConnected, "status", isConnected ? "MQTT Connected" : "Disconnected"))
                .build();
    }
}
