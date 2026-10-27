package BTL.IOT.Dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorDataRecordDto {
    Long id;
    String type;
    BigDecimal value;
    String time;
    LocalDateTime rawTime;
}
