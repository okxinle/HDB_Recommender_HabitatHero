package habitathero.entity;
import java.util.List;

public class StructuralConstraints {

    private double       minBudget;
    private double       maxBudget;
    private List<String> preferredTowns;
    private String       preferredFlatType;
    private int          minLeaseYears;

    // ── Constructors ─────────────────────────────────────────────────────────

    public StructuralConstraints() {}

    public StructuralConstraints(double minBudget,
                                 double maxBudget,
                                 List<String> preferredTowns,
                                 String preferredFlatType,
                                 int minLeaseYears) {
        this.minBudget        = Math.max(0.0, minBudget);
        this.maxBudget        = maxBudget;
        this.preferredTowns   = preferredTowns;
        this.preferredFlatType= preferredFlatType;
        this.minLeaseYears    = Math.max(0, minLeaseYears);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public double       getMinBudget()         { return minBudget; }
    public double       getMaxBudget()         { return maxBudget; }
    public List<String> getPreferredTowns()    { return preferredTowns; }
    public String       getPreferredFlatType() { return preferredFlatType; }
    public int          getMinLeaseYears()     { return minLeaseYears; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setMinBudget(double minBudget)                   { this.minBudget         = Math.max(0.0, minBudget); }
    public void setMaxBudget(double maxBudget)                   { this.maxBudget         = maxBudget; }
    public void setPreferredTowns(List<String> preferredTowns)   { this.preferredTowns    = preferredTowns; }
    public void setPreferredFlatType(String preferredFlatType)   { this.preferredFlatType = preferredFlatType; }
    public void setMinLeaseYears(int minLeaseYears)               { this.minLeaseYears     = Math.max(0, minLeaseYears); }

    @Override
    public String toString() {
        return "StructuralConstraints{minBudget=" + minBudget
            + ", maxBudget=" + maxBudget
                + ", preferredTowns=" + preferredTowns
                + ", preferredFlatType='" + preferredFlatType + "'"
                + ", minLeaseYears=" + minLeaseYears + "}";
    }
}