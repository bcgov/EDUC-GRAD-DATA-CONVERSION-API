package ca.bc.gov.educ.api.dataconversion.model.tsw;

public enum StudentGenderEnum {

    M("Male"),
    F("Female");

    public final String label;


    StudentGenderEnum(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
