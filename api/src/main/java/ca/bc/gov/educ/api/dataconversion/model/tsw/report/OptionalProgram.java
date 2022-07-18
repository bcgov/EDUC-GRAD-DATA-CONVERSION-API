package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import java.io.Serializable;
import java.util.List;

public class OptionalProgram implements Serializable {

    private static final long serialVersionUID = 2L;

    private String optionalProgramCode;
    private String optionalProgramName;
    private String programCompletionDate;
    private String hasRequirementMet;
    private List<GradRequirement> requirementMet;
    private List<NonGradReason> nonGradReasons;

    public String getOptionalProgramCode() {
        return optionalProgramCode;
    }

    public void setOptionalProgramCode(String optionalProgramCode) {
        this.optionalProgramCode = optionalProgramCode;
    }

    public String getOptionalProgramName() {
        return optionalProgramName;
    }

    public void setOptionalProgramName(String optionalProgramName) {
        this.optionalProgramName = optionalProgramName;
    }

    public String getProgramCompletionDate() {
        return programCompletionDate;
    }

    public void setProgramCompletionDate(String programCompletionDate) {
        this.programCompletionDate = programCompletionDate;
    }

    public String getHasRequirementMet() {
        return hasRequirementMet;
    }

    public void setHasRequirementMet(String hasRequirementMet) {
        this.hasRequirementMet = hasRequirementMet;
    }

    public List<GradRequirement> getRequirementMet() {
        return requirementMet;
    }

    public void setRequirementMet(List<GradRequirement> requirementMet) {
        this.requirementMet = requirementMet;
    }

    public List<NonGradReason> getNonGradReasons() {
        return nonGradReasons;
    }

    public void setNonGradReasons(List<NonGradReason> nonGradReasons) {
        this.nonGradReasons = nonGradReasons;
    }
}
