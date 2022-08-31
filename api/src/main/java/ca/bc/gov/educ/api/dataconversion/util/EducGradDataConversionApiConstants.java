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
    public static final String TRAX_STREAM_NAME="TRAX_STATUS_EVENTS";
    public static final String CORRELATION_ID = "correlationID";
    public static final String API_ROOT_MAPPING = "";
    public static final String API_VERSION = "v1";
    public static final String GRAD_BATCH_API_ROOT_MAPPING = "/api/" + API_VERSION + "/data-conversion";

    // Data Conversion
    public static final String GRAD_STUDENT_DATA_CONVERSION_BATCH_JOB = "/gradStudent";
    public static final String GRAD_STUDENT_PARALLEL_DATA_CONVERSION_BATCH_JOB = "/student/parallel";
    public static final String GRAD_COURSE_RESTRICTION_DATA_CONVERSION_BATCH_JOB = "/courseRestriction";
    public static final String GRAD_COURSE_REQUIREMENT_DATA_CONVERSION_BATCH_JOB = "/courseRequirement";

    // Util
    public static final String PEN_UPDATES_PARALLEL_BATCH_JOB = "/penUpdates/parallel";

    //Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String TRAX_DATE_FORMAT = "yyyyMM";
    public static final String TRAX_SLP_DATE_FORMAT = "yyyyMMdd";
    public static final String SECONDARY_DATE_FORMAT = "yyyy/MM/dd";
    public static final String DEFAULT_CREATED_BY = "DATA_CONV";
    public static final String DEFAULT_UPDATED_BY = "DATA_CONV";

    public static final String DEFAULT_END_DATE_FORMAT = "20991231"; // yyyyMMdd

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

    @Value("${endpoint.grad-program-api.programs.url}")
    private String gradProgramUrl;

    @Value("${endpoint.grad-program-api.special-program.url}")
    private String gradOptionalProgramUrl;

    @Value("${endpoint.grad-program-api.optional-program-by-id.url}")
    private String gradOptionalProgramByIDUrl;

    @Value("${endpoint.grad-program-api.career-program.url}")
    private String gradCareerProgramUrl;

    @Value("${endpoint.grad-program-api.program-rules.url}")
    private String gradProgramRulesUrl;

    @Value("${endpoint.grad-program-api.program-rules-by-trax.url}")
    private String gradProgramRulesByTraxReqNumberUrl;

    @Value("${endpoint.grad-student-api.add-new-pen.url}")
    private String addNewPenFromGradStudentApiUrl;

    @Value("${endpoint.grad-assessment-api.assessment-requirement.url}")
    private String addAssessmentRequirementApiUrl;

    @Value("${endpoint.grad-assessment-api.student-assessments-by-pen.url}")
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

    @Value("${endpoint.grad-trax-api.school.check-school-by-mincode.url}")
    private String checkSchoolByMincodeUrl;

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

    @Value("${endpoint.grad-trax-api.tsw.get-tran-demog-by-pen.url}")
    private String tswTranscriptStudentDemogByPenUrl;

    @Value("${endpoint.grad-trax-api.tsw.get-student-graduated-by-pen.url}")
    private String tswStudentIsGraduatedByPenUrl;

    @Value("${endpoint.grad-trax-api.tsw.get-tran-courses-by-pen.url}")
    private String tswTranscriptStudentCoursesByPenUrl;

    //Sree
    @Value("${endpoint.grad-graduation-report-api.get-transcript}")
    private String transcript;
    @Value("${endpoint.educ-school-api.url}")
    private String schoolCategoryCode;
    @Value("${endpoint.grad-student-graduation-api.get-special-cases.url}")
    private String specialCase;
    @Value("${endpoint.grad-program-api.program_name_by_program_code.url}")
    private String programNameEndpoint;
    @Value("${endpoint.grad-graduation-report-api.update-grad-student-transcript.url}")
    private String updateGradStudentTranscript;
    @Value("${endpoint.report-api.transcript_report}")
    private String transcriptReport;
    @Value("${endpoint.grad-graduation-report-api.get-cert-list}")
    private String certList;
    @Value("${endpoint.report-api.certificate_report}")
    private String certificateReport;
    @Value("${endpoint.grad-graduation-report-api.update-grad-student-certificate.url}")
    private String updateGradStudentCertificate;
    @Value("${endpoint.grad-graduation-report-api.update-grad-student-report.url}")
    private String updateGradStudentReport;
    @Value("${endpoint.report-api.achievement_report}")
    private String achievementReport;
    //sree
    @Value("${endpoint.grad-trax-api.school.school-by-min-code.url}")
    private String schoolByMincode;

    @Value("${data-conversion.student-guid-pen-xref.enabled}")
    private boolean studentGuidPenXrefEnabled;

    // Incremental Grad Update
    @Value("${grad.update.enabled}")
    private boolean gradUpdateEnabled;

    // Number of Partitions
    @Value("${batch.partitions.number}")
    private int numberOfPartitions;

    // Splunk LogHelper Enabled
    @Value("${splunk.log-helper.enabled}")
    private boolean splunkLogHelperEnabled;
}
