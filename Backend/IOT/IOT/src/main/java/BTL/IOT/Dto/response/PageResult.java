package BTL.IOT.Dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResult<T> {
    List<T> items;
    long total;
    int page;
    int pageSize;
    int totalPages;
}
