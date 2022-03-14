package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.entity.trax.TraxStudentEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class PenUpdatesPartitionHandlerCreator implements Tasklet {

    private static final Logger LOGGER = LoggerFactory.getLogger(PenUpdatesPartitionHandlerCreator.class);

    @Autowired
    DataConversionService dataConversionService;

    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['data']}")
    List<String> partitionData;

    @Value("#{stepExecutionContext['summary']}")
    ConversionStudentSummaryDTO summaryDTO;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Process partitioned data in parallel asynchronously
        partitionData.stream().forEach(d -> {
            if (summaryDTO.getProcessedCount() % 50 == 0) {
                summaryDTO.setAccessToken(fetchAccessToken());
            }
            System.out.println(Thread.currentThread().getName() + " processing partitionData = " + d);
            TraxStudentEntity st = new TraxStudentEntity();
            st.setStudNo(d);
            dataConversionService.readTraxStudentAndAddNewPen(st, summaryDTO);
        });
        System.out.println(Thread.currentThread().getName() + " summary processed count = " + summaryDTO.getProcessedCount());

        // Aggregate summary
        aggregate(contribution);
        return RepeatStatus.FINISHED;
    }

    private void aggregate(StepContribution contribution) {
        ConversionStudentSummaryDTO totalSummaryDTO = (ConversionStudentSummaryDTO)contribution.getStepExecution().getJobExecution().getExecutionContext().get("penUpdatesSummaryDTO");
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new ConversionStudentSummaryDTO();
            totalSummaryDTO.setReadCount(summaryDTO.getReadCount());
            totalSummaryDTO.setProcessedCount(summaryDTO.getProcessedCount());
            contribution.getStepExecution().getJobExecution().getExecutionContext().put("penUpdatesSummaryDTO", totalSummaryDTO);
        } else {
            totalSummaryDTO.setReadCount(totalSummaryDTO.getReadCount() + summaryDTO.getReadCount());
            totalSummaryDTO.setProcessedCount(totalSummaryDTO.getProcessedCount() + summaryDTO.getProcessedCount());
        }
    }

    private String fetchAccessToken() {
        LOGGER.info("Fetching the access token from KeyCloak API");
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            return res.getAccess_token();
        }
        return null;
    }
}