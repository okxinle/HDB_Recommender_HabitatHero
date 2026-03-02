package habitathero.entity;
public class WeightedPreference {

    private String  factorName;
    private double  priorityWeight;
    private boolean isStrict;

    // ── Constructors ───────────────────────────────────────────────────────

    public WeightedPreference() {}

    public WeightedPreference(String factorName, double priorityWeight, boolean isStrict) {
        this.factorName     = factorName;
        this.priorityWeight = priorityWeight;
        this.isStrict       = isStrict;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public String  getFactorName()     { return factorName; }
    public double  getPriorityWeight() { return priorityWeight; }
    public boolean isStrict()          { return isStrict; }

    // ── Setters ────────────────────────────────────────────────────────────

    public void setFactorName(String factorName)          { this.factorName     = factorName; }
    public void setPriorityWeight(double priorityWeight)  { this.priorityWeight = priorityWeight; }
    public void setStrict(boolean isStrict)               { this.isStrict       = isStrict; }

    @Override
    public String toString() {
        return "WeightedPreference{factorName='" + factorName + "'"
                + ", priorityWeight=" + priorityWeight
                + ", isStrict=" + isStrict + "}";
    }
}
