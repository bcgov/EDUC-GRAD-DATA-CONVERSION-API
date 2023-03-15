package ca.bc.gov.educ.api.dataconversion.model.tsw;

import ca.bc.gov.educ.api.dataconversion.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
public class GradStudentTranscripts extends BaseModel {

	private UUID id;
	private String transcript;
	private String transcriptTypeCode;
	private String transcriptTypeLabel;
	private UUID studentID;
	private Date distributionDate;
	private String documentStatusCode;
	private String documentStatusLabel;
	private boolean overwrite = false;
}
