package BTL.IOT.Dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActionRecordDto {
    Long id;
    String device;
    Integer deviceId;
    String status;
    String action;
    Long ts;
    String time;
    LocalDateTime rawTime;
}
