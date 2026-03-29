package habitathero.entity;

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

    @Id // The primary key is the userId, creating a One-to-One link with UserAccount
    private int userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private StructuralConstraints structuralConstraints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private CommuterProfile commuterProfile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<WeightedPreference> softConstraints;
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
    public StructuralConstraints    getStructuralConstraints() { return structuralConstraints; }
    public CommuterProfile          getCommuterProfile()       { return commuterProfile; }
    public List<WeightedPreference> getSoftConstraints()       { return softConstraints; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setUserId(int userId)                                          { this.userId                = userId; }
    public void setStructuralConstraints(StructuralConstraints sc)             { this.structuralConstraints = sc; }
    public void setCommuterProfile(CommuterProfile commuterProfile)            { this.commuterProfile       = commuterProfile; }
    public void setSoftConstraints(List<WeightedPreference> softConstraints)   { this.softConstraints       = softConstraints; }

    @Override
    public String toString() {
        return "UserProfile{userId=" + userId
                + ", structuralConstraints=" + structuralConstraints
                + ", commuterProfile=" + commuterProfile
                + ", softConstraints=" + softConstraints + "}";
    }
}