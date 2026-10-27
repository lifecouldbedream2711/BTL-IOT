package BTL.IOT.Repository;

import BTL.IOT.Entity.Action;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {

    Optional<Action> findTopByDeviceIdOrderByTimeDesc(Integer deviceId);

    @Query("SELECT a FROM Action a " +
           "WHERE (:deviceId IS NULL OR a.deviceId = :deviceId) " +
           "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
           "AND (:action IS NULL OR :action = '' OR a.action = :action) " +
           "AND (:timeStr IS NULL OR :timeStr = '' OR CAST(a.time AS string) LIKE %:timeStr%) " +
           "ORDER BY a.time DESC, a.id DESC")
    Page<Action> searchActions(
            @Param("deviceId") Integer deviceId,
            @Param("status") String status,
            @Param("action") String action,
            @Param("timeStr") String timeStr,
            Pageable pageable
    );
}
