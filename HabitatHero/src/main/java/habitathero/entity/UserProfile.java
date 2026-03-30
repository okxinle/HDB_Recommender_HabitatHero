package habitathero.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id // The primary key is the userId, matching your ProfileController logic
    @Column(name = "user_id")
    private int userId;

    // REMOVED profileId and UserAccount object to prevent mapping conflicts 
    // and maintain the One-to-One relationship handled by the controller.

    @Column(name = "profile_name", nullable = false)
    private String profileName = "Default";

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structural_constraints", columnDefinition = "jsonb")
    private StructuralConstraints structuralConstraints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "commuter_profile", columnDefinition = "jsonb")
    private CommuterProfile commuterProfile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "soft_constraints", columnDefinition = "jsonb")
    private List<WeightedPreference> softConstraints;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ── Constructors ─────────────────────────────────────────────────────────

    public UserProfile() {}

    public UserProfile(int userId,
                       StructuralConstraints structuralConstraints,
                       CommuterProfile commuterProfile,
                       List<WeightedPreference> softConstraints) {
        this.userId                = userId;
        this.structuralConstraints = structuralConstraints;
        this.commuterProfile       = commuterProfile;
        this.softConstraints       = softConstraints;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int                      getUserId()                { return userId; }
    public String                   getProfileName()           { return profileName; }
    public boolean                  isDefault()                { return isDefault; }
    public StructuralConstraints    getStructuralConstraints() { return structuralConstraints; }
    public CommuterProfile          getCommuterProfile()       { return commuterProfile; }
    public List<WeightedPreference> getSoftConstraints()       { return softConstraints; }
    public LocalDateTime            getCreatedAt()             { return createdAt; }
    public LocalDateTime            getUpdatedAt()             { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setUserId(int userId)                                          { this.userId                = userId; }
    public void setProfileName(String profileName)                             { this.profileName           = profileName; }
    public void setDefault(boolean isDefault)                                  { this.isDefault             = isDefault; }
    public void setStructuralConstraints(StructuralConstraints sc)             { this.structuralConstraints = sc; }
    public void setCommuterProfile(CommuterProfile commuterProfile)            { this.commuterProfile       = commuterProfile; }
    public void setSoftConstraints(List<WeightedPreference> softConstraints)   { this.softConstraints       = softConstraints; }
    public void setCreatedAt(LocalDateTime createdAt)                          { this.createdAt             = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)                          { this.updatedAt             = updatedAt; }

    @Override
    public String toString() {
        return "UserProfile{userId=" + userId
                + ", profileName='" + profileName + '\''
                + ", isDefault=" + isDefault
                + ", structuralConstraints=" + structuralConstraints
                + ", commuterProfile=" + commuterProfile
                + ", softConstraints=" + softConstraints 
                + ", createdAt=" + createdAt 
                + ", updatedAt=" + updatedAt + "}";
    }
}