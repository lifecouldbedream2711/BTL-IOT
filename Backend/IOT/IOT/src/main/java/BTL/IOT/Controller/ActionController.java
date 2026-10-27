package BTL.IOT.Controller;

import BTL.IOT.Dto.response.APIResponse;
import BTL.IOT.Dto.response.ActionRecordDto;
import BTL.IOT.Dto.response.PageResult;
import BTL.IOT.Service.ActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actions")
@RequiredArgsConstructor
@Tag(name = "Action API", description = "Các API tra cứu lịch sử điều khiển bật/tắt thiết bị")
public class ActionController {

    private final ActionService actionService;

    @GetMapping
    @Operation(summary = "Lấy danh sách lịch sử bật tắt thiết bị có phân trang và bộ lọc")
    public APIResponse<PageResult<ActionRecordDto>> getActions(
            @RequestParam(name = "device", defaultValue = "All Devices") String device,
            @RequestParam(name = "status", defaultValue = "All Status") String status,
            @RequestParam(name = "actionStatus", defaultValue = "All Action Status") String actionStatus,
            @RequestParam(name = "time", required = false) String time,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return APIResponse.<PageResult<ActionRecordDto>>builder()
                .code(200)
                .message("Lấy lịch sử hoạt động thành công")
                .result(actionService.searchActions(device, status, actionStatus, time, page, pageSize))
                .build();
    }
}
