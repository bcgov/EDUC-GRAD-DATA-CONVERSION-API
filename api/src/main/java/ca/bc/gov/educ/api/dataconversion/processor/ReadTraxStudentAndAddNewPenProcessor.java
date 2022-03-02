package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.entity.trax.TraxStudentEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

public class ReadTraxStudentAndAddNewPenProcessor implements ItemProcessor<TraxStudentEntity, TraxStudentEntity> {

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
	public TraxStudentEntity process(TraxStudentEntity traxStudentEntity) throws Exception {
		return dataConversionService.readTraxStudentAndAddNewPen(traxStudentEntity, summaryDTO);
	}
}
