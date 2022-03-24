package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionAlert;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class StudentPartitionHandlerCreator extends BasePartitionHandlerCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentPartitionHandlerCreator.class);

    @Autowired
    DataConversionService dataConversionService;

    @Autowired
    StudentService studentService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LOGGER.info("=======> start partition : read count = " + partitionData.size());
        // Process partitioned data in parallel asynchronously
        partitionData.stream().forEach(pen -> {
            if (summaryDTO.getProcessedCount() % 100 == 0) {
                summaryDTO.setAccessToken(fetchAccessToken());
            }
            LOGGER.info(" ==> pen = " + pen);
            try {
                List<ConvGradStudent> students = dataConversionService.loadGradStudentDataFromTrax(pen);
                if (students != null && !students.isEmpty()) {
                    students.forEach(st -> {
                        ConvGradStudent responseStudent = studentService.convertStudent(st, summaryDTO);
                        dataConversionService.saveTraxStudent(pen, responseStudent.getResult().toString());
                    });
                }
            } catch (Exception e) {
                ConversionAlert error = new ConversionAlert();
                error.setItem(pen);
                error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
                summaryDTO.getErrors().add(error);
                LOGGER.error("unknown exception: " + e.getLocalizedMessage());
            }
        });
        LOGGER.info("=======> end partition : processed count = " + summaryDTO.getProcessedCount());

        // Aggregate summary
        aggregate(contribution, "STUDENT_LOAD", "studentSummaryDTO");
        return RepeatStatus.FINISHED;
    }
}