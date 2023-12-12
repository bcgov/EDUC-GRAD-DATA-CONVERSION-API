package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class GradStudentTranscriptValidation extends BaseModel {

	private GradStudentTranscriptValidationKey studentTranscriptValidationKey;
	private Long batchId;
    private String transcriptTypeCode;
	private String documentStatusCode;
	private String validationResult;

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("GradStudentTranscriptValidation{");
		sb.append("studentTranscriptValidationKey=").append(studentTranscriptValidationKey);
		sb.append(", batchId=").append(batchId);
		sb.append(", transcriptTypeCode='").append(transcriptTypeCode).append('\'');
		sb.append(", documentStatusCode='").append(documentStatusCode).append('\'');
		sb.append(", validationResult='").append(validationResult).append('\'');
		sb.append('}');
		return sb.toString();
	}
}