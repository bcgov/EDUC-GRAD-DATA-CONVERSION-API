package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class ExceptionMessage {

	private String exceptionName;
	private String exceptionDetails;
}
