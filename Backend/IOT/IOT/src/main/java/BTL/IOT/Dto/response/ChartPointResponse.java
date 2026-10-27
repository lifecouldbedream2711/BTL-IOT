package BTL.IOT.Dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChartPointResponse {
    String time;
    Integer light;
    Double humidity;
    Double temp;
}
