package BTL.IOT.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "DataSensors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    Long id;

    @Column(name = "SensorID", nullable = false)
    Integer sensorId;

    @Column(name = "Value", nullable = false, precision = 10, scale = 2)
    BigDecimal value;

    @Column(name = "Time", nullable = false)
    LocalDateTime time;
}
