import java.util.List;

public class UserProfile {

    private int                       userId;
    private StructuralConstraints     structuralConstraints;
    private CommuterProfile           commuterProfile;
    private List<WeightedPreference>  softConstraints;

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