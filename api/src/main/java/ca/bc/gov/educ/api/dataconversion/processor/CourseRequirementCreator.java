package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.process.CourseProcess;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

public class CourseRequirementCreator implements Tasklet {

    @Autowired
    CourseProcess courseProcess;

    private ConversionCourseSummaryDTO summaryDTO;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        summaryDTO =  (ConversionCourseSummaryDTO)stepContribution.getStepExecution().getJobExecution().getExecutionContext().get("courseRequirementSummaryDTO");
        courseProcess.createCourseRequirements(summaryDTO);
        return RepeatStatus.FINISHED;
    }
}