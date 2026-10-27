package BTL.IOT.Controller;

import BTL.IOT.Dto.response.APIResponse;
import BTL.IOT.Dto.response.PageResult;
import BTL.IOT.Dto.response.SensorDataRecordDto;
import BTL.IOT.Service.DataSensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensor-data")
@RequiredArgsConstructor
@Tag(name = "Data Sensor API", description = "Các API tra cứu, lọc và phân trang lịch sử dữ liệu cảm biến")
public class DataSensorController {

    private final DataSensorService dataSensorService;

    @GetMapping
    @Operation(summary = "Lấy danh sách lịch sử dữ liệu cảm biến có phân trang, lọc theo loại và tìm kiếm")
    public APIResponse<PageResult<SensorDataRecordDto>> getSensorData(
            @RequestParam(name = "sensorType", defaultValue = "All") String sensorType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return APIResponse.<PageResult<SensorDataRecordDto>>builder()
                .code(200)
                .message("Lấy danh sách dữ liệu cảm biến thành công")
                .result(dataSensorService.searchDataSensors(sensorType, keyword, page, pageSize))
                .build();
    }
}
