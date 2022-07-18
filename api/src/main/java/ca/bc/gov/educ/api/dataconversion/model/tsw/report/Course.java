package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import java.io.Serializable;

public class Course implements Serializable {
    private static final long serialVersionUID = 2L;

    private String name;
    private String code;
    private String level;
    private String credits;
    private String sessionDate;
    private String type;
    private String relatedCourse;
    private String relatedLevel;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String value) {
        this.level = value;
    }

    public String getCredits() {
        return credits;
    }

    public void setCredits(String value) {
        this.credits = value;
    }

    public String getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(String value) {
        this.sessionDate = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public String getRelatedCourse() {
        return relatedCourse;
    }

    public void setRelatedCourse(String value) {
        this.relatedCourse = value;
    }

    public String getRelatedLevel() {
        return relatedLevel;
    }

    public void setRelatedLevel(String value) {
        this.relatedLevel = value;
    }
}
