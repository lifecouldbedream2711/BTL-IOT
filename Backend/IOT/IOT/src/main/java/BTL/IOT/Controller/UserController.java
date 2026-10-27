package BTL.IOT.Controller;

import BTL.IOT.Dto.response.APIResponse;
import BTL.IOT.Dto.response.UserProfileDto;
import BTL.IOT.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "Các API thông tin người dùng và sinh viên")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Lấy thông tin tài khoản người dùng / sinh viên")
    public APIResponse<UserProfileDto> getProfile() {
        return APIResponse.<UserProfileDto>builder()
                .code(200)
                .message("Lấy thông tin người dùng thành công")
                .result(userService.getUserProfile())
                .build();
    }
}
