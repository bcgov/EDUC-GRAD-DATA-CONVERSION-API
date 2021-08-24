package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;

import java.util.List;

public class DataConversionCourseRestrictionReader implements ItemReader<GradCourseRestriction> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionCourseRestrictionReader.class);

    private final DataConversionService dataConversionService;
    private final RestUtils restUtils;

    private int indexForCourseRestriction;
    private List<GradCourseRestriction> courseRestrictionList;
    private ConversionSummaryDTO summaryDTO;

    public DataConversionCourseRestrictionReader(DataConversionService dataConversionService, RestUtils restUtils) {
        this.dataConversionService = dataConversionService;
        this.restUtils = restUtils;

        indexForCourseRestriction = 0;
    }

    @BeforeStep
    public void initializeSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = new ConversionSummaryDTO();
        summaryDTO.setTableName("COURSE_RESTRICTION");
        jobContext.put("courseRestrictionSummaryDTO", summaryDTO);
    }

    @Override
    public GradCourseRestriction read() {
        LOGGER.info("Reading the information of the next course restriction");

        if (indexForCourseRestriction % 300 == 0) {
            fetchAccessToken();
        }

        if (courseRestrictionDataIsNotInitialized()) {
            courseRestrictionList = loadRawCourseRestrictionData();
        	summaryDTO.setReadCount(courseRestrictionList.size());
        }

        GradCourseRestriction nextCourseRestriction = null;
        
        if (indexForCourseRestriction < courseRestrictionList.size()) {
            nextCourseRestriction = courseRestrictionList.get(indexForCourseRestriction);
            indexForCourseRestriction++;
            LOGGER.info("Found course restriction: {} in total {}", indexForCourseRestriction, summaryDTO.getReadCount());
        }
        else {
        	indexForCourseRestriction = 0;
            courseRestrictionList = null;
        }
        return nextCourseRestriction;
    }

    private boolean courseRestrictionDataIsNotInitialized() {
        return this.courseRestrictionList == null;
    }

    private List<GradCourseRestriction> loadRawCourseRestrictionData() {
        LOGGER.info("Fetching Student List that need Data Conversion Processing");
        return dataConversionService.loadInitialRawGradCourseRestrictionsData(false);
    }

    private void fetchAccessToken() {
        LOGGER.info("Fetching the access token from KeyCloak API");
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            summaryDTO.setAccessToken(res.getAccess_token());
            LOGGER.info("Setting the new access token in summaryDTO.");
        }
    }
}
