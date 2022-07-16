package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import java.io.Serializable;

public class Address implements Serializable {
    private static final long serialVersionUID = 2L;

    private String streetLine1;
    private String streetLine2;
    private String city;
    private String region;
    private String country;
    private String code;

    public String getStreetLine1() {
        return streetLine1;
    }

    public void setStreetLine1(String value) {
        this.streetLine1 = value;
    }

    public String getStreetLine2() {
        return streetLine2;
    }

    public void setStreetLine2(String value) {
        this.streetLine2 = value;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String value) {
        this.city = value;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String value) {
        this.region = value;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String value) {
        this.country = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }
}
