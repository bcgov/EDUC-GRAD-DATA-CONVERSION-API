package ca.bc.gov.educ.api.dataconversion.model.tsw;

import ca.bc.gov.educ.api.dataconversion.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProgramCertificateTranscript extends BaseModel {

	private UUID pcId;
	private String graduationProgramCode;
	private String schoolCategoryCode;
	private String certificateTypeCode;
	private String transcriptTypeCode;
	private String transcriptPaperType;
	private String certificatePaperType;
}
