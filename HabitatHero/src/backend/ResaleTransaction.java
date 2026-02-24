public class ResaleTransaction {

	private String month;
	private String town;
	private String block;
	private String flatType;
	private double floorAreaSqm;
	private int remainingLease;
	private double resalePrice;

	public double getResalePrice() {
		return this.resalePrice;
	}

	public String getTown() {
		return this.town;
	}

	public String getFlatType() {
		return this.flatType;
	}

	public double getPricePerSqm() {
		// TODO - implement ResaleTransaction.getPricePerSqm
		throw new UnsupportedOperationException();
	}

}