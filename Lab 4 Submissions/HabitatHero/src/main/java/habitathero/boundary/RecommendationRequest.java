package habitathero.boundary;

import java.util.List;

import habitathero.entity.UserProfile;

public class RecommendationRequest extends UserProfile {

    private String postalCodeA;
    private String postalCodeB;
    private String convenienceMode;
    private double convenienceWeight;
    private List<String> selectedAmenities;
    private String parentsPostalCode;

    public String getPostalCodeA() {
        return postalCodeA;
    }

    public void setPostalCodeA(String postalCodeA) {
        this.postalCodeA = postalCodeA;
    }

    public String getPostalCodeB() {
        return postalCodeB;
    }

    public void setPostalCodeB(String postalCodeB) {
        this.postalCodeB = postalCodeB;
    }

    public String getConvenienceMode() {
        return convenienceMode;
    }

    public void setConvenienceMode(String convenienceMode) {
        this.convenienceMode = convenienceMode;
    }

    public double getConvenienceWeight() {
        return convenienceWeight;
    }

    public void setConvenienceWeight(double convenienceWeight) {
        this.convenienceWeight = convenienceWeight;
    }

    public List<String> getSelectedAmenities() {
        return selectedAmenities;
    }

    public void setSelectedAmenities(List<String> selectedAmenities) {
        this.selectedAmenities = selectedAmenities;
    }

    public String getParentsPostalCode() {
        return parentsPostalCode;
    }

    public void setParentsPostalCode(String parentsPostalCode) {
        this.parentsPostalCode = parentsPostalCode;
    }
}
