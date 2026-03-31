package habitathero.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_saved_results")
public class UserSavedResults {

    @Id
    @Column(name = "user_id")
    private int userId;

    @Column(name = "results_json", nullable = false, columnDefinition = "TEXT")
    private String resultsJson;

    public UserSavedResults() {}

    public UserSavedResults(int userId, String resultsJson) {
        this.userId = userId;
        this.resultsJson = resultsJson;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getResultsJson() {
        return resultsJson;
    }

    public void setResultsJson(String resultsJson) {
        this.resultsJson = resultsJson;
    }
}
