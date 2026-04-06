package habitathero.control;

import habitathero.entity.HDBBlock;

public class BlockCandidateView {

    private HDBBlock block;
    private double averageResalePrice;
    private double averageFloorAreaSqm;
    private double averageRemainingLease;
    private long transactionCount;

    private double distanceToCommuterAKm;
    private double distanceToCommuterBKm;
    private double totalDistanceKm;
    private double fairnessGapKm;

    private double commuteScore;

    public BlockCandidateView(HDBBlock block,
                              Double averageResalePrice,
                              Double averageFloorAreaSqm,
                              Double averageRemainingLease,
                              Long transactionCount) {
        this.block = block;
        this.averageResalePrice = averageResalePrice == null ? 0.0 : averageResalePrice;
        this.averageFloorAreaSqm = averageFloorAreaSqm == null ? 0.0 : averageFloorAreaSqm;
        this.averageRemainingLease = averageRemainingLease == null ? 0.0 : averageRemainingLease;
        this.transactionCount = transactionCount == null ? 0L : transactionCount;
    }

    public HDBBlock getBlock() {
        return block;
    }

    public void setBlock(HDBBlock block) {
        this.block = block;
    }

    public double getAverageResalePrice() {
        return averageResalePrice;
    }

    public void setAverageResalePrice(double averageResalePrice) {
        this.averageResalePrice = averageResalePrice;
    }

    public double getAverageFloorAreaSqm() {
        return averageFloorAreaSqm;
    }

    public void setAverageFloorAreaSqm(double averageFloorAreaSqm) {
        this.averageFloorAreaSqm = averageFloorAreaSqm;
    }

    public double getAverageRemainingLease() {
        return averageRemainingLease;
    }

    public void setAverageRemainingLease(double averageRemainingLease) {
        this.averageRemainingLease = averageRemainingLease;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public double getDistanceToCommuterAKm() {
        return distanceToCommuterAKm;
    }

    public void setDistanceToCommuterAKm(double distanceToCommuterAKm) {
        this.distanceToCommuterAKm = distanceToCommuterAKm;
    }

    public double getDistanceToCommuterBKm() {
        return distanceToCommuterBKm;
    }

    public void setDistanceToCommuterBKm(double distanceToCommuterBKm) {
        this.distanceToCommuterBKm = distanceToCommuterBKm;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public double getFairnessGapKm() {
        return fairnessGapKm;
    }

    public void setFairnessGapKm(double fairnessGapKm) {
        this.fairnessGapKm = fairnessGapKm;
    }

    public double getCommuteScore() {
        return commuteScore;
    }

    public void setCommuteScore(double commuteScore) {
        this.commuteScore = commuteScore;
    }
}
