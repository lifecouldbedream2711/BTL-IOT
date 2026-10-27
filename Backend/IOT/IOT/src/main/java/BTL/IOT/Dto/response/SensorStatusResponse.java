package BTL.IOT.Dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorStatusResponse {
    Double temp;
    Double humidity;
    Integer light;
    LocalDateTime time;
}
