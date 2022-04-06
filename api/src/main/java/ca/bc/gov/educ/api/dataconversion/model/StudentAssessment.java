package ca.bc.gov.educ.api.dataconversion.model;

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
    private String specialCase;
    private String exceededWriteFlag;    
    private Double proficiencyScore;
    private Assessment assessmentDetails;
    private String mincodeAssessment;
    private String mincodeAssessmentName;
    private boolean hasMoreInfo;
    
    public String getPen() {
    	return pen != null ? pen.trim():null;
    }
    
    public String getAssessmentName() {
    	return assessmentName != null ? assessmentName.trim():null;
    }
    
    public String getAssessmentCode() {
    	return assessmentCode != null ? assessmentCode.trim():null;
    }
    
    public String getMincodeAssessment() {
    	return mincodeAssessment != null ? mincodeAssessment.trim():null;
    }

	@Override
	public String toString() {
		return "StudentAssessment [pen=" + pen + ", assessmentCode=" + assessmentCode + ", assessmentName="
				+ assessmentName + ", sessionDate=" + sessionDate + ", gradReqMet=" + gradReqMet + ", specialCase="
				+ specialCase + ", exceededWriteFlag=" + exceededWriteFlag + ", proficiencyScore=" + proficiencyScore
				+ ", assessmentDetails=" + assessmentDetails + ", mincodeAssessment=" + mincodeAssessment + "]";
	}		 
}