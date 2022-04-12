package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.CourseRestriction;
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

public class DataConversionCourseRestrictionReader implements ItemReader<CourseRestriction> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionCourseRestrictionReader.class);

    private final DataConversionService dataConversionService;
    private final RestUtils restUtils;

    private int indexForCourseRestriction;
    private List<CourseRestriction> courseRestrictionList;
    private ConversionCourseSummaryDTO summaryDTO;

    public DataConversionCourseRestrictionReader(DataConversionService dataConversionService, RestUtils restUtils) {
        this.dataConversionService = dataConversionService;
        this.restUtils = restUtils;

        indexForCourseRestriction = 0;
    }

    @BeforeStep
    public void initializeSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = new ConversionCourseSummaryDTO();
        summaryDTO.setTableName("COURSE_RESTRICTION");
        jobContext.put("courseRestrictionSummaryDTO", summaryDTO);

        // initialize
        courseRestrictionList = null;
        indexForCourseRestriction = 0;
    }

    @Override
    public CourseRestriction read() {
        LOGGER.info("Reading the information of the next course restriction");

        if (courseRestrictionDataIsNotInitialized()) {
            courseRestrictionList = loadRawCourseRestrictionData();
        	summaryDTO.setReadCount(courseRestrictionList.size());
        }

        if (indexForCourseRestriction % 100 == 0) {
            fetchAccessToken();
        }

        CourseRestriction nextCourseRestriction = null;
        
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

    private List<CourseRestriction> loadRawCourseRestrictionData() {
        LOGGER.info("Fetching Course Restriction List that need Data Conversion Processing");
        return dataConversionService.loadGradCourseRestrictionsDataFromTrax();
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
