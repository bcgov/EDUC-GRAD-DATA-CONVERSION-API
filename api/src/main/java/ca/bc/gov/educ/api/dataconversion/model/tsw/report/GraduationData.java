package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GraduationData implements Serializable {
    private static final long serialVersionUID = 2L;

    private Date graduationDate;
    private boolean honorsFlag;
    private boolean dogwoodFlag;
    private List<String> programCodes;
    private List<String> programNames;
    private String totalCreditsUsedForGrad;

    @JsonFormat(pattern="yyyy-MM-dd")
    public Date getGraduationDate() {
        return graduationDate;
    }

    public void setGraduationDate(Date value) {
        this.graduationDate = value;
    }

    public boolean getHonorsFlag() {
        return honorsFlag;
    }

    public void setHonorsFlag(boolean value) {
        this.honorsFlag = value;
    }

    public boolean getDogwoodFlag() {
        return dogwoodFlag;
    }

    public void setDogwoodFlag(boolean value) {
        this.dogwoodFlag = value;
    }

    public List<String> getProgramCodes() {
        if(programCodes == null) {
            programCodes = new ArrayList<>();
        }
        return programCodes;
    }

    public void setProgramCodes(List<String> value) {
        this.programCodes = value;
    }

    public List<String> getProgramNames() {
        if(programNames == null) {
            programNames = new ArrayList<>();
        }
        return programNames;
    }

    public void setProgramNames(List<String> value) {
        this.programNames = value;
    }

    public String getTotalCreditsUsedForGrad() {
        return totalCreditsUsedForGrad;
    }

    public void setTotalCreditsUsedForGrad(String value) {
        this.totalCreditsUsedForGrad = value;
    }
}
