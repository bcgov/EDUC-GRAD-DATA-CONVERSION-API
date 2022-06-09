package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionAlert;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.TraxStudentNo;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class StudentPartitionProcessor implements ItemProcessor<String, ConvGradStudent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentPartitionProcessor.class);

    @Autowired
	private StudentService studentService;

	@Autowired
	private RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	private ConversionStudentSummaryDTO summaryDTO;

	@Override
	public ConvGradStudent process(String pen) throws Exception {
		ConvGradStudent responseStudent = null;
		try {
			List<ConvGradStudent> students = restUtils.getTraxStudentMasterDataByPen(pen, summaryDTO.getAccessToken());
			if (students != null && !students.isEmpty()) {
				responseStudent = students.get(0);

				// convert
				responseStudent = studentService.convertStudent(students.get(0), summaryDTO);

				// update status
				TraxStudentNo traxStudentNo = new TraxStudentNo();
				traxStudentNo.setStudNo(pen);
				traxStudentNo.setStatus(ConversionResultType.SUCCESS.toString());
				restUtils.saveTraxStudentNo(traxStudentNo, summaryDTO.getAccessToken());
			}
		} catch (Exception e) {
			ConversionAlert error = new ConversionAlert();
			error.setItem(pen);
			error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
			summaryDTO.getErrors().add(error);
			LOGGER.error("unknown exception: " + e.getLocalizedMessage());
		}
		return responseStudent;
	}
}
