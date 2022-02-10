package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

public class ReadTraxStudentAndAddNewPenProcessor implements ItemProcessor<ConvGradStudent, ConvGradStudent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadTraxStudentAndAddNewPenProcessor.class);

    @Autowired
	private DataConversionService dataConversionService;

	private ConversionStudentSummaryDTO summaryDTO;

	@BeforeStep
	public void retrieveSummaryDto(StepExecution stepExecution) {
		JobExecution jobExecution = stepExecution.getJobExecution();
		ExecutionContext jobContext = jobExecution.getExecutionContext();
		summaryDTO = (ConversionStudentSummaryDTO)jobContext.get("studentSummaryDTO");
	}

	@Override
	public ConvGradStudent process(ConvGradStudent convGradStudent) throws Exception {
		return dataConversionService.readTraxStudentAndAddNewPen(convGradStudent, summaryDTO);
	}
}
