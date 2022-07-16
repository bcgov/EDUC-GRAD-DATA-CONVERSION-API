package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class StudentAssessment {

    private String pen;
    private String assessmentCode;
    private String assessmentName;
    private String sessionDate;
    private String gradReqMet;
    private String gradReqMetDetail;
    private String specialCase;
    private String exceededWriteFlag;
    private Double proficiencyScore;
    private String wroteFlag;
    private Double rawScore;
    private Double percentComplete;
    private Double irtScore;
    private boolean isFailed;
    private boolean isDuplicate;
    private boolean isUsed;
    private boolean isProjected;
    private boolean isNotCompleted;
    
	@Override
	public String toString() {
		return "StudentAssessment [pen=" + pen + ", assessmentCode=" + assessmentCode + ", assessmentName="
				+ assessmentName + ", sessionDate=" + sessionDate + ", gradReqMet=" + gradReqMet + ", gradReqMetDetail="
				+ gradReqMetDetail + ", specialCase=" + specialCase + ", exceededWriteFlag=" + exceededWriteFlag
				+ ", proficiencyScore=" + proficiencyScore + ", wroteFlag=" + wroteFlag + ", rawScore=" + rawScore
				+ ", percentComplete=" + percentComplete + ", irtScore=" + irtScore + ", isFailed=" + isFailed
				+ ", isDuplicate=" + isDuplicate + ", isUsed=" + isUsed + ", isProjected=" + isProjected
				+ ", isNotCompleted=" + isNotCompleted + "]";
	}

    
}