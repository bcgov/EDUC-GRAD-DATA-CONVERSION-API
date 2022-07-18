package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;
import java.util.List;

public class School implements Serializable {

    private static final long serialVersionUID = 2L;

    private String mincode;
    private String name;
    private String typeIndicator;
    private String typeBanner;
    private String signatureCode;
    private String distno;
    private String schlno;
    private String schoolCategoryCode;
    private Address address;
    private String phoneNumber = "";
    private String dogwoodElig = "";

    private List<Student> students;

    public String getMincode() {
        return mincode;
    }

    public void setMincode(String value) {
        this.mincode = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getTypeIndicator() {
        return typeIndicator;
    }

    public void setTypeIndicator(String value) {
        this.typeIndicator = value;
    }

    public String getTypeBanner() {
        return typeBanner;
    }

    public void setTypeBanner(String value) {
        this.typeBanner = value;
    }

    public String getSignatureCode() {
        return signatureCode;
    }

    public void setSignatureCode(String value) {
        this.signatureCode = value;
    }

    public String getDistno() {
        return distno;
    }

    public void setDistno(String value) {
        this.distno = value;
    }

    public String getSchlno() {
        return schlno;
    }

    public void setSchlno(String value) {
        this.schlno = value;
    }

    public String getSchoolCategoryCode() {
        return schoolCategoryCode;
    }

    public void setSchoolCategoryCode(String value) {
        this.schoolCategoryCode = value;
    }

    @JsonDeserialize(as = Address.class)
    public Address getAddress() {
        return address;
    }

    public void setAddress(Address value) {
        this.address = value;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDogwoodElig() {
        return dogwoodElig;
    }

    public void setDogwoodElig(String dogwoodElig) {
        this.dogwoodElig = dogwoodElig;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }
}
