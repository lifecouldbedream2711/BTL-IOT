package BTL.IOT.Repository;

import BTL.IOT.Entity.Action;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {

    Optional<Action> findTopByDeviceIdOrderByTimeDesc(Integer deviceId);

    Optional<Action> findTopByDeviceIdAndStatusNotOrderByTimeDesc(Integer deviceId, String status);

    List<Action> findByStatusOrderByTimeAsc(String status);

    @Query("SELECT a FROM Action a " +
           "WHERE (:deviceId IS NULL OR a.deviceId = :deviceId) " +
           "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
           "AND (:actionStatus IS NULL OR :actionStatus = '' OR " +
           "     (:actionStatus = 'Pending' AND UPPER(a.status) = 'PENDING') OR " +
           "     (:actionStatus = 'Failed' AND UPPER(a.status) = 'FAILED') OR " +
           "     (:actionStatus = 'Success' AND UPPER(a.status) NOT IN ('PENDING', 'FAILED'))) " +
           "AND (:timeStr IS NULL OR :timeStr = '' OR CAST(a.time AS string) LIKE %:timeStr%) " +
           "ORDER BY a.time DESC, a.id DESC")
    Page<Action> searchActions(
            @Param("deviceId") Integer deviceId,
            @Param("status") String status,
            @Param("actionStatus") String actionStatus,
            @Param("timeStr") String timeStr,
            Pageable pageable
    );
}
