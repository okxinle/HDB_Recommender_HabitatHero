package entity;
import java.util.List;

public class StructuralConstraints {

    private double       maxBudget;
    private List<String> preferredTowns;
    private String       preferredFlatType;
    private int          minLeaseYears;

    // ── Constructors ─────────────────────────────────────────────────────────

    public StructuralConstraints() {}

    public StructuralConstraints(double maxBudget,
                                 List<String> preferredTowns,
                                 String preferredFlatType,
                                 int minLeaseYears) {
        this.maxBudget        = maxBudget;
        this.preferredTowns   = preferredTowns;
        this.preferredFlatType= preferredFlatType;
        this.minLeaseYears    = minLeaseYears;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public double       getMaxBudget()         { return maxBudget; }
    public List<String> getPreferredTowns()    { return preferredTowns; }
    public String       getPreferredFlatType() { return preferredFlatType; }
    public int          getMinLeaseYears()     { return minLeaseYears; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setMaxBudget(double maxBudget)                   { this.maxBudget         = maxBudget; }
    public void setPreferredTowns(List<String> preferredTowns)   { this.preferredTowns    = preferredTowns; }
    public void setPreferredFlatType(String preferredFlatType)   { this.preferredFlatType = preferredFlatType; }
    public void setMinLeaseYears(int minLeaseYears)               { this.minLeaseYears     = minLeaseYears; }

    @Override
    public String toString() {
        return "StructuralConstraints{maxBudget=" + maxBudget
                + ", preferredTowns=" + preferredTowns
                + ", preferredFlatType='" + preferredFlatType + "'"
                + ", minLeaseYears=" + minLeaseYears + "}";
    }
}