package habitathero.boundary;

import habitathero.entity.UserProfile;

public class RecommendationRequest extends UserProfile {

    private String postalCodeA;
    private String postalCodeB;

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
}
