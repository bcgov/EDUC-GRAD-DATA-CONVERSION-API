package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GradCourse;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;

import java.util.List;

public class DataConversionCourseRequirementReader implements ItemReader<GradCourse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionCourseRequirementReader.class);

    private final RestUtils restUtils;

    private int indexForCourseRequirement;
    private List<GradCourse> courseRequirementList;
    private ConversionCourseSummaryDTO summaryDTO;

    public DataConversionCourseRequirementReader(RestUtils restUtils) {
        this.restUtils = restUtils;

        indexForCourseRequirement = 0;
    }

    @BeforeStep
    public void initializeSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = new ConversionCourseSummaryDTO();
        summaryDTO.setTableName("COURSE_REQUIREMENT");
        jobContext.put("courseRequirementSummaryDTO", summaryDTO);

        // initialize
        courseRequirementList = null;
        indexForCourseRequirement = 0;
    }

    @Override
    public GradCourse read() {
        LOGGER.info("Reading the information of the next course requirement");

        if (indexForCourseRequirement % 50 == 0) {
            fetchAccessToken();
        }

        if (courseRequirementDataIsNotInitialized()) {
            courseRequirementList = loadCourseRequirementData(summaryDTO.getAccessToken());
        	summaryDTO.setReadCount(courseRequirementList.size());
        }

        GradCourse nextCourseRequirement = null;
        
        if (indexForCourseRequirement < courseRequirementList.size()) {
            nextCourseRequirement = courseRequirementList.get(indexForCourseRequirement);
            indexForCourseRequirement++;
            LOGGER.info("Found course requirement: {} in total {}", indexForCourseRequirement, summaryDTO.getReadCount());
        }
        else {
            indexForCourseRequirement = 0;
            courseRequirementList = null;
        }
        return nextCourseRequirement;
    }

    private boolean courseRequirementDataIsNotInitialized() {
        return this.courseRequirementList == null;
    }

    private List<GradCourse> loadCourseRequirementData(String accessToken) {
        LOGGER.info("Fetching Course Requirement List that need Data Conversion Processing");
        return restUtils.getTraxCourseRequirements(accessToken);
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
