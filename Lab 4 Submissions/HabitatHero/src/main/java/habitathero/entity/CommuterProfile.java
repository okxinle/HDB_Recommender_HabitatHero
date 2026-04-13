package habitathero.entity;

import com.fasterxml.jackson.annotation.JsonAlias;

public class CommuterProfile {

    @JsonAlias({"enabled", "isEnabled"})
    private boolean isEnabled;
    private Coordinates destinationA;
    private Coordinates destinationB;

    // ── Constructors ─────────────────────────────────────────────────────────

    public CommuterProfile() {}

    public CommuterProfile(boolean isEnabled,
                           Coordinates destinationA,
                           Coordinates destinationB) {
        this.isEnabled    = isEnabled;
        this.destinationA = destinationA;
        this.destinationB = destinationB;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean      isEnabled()       { return isEnabled; }
    public Coordinates  getDestinationA() { return destinationA; }
    public Coordinates  getDestinationB() { return destinationB; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setEnabled(boolean isEnabled)             { this.isEnabled    = isEnabled; }
    public void setDestinationA(Coordinates destinationA) { this.destinationA = destinationA; }
    public void setDestinationB(Coordinates destinationB) { this.destinationB = destinationB; }

    @Override
    public String toString() {
        return "CommuterProfile{isEnabled=" + isEnabled
                + ", destinationA=" + destinationA
                + ", destinationB=" + destinationB + "}";
    }
}