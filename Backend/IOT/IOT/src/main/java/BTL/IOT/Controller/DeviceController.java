package BTL.IOT.Controller;

import BTL.IOT.Dto.request.DeviceActionRequest;
import BTL.IOT.Dto.response.APIResponse;
import BTL.IOT.Dto.response.DeviceDto;
import BTL.IOT.Service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "Device API", description = "Các API điều khiển và lấy trạng thái thiết bị (Fan, Light, Air Conditioner)")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả thiết bị kèm trạng thái bật/tắt mới nhất")
    public APIResponse<List<DeviceDto>> getAllDevices() {
        return APIResponse.<List<DeviceDto>>builder()
                .code(200)
                .message("Lấy danh sách thiết bị thành công")
                .result(deviceService.getAllDevices())
                .build();
    }

    @PostMapping("/{id}/action")
    @Operation(summary = "Gửi lệnh điều khiển bật/tắt thiết bị (ON/OFF) qua MQTT")
    public APIResponse<DeviceDto> controlDevice(
            @PathVariable("id") String id,
            @RequestBody(required = false) DeviceActionRequest request
    ) {
        String action = request != null ? request.getAction() : "TOGGLE";
        return APIResponse.<DeviceDto>builder()
                .code(200)
                .message("Điều khiển thiết bị thành công")
                .result(deviceService.controlDevice(id, action))
                .build();
    }

    @PostMapping("/{id}/toggle")
    @Operation(summary = "Đảo trạng thái bật/tắt thiết bị")
    public APIResponse<DeviceDto> toggleDevice(@PathVariable("id") String id) {
        return APIResponse.<DeviceDto>builder()
                .code(200)
                .message("Đảo trạng thái thiết bị thành công")
                .result(deviceService.controlDevice(id, "TOGGLE"))
                .build();
    }
}
