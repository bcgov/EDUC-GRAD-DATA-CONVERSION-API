package ca.bc.gov.educ.api.dataconversion.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class EducGradDataConversionApiConstants {

    public static final String API_NAME = "GRAD-DATA-CONVERSION-API";
    public static final String TRAX_STREAM_NAME="TRAX_STATUS_EVENT_STREAM";
    public static final String CORRELATION_ID = "correlationID";
    public static final String API_ROOT_MAPPING = "";
    public static final String API_VERSION = "v1";
    public static final String GRAD_DATA_CONVERSION_API_ROOT_MAPPING = "/api/" + API_VERSION + "/data-conversion";

    // Data Conversion
    public static final String GRAD_STUDENT_PARALLEL_DATA_CONVERSION_BATCH_JOB = "/student/parallel";
    public static final String GRAD_COURSE_RESTRICTION_DATA_CONVERSION_BATCH_JOB = "/courseRestriction";
    public static final String GRAD_COURSE_REQUIREMENT_DATA_CONVERSION_BATCH_JOB = "/courseRequirement";
    public static final String GRAD_STUDENT_BY_PEN_STUDENT_API = "/student/pen/{pen}";
    public static final String GRAD_CASCADE_DELETE_STUDENT_BY_PEN = "/student/pen/{pen}";

    // Util
    public static final String PEN_UPDATES_PARALLEL_BATCH_JOB = "/penUpdates/parallel";

    //Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String TRAX_DATE_FORMAT = "yyyyMM";
    public static final String TRAX_SLP_DATE_FORMAT = "yyyyMMdd";
    public static final String SECONDARY_DATE_FORMAT = "yyyy/MM/dd";
    public static final String DEFAULT_CREATED_BY = "DATA_CONV";
    public static final String DEFAULT_UPDATED_BY = "DATA_CONV";

    @Value("${authorization.user}")
    private String userName;

    @Value("${authorization.password}")
    private String password;

    //NAT
    /**
     * The Server.
     */
    @Value("${nats.url}")
    private String natsUrl;
    /**
     * The Max reconnect.
     */
    @Value("${nats.maxReconnect}")
    private int maxReconnect;
    /**
     * The Connection name.
     */
    @Value("${nats.connectionName}")
    private String connectionName;

    @Value("${endpoint.keycloak.getToken}")
    private String tokenUrl;

    // API endpoints
    @Value("${endpoint.pen-student-api.by-pen.url}")
    private String penStudentApiByPenUrl;

    @Value("${endpoint.grad-program-api.special-program.url}")
    private String gradOptionalProgramUrl;

    @Value("${endpoint.grad-program-api.optional-program-by-id.url}")
    private String gradOptionalProgramByIDUrl;

    @Value("${endpoint.grad-student-api.add-new-pen.url}")
    private String addNewPenFromGradStudentApiUrl;

    @Value("${endpoint.grad-student-api.read-grad-student-record}")
    private String readGraduationStudentRecord;

    @Value("${endpoint.grad-student-api.save-grad-student-record}")
    private String saveGraduationStudentRecord;

    @Value("${endpoint.grad-student-api.save-grad-student-record-for-ongoing-updates}")
    private String saveGraduationStudentRecordForOngoingUpdates;

    @Value("${endpoint.grad-student-api.read-student-optional-programs}")
    private String readStudentOptionalPrograms;

    @Value("${endpoint.grad-student-api.save-student-optional-program}")
    private String saveStudentOptionalProgram;

    @Value("${endpoint.grad-student-api.remove-student-optional-program}")
    private String removeStudentOptionalProgram;

    @Value("${endpoint.grad-student-api.read-student-career-programs}")
    private String readStudentCareerPrograms;

    @Value("${endpoint.grad-student-api.save-student-career-program}")
    private String saveStudentCareerProgram;

    @Value("${endpoint.grad-student-api.remove-student-career-program}")
    private String removeStudentCareerProgram;

    @Value("${endpoint.grad-student-api.get-student-by-pen}")
    private String gradStudentByPenUrl;

    @Value("${endpoint.grad-student-api.get-student-notes-by-studentID}")
    private String gradStudentNotesByStudentID;

    @Value("${endpoint.grad-assessment-api.assessment-requirement.url}")
    private String addAssessmentRequirementApiUrl;

    @Value("${endpoint.grad-assessment-api.student-assessments.by-pen.url}")
    private String studentAssessmentsByPenApiUrl;

    @Value("${endpoint.grad-course-api.student-courses-by-pen.url}")
    private String studentCoursesByPenApiUrl;

    @Value("${endpoint.grad-course-api.course-restriction.url}")
    private String gradCourseRestrictionApiUrl;

    @Value("${endpoint.grad-course-api.save-course-restriction.url}")
    private String saveCourseRestrictionApiUrl;

    @Value("${endpoint.grad-course-api.check-course-requirement.url}")
    private String checkCourseRequirementApiUrl;

    @Value("${endpoint.grad-course-api.save-course-requirement.url}")
    private String saveCourseRequirementApiUrl;

    @Value("${endpoint.grad-course-api.check-french-immersion-course.url}")
    private String checkFrenchImmersionCourse;

    @Value("${endpoint.grad-course-api.check-french-immersion-course-for-en.url}")
    private String checkFrenchImmersionCourseForEN;

    @Value("${endpoint.grad-course-api.check-blank-language-course.url}")
    private String checkBlankLanguageCourse;

    @Value("${endpoint.grad-course-api.check-french-language-course.url}")
    private String checkFrenchLanguageCourse;

    @Value("${endpoint.grad-trax-api.student.get-student-no-list-by-page.url}")
    private String traxStudentNoListByPageUrl;

    @Value("${endpoint.grad-trax-api.student.get-total-number-of-student-no-list.url}")
    private String totalNumberOfTraxStudentNoListUrl;

    @Value("${endpoint.grad-trax-api.student.get-student-demog-data.url}")
    private String traxStudentDemogDataByPenUrl;

    @Value("${endpoint.grad-trax-api.student.get-student-master-data.url}")
    private String traxStudentMasterDataByPenUrl;

    @Value("${endpoint.grad-trax-api.course.get-course-restrictions.url}")
    private String traxCourseRestrictionsUrl;

    @Value("${endpoint.grad-trax-api.course.get-course-requirements.url}")
    private String traxCourseRequirementsUrl;

    @Value("${endpoint.grad-trax-api.student.save-trax-student-no.url}")
    private String saveTraxStudentNoUrl;

    // Incremental Grad Update
    @Value("${grad.update.enabled}")
    private boolean gradUpdateEnabled;

    // Number of Partitions
    @Value("${batch.partitions.number}")
    private int numberOfPartitions;

    // Token expiry offset (seconds)
    @Value("${batch.token-expiry.offset}")
    private int tokenExpiryOffset;

    // Splunk LogHelper Enabled
    @Value("${splunk.log-helper.enabled}")
    private boolean splunkLogHelperEnabled;

    // Scheduler: ongoing updates from TRAX to GRAD
    @Value("${cron.scheduled.process.events.stan.run}")
    private String traxToGradCronRun;

    @Value("${cron.scheduled.process.events.stan.lockAtLeastFor}")
    private String traxToGradLockAtLeastFor;

    @Value("${cron.scheduled.process.events.stan.lockAtMostFor}")
    private String traxToGradLockAtMostFor;

    @Value("${cron.scheduled.process.events.stan.threshold}")
    private int traxToGradProcessingThreshold;

    @Value("${cron.scheduled.process.purge-old-records.staleInDays}")
    private int recordsStaleInDays;
}
