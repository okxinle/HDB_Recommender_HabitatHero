package entity;
public class HDBBlock {

    private int         blockId;
    private String      postalCode;
    private String      town;
    private double      estimatedPrice;
    private int         remainingLeaseYears;
    private boolean     westSunStatus;
    private String      noiseRiskLevel;
    private boolean     futureRiskFlag;
    private double      globalMatchIndex;

    // ── Constructors ─────────────────────────────────────────────────────────

    public HDBBlock() {}

    public HDBBlock(int blockId,
                    String postalCode,
                    String town,
                    double estimatedPrice,
                    int remainingLeaseYears,
                    boolean westSunStatus,
                    String noiseRiskLevel,
                    boolean futureRiskFlag) {
        this.blockId             = blockId;
        this.postalCode          = postalCode;
        this.town                = town;
        this.estimatedPrice      = estimatedPrice;
        this.remainingLeaseYears = remainingLeaseYears;
        this.westSunStatus       = westSunStatus;
        this.noiseRiskLevel      = noiseRiskLevel;
        this.futureRiskFlag      = futureRiskFlag;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int     getBlockId()             { return blockId; }
    public String  getPostalCode()          { return postalCode; }
    public String  getTown()                { return town; }
    public double  getEstimatedPrice()      { return estimatedPrice; }
    public int     getRemainingLeaseYears() { return remainingLeaseYears; }
    public boolean isWestSunStatus()        { return westSunStatus; }
    public String  getNoiseRiskLevel()      { return noiseRiskLevel; }
    public boolean isFutureRiskFlag()       { return futureRiskFlag; }
    public double  getGlobalMatchIndex()    { return globalMatchIndex; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setBlockId(int blockId)                         { this.blockId             = blockId; }
    public void setPostalCode(String postalCode)                { this.postalCode          = postalCode; }
    public void setTown(String town)                            { this.town                = town; }
    public void setEstimatedPrice(double estimatedPrice)        { this.estimatedPrice      = estimatedPrice; }
    public void setRemainingLeaseYears(int remainingLeaseYears) { this.remainingLeaseYears = remainingLeaseYears; }
    public void setWestSunStatus(boolean westSunStatus)         { this.westSunStatus       = westSunStatus; }
    public void setNoiseRiskLevel(String noiseRiskLevel)        { this.noiseRiskLevel      = noiseRiskLevel; }
    public void setFutureRiskFlag(boolean futureRiskFlag)       { this.futureRiskFlag      = futureRiskFlag; }
    public void setGlobalMatchIndex(double globalMatchIndex)    { this.globalMatchIndex    = globalMatchIndex; }

    @Override
    public String toString() {
        return "HDBBlock{blockId=" + blockId
                + ", postalCode='" + postalCode + "'"
                + ", town='" + town + "'"
                + ", estimatedPrice=" + estimatedPrice
                + ", remainingLeaseYears=" + remainingLeaseYears
                + ", westSunStatus=" + westSunStatus
                + ", noiseRiskLevel='" + noiseRiskLevel + "'"
                + ", futureRiskFlag=" + futureRiskFlag
                + ", globalMatchIndex=" + globalMatchIndex + "}";
    }
}