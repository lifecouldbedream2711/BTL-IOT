package BTL.IOT.Exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum ErrorCode {
    DEVICE_NOT_FOUND(1001, "Thiết bị không tồn tại", HttpStatus.NOT_FOUND),
    SENSOR_NOT_FOUND(1002, "Cảm biến không tồn tại", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(1003, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    MQTT_ERROR(1004, "Lỗi kết nối MQTT Broker", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT(1005, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Tài khoản hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR);

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
