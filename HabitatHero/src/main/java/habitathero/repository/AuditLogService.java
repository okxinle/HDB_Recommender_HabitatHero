package habitathero.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import habitathero.entity.AuditLog;

@Repository
public interface AuditLogService extends JpaRepository<AuditLog, Long> {
    
}
