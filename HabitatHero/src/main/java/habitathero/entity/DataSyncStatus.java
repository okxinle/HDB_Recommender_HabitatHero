package habitathero.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "data_sync_status")
public class DataSyncStatus {

    /**
     * A stable identifier for the data source, e.g.
     * "HDB_DATA_API", "ONEMAP_ROUTING", "URA_MASTERPLAN"
     */
    @Id
    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    /**
     * Suggested values: "RUNNING", "SUCCESS", "FAILED"
     */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "SUCCESS";

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    public DataSyncStatus() {}

    public DataSyncStatus(String source, String status) {
        this.source = source;
        this.status = status;
    }

    public DataSyncStatus(String source, LocalDateTime lastRunAt, LocalDateTime lastSuccessAt, String status, String message) {
        this.source = source;
        this.lastRunAt = lastRunAt;
        this.lastSuccessAt = lastSuccessAt;
        this.status = status;
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(LocalDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public LocalDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(LocalDateTime lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "DataSyncStatus{" +
                "source='" + source + '\'' +
                ", lastRunAt=" + lastRunAt +
                ", lastSuccessAt=" + lastSuccessAt +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}