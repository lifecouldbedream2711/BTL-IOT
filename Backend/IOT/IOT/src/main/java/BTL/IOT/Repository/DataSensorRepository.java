package BTL.IOT.Repository;

import BTL.IOT.Entity.DataSensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSensorRepository extends JpaRepository<DataSensor, Long> {

    Optional<DataSensor> findTopBySensorIdOrderByTimeDesc(Integer sensorId);

    List<DataSensor> findTop15BySensorIdOrderByTimeDesc(Integer sensorId);

    @Query("SELECT d FROM DataSensor d WHERE (:sensorId IS NULL OR d.sensorId = :sensorId) ORDER BY d.time DESC, d.id DESC")
    Page<DataSensor> findBySensorIdFiltered(@Param("sensorId") Integer sensorId, Pageable pageable);

    @Query("SELECT d FROM DataSensor d " +
           "WHERE (:sensorId IS NULL OR d.sensorId = :sensorId) " +
           "AND (:keyword IS NULL OR :keyword = '' OR CAST(d.value AS string) LIKE %:keyword% OR CAST(d.time AS string) LIKE %:keyword%) " +
           "ORDER BY d.time DESC, d.id DESC")
    Page<DataSensor> searchDataSensors(@Param("sensorId") Integer sensorId, @Param("keyword") String keyword, Pageable pageable);
}
