package habitathero.entity;
import java.time.LocalDateTime;
public class AuditLogEntry {

	private String logID;
	private String adminID;
	private String action;
	private String details;
	private LocalDateTime timestamp;

}