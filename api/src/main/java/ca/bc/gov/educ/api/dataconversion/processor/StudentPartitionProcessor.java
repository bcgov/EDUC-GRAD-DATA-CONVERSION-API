package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.constant.StudentLoadType;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionAlert;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.TraxStudentNo;
import ca.bc.gov.educ.api.dataconversion.process.StudentProcess;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class StudentPartitionProcessor implements ItemProcessor<String, ConvGradStudent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentPartitionProcessor.class);

    @Autowired
	private StudentProcess studentProcess;

	@Autowired
	private RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	private ConversionStudentSummaryDTO summaryDTO;

	@Value("#{stepExecutionContext['reload']}")
	private String reload;

	@Override
	public ConvGradStudent process(String pen) throws Exception {
		ConvGradStudent responseStudent = null;
		try {
			List<ConvGradStudent> students = restUtils.getTraxStudentMasterDataByPen(pen, summaryDTO.getAccessToken());
			if (students != null && !students.isEmpty()) {
				responseStudent = students.get(0);
				if (responseStudent.getResult() != null &&
					responseStudent.getResult().equals(ConversionResultType.FAILURE)) {
					String reason = "";
					if (responseStudent.getStudentLoadType() == StudentLoadType.GRAD_TWO) {
						reason = "ERROR-Skip data: [graduated - two programs] not implemented yet";
					} else if (responseStudent.getStudentLoadType() == StudentLoadType.NONE) {
						reason = "ERROR-Bad data: student is not in [ungrad, grad_one, grad_two]";
					} else {
						reason = "ERROR-Bad data: unknown error from TRAX";
					}
					summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1L);
					restUtils.saveTraxStudentNo(new TraxStudentNo(pen, "F", reason), summaryDTO.getAccessToken());
					return null;
				}
				// convert
				responseStudent = studentProcess.convertStudent(students.get(0), summaryDTO, StringUtils.equalsIgnoreCase(reload, "Y"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			ConversionAlert error = new ConversionAlert();
			error.setItem(pen);
			error.setLevel(ConversionAlert.AlertLevelEnum.ERROR);
			error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
			summaryDTO.getErrors().add(error);

			LOGGER.error("unknown exception: " + e.getLocalizedMessage());
			if (responseStudent == null) {
				responseStudent = ConvGradStudent.builder().pen(pen).result(ConversionResultType.FAILURE).build();
			}
		}
		return responseStudent;
	}
}
