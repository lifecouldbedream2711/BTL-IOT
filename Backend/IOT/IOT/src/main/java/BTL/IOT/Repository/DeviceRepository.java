package BTL.IOT.Repository;

import BTL.IOT.Entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer> {
    Optional<Device> findByNameIgnoreCase(String name);
}
