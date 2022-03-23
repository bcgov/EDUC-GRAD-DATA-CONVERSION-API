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
    public static final String READ_TRAX_AND_ADD_NEW_PEN_BATCH_JOB = "/addNewPen";
    public static final String PEN_UPDATES_PARALLEL_BATCH_JOB = "/penUpdates/parallel";

    //Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String TRAX_DATE_FORMAT = "yyyyMM";
    public static final String TRAX_SLP_DATE_FORMAT = "yyyyMMdd";

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

    @Value("${endpoint.grad-program-api.special-program.url}")
    private String gradProgramManagementUrl;

    @Value("${endpoint.grad-student-api.add-new-pen.url}")
    private String addNewPenFromGradStudentApiUrl;

    @Value("${data-conversion.student-guid-pen-xref.enabled}")
    private boolean studentGuidPenXrefEnabled;

    // Incremental Grad Update
    @Value("${grad.update.enabled}")
    private boolean gradUpdateEnabled;

    // Page Size
    @Value("${batch.page.size}")
    private int pageSize;
}
