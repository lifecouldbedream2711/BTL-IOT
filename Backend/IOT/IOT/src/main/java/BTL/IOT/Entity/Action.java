package BTL.IOT.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "Actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    Long id;

    @Column(name = "DeviceID", nullable = false)
    Integer deviceId;

    @Column(name = "Action", nullable = false, length = 50)
    String action;

    @Column(name = "Status", nullable = false, length = 50)
    String status;

    @Column(name = "Time", nullable = false)
    LocalDateTime time;
}
