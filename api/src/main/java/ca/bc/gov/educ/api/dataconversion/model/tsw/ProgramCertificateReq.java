package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.Data;

@Data
public class ProgramCertificateReq {

	private String optionalProgram;	
	private String programCode;	
	private String schoolCategoryCode;
}
