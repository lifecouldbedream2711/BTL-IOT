package BTL.IOT.Controller;

import BTL.IOT.Dto.request.AuthenticationRequest;
import BTL.IOT.Dto.request.IntrospectRequest;
import BTL.IOT.Dto.response.APIResponse;
import BTL.IOT.Dto.response.AuthenticationResponse;
import BTL.IOT.Dto.response.IntrospectResponse;
import BTL.IOT.Service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Authentication Controller", description = "APIs xác thực và cấp phát JWT token")
public class AuthenticationController {

    AuthenticationService authenticationService;

    @PostMapping({"/Authenticate/token", "/api/auth/token", "/api/auth/login"})
    @Operation(summary = "Đăng nhập và lấy JWT token")
    public APIResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return APIResponse.<AuthenticationResponse>builder()
                .code(1000)
                .message("Đăng nhập thành công")
                .result(result)
                .build();
    }

    @PostMapping({"/Authenticate/introspect", "/api/auth/introspect"})
    @Operation(summary = "Kiểm tra tính hợp lệ của JWT token")
    public APIResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return APIResponse.<IntrospectResponse>builder()
                .code(1000)
                .message("Kiểm tra token thành công")
                .result(result)
                .build();
    }
}
