package ca.bc.gov.educ.api.dataconversion.util;


import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.model.StudentAssessment;
import ca.bc.gov.educ.api.dataconversion.model.tsw.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.ReportData;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.ReportOptions;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.ReportRequest;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.Transcript;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import lombok.val;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class RestUtilsTest {
    @Autowired
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @MockBean
    EventRepository eventRepository;

    @Autowired
    private EducGradDataConversionApiConstants constants;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock
    private WebClient.RequestBodySpec requestBodyMock;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriMock;
    @Mock
    private WebClient.ResponseSpec responseMock;

    // NATS
    @MockBean
    private NatsConnection natsConnection;
    @MockBean
    private Subscriber subscriber;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetTokenResponseObject_returnsToken_with_APICallSuccess() {
        final ResponseObj tokenObject = new ResponseObj();
        String mockToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJtbUhsTG4tUFlpdTl3MlVhRnh5Yk5nekQ3d2ZIb3ZBRFhHSzNROTk0cHZrIn0.eyJleHAiOjE2NjMxODg1MzMsImlhdCI6MTY2MzE4ODIzMywianRpIjoiZjA2ZWJmZDUtMzRlMi00NjY5LTg0MDktOThkNTc3OGZiYmM3IiwiaXNzIjoiaHR0cHM6Ly9zb2FtLWRldi5hcHBzLnNpbHZlci5kZXZvcHMuZ292LmJjLmNhL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI4ZGFjNmM3Yy0xYjU5LTQ5ZDEtOTMwNC0wZGRkMTdlZGE0YWQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJncmFkLWFkbWluLWNsaWVudCIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cHM6Ly9kZXYuZ3JhZC5nb3YuYmMuY2EiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6IldSSVRFX1NUVURFTlQgR1JBRF9CVVNJTkVTU19SIENSRUFURV9TVFVERU5UX1hNTF9UUkFOU0NSSVBUX1JFUE9SVCBDUkVBVEVfR1JBRF9BU1NFU1NNRU5UX1JFUVVJUkVNRU5UX0RBVEEgUkVBRF9TVFVERU5UIFJFQURfU0NIT09MIGVtYWlsIHByb2ZpbGUiLCJjbGllbnRJZCI6ImdyYWQtYWRtaW4tY2xpZW50IiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJjbGllbnRIb3N0IjoiMTQyLjMxLjQwLjE1NiIsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC1ncmFkLWFkbWluLWNsaWVudCIsImNsaWVudEFkZHJlc3MiOiIxNDIuMzEuNDAuMTU2In0.AqSxYzfanjhxCEuxLVHcJWA528AglXezS0-6EBohLsAJ4W1prdcrcS7p6yv1mSBs9GEkCu7SZhjl97xWaNXf7Emd4O0ieawgfXhDdgCtWtpLc0X2NjRTcZmv9kCpr__LmX4Zl3temUShNLVsSI95iBD7GKQmx_qTMpf3fiXdmmBvpZIibEly9RBbrio5DirqdYKuj0CO3x7xruBdBQnutr_GK7_vkmpw-X4RAyxsCwxSDequot1cCgMcJvPb6SxOL0BHx01OjM84FPwf2DwDrLvhXXhh4KucykUJ7QfiA5unmlLQ0wfG-bBJDwpjlXazF8jOQNEcasABVTftW6s8NA";
        tokenObject.setAccess_token(mockToken);
        tokenObject.setRefresh_token("456");

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getTokenUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(ResponseObj.class)).thenReturn(Mono.just(tokenObject));

        val result = this.restUtils.getTokenResponseObject();
        assertThat(result).isNotNull();
        assertThat(result.getAccess_token()).isEqualTo(mockToken);
        assertThat(result.getRefresh_token()).isEqualTo("456");
    }

    @Test
    public void testGetStudentByPen_givenValues_returnsStudent_with_APICallSuccess() {
        final String studentID = UUID.randomUUID().toString();
        final Student student = new Student();
        final String pen = "123456789";
        student.setStudentID(studentID);
        student.setPen(pen);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getPenStudentApiByPenUrl(), pen))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(student)));

        val result = this.restUtils.getStudentsByPen(pen, "abc");
        assertThat(result).isNotNull();
        assertThat(result.size() > 0).isTrue();
        assertThat(result.get(0).getPen()).isEqualTo(pen);
    }

    @Test
    public void testGetSpecialProgram_givenValues_returnsGradSpecialProgram_with_APICallSuccess() {
        final UUID specialProgramID = UUID.randomUUID();
        final OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(specialProgramID);
        specialProgram.setGraduationProgramCode("abc");
        specialProgram.setOptProgramCode("def");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getGradOptionalProgramUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(OptionalProgram.class)).thenReturn(Mono.just(specialProgram));
        val result = this.restUtils.getOptionalProgram("abc", "def", "123");
        assertThat(result).isNotNull();
        assertThat(result.getGraduationProgramCode()).isEqualTo("abc");
        assertThat(result.getOptProgramCode()).isEqualTo("def");
    }

    @Test
    public void testGetStudentAssessments_givenValues_returnsStudentAssessments_withAPICallSuccess() {
        final String pen = "123456789";
        final String assmtCode = "assmtCode";

        final Assessment assessment = new Assessment();
        assessment.setAssessmentCode(assmtCode);
        assessment.setAssessmentName(assmtCode + " Test Name");

        final StudentAssessment studentAssessment = new StudentAssessment();
        studentAssessment.setPen(pen);
        studentAssessment.setAssessmentCode(assmtCode);
        studentAssessment.setAssessmentDetails(assessment);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getStudentAssessmentsByPenApiUrl(), pen))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<StudentAssessment>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(studentAssessment)));

        val result = this.restUtils.getStudentAssessmentsByPen(pen, "abc");
        assertThat(result).isNotNull();
        assertThat(result.size() > 0).isTrue();
        assertThat(result.get(0).getPen()).isEqualTo(pen);
    }

    @Test
    public void testAddNewPen_returnsStudent_with_APICallSuccess() {
        final String studentID = UUID.randomUUID().toString();
        final Student student = new Student();
        final String pen = "123456789";
        student.setStudentID(studentID);
        student.setPen(pen);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getAddNewPenFromGradStudentApiUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Student.class)).thenReturn(Mono.just(student));

        val result = this.restUtils.addNewPen(student, "123");
        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
    }

    @Test
    public void testAddAssessRequirement_returnsAssessmentRequirement_with_APICallSuccess() {
        final String assmtCode = "assmtCode";
        final String assmtReqCode = "ruleCode";

        final AssessmentRequirementCode ruleCode = new AssessmentRequirementCode();
        ruleCode.setAssmtRequirementCode(assmtReqCode);

        final AssessmentRequirement assessmentRequirement = new AssessmentRequirement();
        assessmentRequirement.setAssessmentCode(assmtCode);
        assessmentRequirement.setRuleCode(ruleCode);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getAddAssessmentRequirementApiUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(AssessmentRequirement.class)).thenReturn(Mono.just(assessmentRequirement));

        val result = this.restUtils.addAssessmentRequirement(assessmentRequirement, "123");
        assertThat(result).isNotNull();
        assertThat(result.getAssessmentCode()).isEqualTo(assmtCode);
        assertThat(result.getRuleCode().getAssmtRequirementCode()).isEqualTo(assmtReqCode);
    }

    @Test
    public void testSaveCourseRestriction_returnsCourseRestriction_with_APICallSuccess() {
        final String courseCode = "courseCode";
        final String courseLevel = "11";
        final String restrictedCourseCode = "rest code";
        final String restrictedCourseLevel = "11";

        final CourseRestriction courseRestriction = new CourseRestriction();
        courseRestriction.setMainCourse(courseCode);
        courseRestriction.setMainCourseLevel(courseLevel);
        courseRestriction.setRestrictedCourse(restrictedCourseCode);
        courseRestriction.setRestrictedCourseLevel(restrictedCourseLevel);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getSaveCourseRestrictionApiUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(CourseRestriction.class)).thenReturn(Mono.just(courseRestriction));

        val result = this.restUtils.saveCourseRestriction(courseRestriction, "123");
        assertThat(result).isNotNull();
        assertThat(result.getMainCourse()).isEqualTo(courseCode);
        assertThat(result.getMainCourseLevel()).isEqualTo(courseLevel);
        assertThat(result.getRestrictedCourse()).isEqualTo(restrictedCourseCode);
        assertThat(result.getRestrictedCourseLevel()).isEqualTo(restrictedCourseLevel);
    }


    @Test
    public void testSaveCourseRequirement_returnsCourseRequirement_with_APICallSuccess() {
        final String courseCode = "courseCode";
        final String courseLevel = "11";
        final String courseReqCode = "ruleCode";

        final CourseRequirementCodeDTO ruleCode = new CourseRequirementCodeDTO();
        ruleCode.setCourseRequirementCode(courseReqCode);

        final CourseRequirement courseRequirement = new CourseRequirement();
        courseRequirement.setCourseCode(courseCode);
        courseRequirement.setCourseLevel(courseLevel);
        courseRequirement.setRuleCode(ruleCode);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getSaveCourseRequirementApiUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(CourseRequirement.class)).thenReturn(Mono.just(courseRequirement));

        val result = this.restUtils.saveCourseRequirement(courseRequirement, "123");
        assertThat(result).isNotNull();
        assertThat(result.getCourseCode()).isEqualTo(courseCode);
        assertThat(result.getRuleCode().getCourseRequirementCode()).isEqualTo(courseReqCode);
    }

    @Test
    public void testCheckFrenchImmersionCourse_returnsTrue_with_APICallSuccess() {
        final String pen = "123456789";
        final String courseLevel = "11";

        Map<String, String> params = new HashMap<>();
        params.put("pen", pen);
        params.put("courseLevel", courseLevel);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getCheckFrenchImmersionCourse()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(Boolean.class)).thenReturn(Mono.just(Boolean.TRUE));

        val result = this.restUtils.checkFrenchImmersionCourse(pen, courseLevel, "abc");
        assertThat(result).isNotNull();
        assertThat(result).isTrue();
    }

    @Test
    public void testCheckFrenchImmersionCourseFor1986EN_returnsTrue_with_APICallSuccess() {
        final String pen = "123456789";

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getCheckFrenchImmersionCourseForEN()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(Boolean.class)).thenReturn(Mono.just(Boolean.TRUE));

        val result = this.restUtils.checkFrenchImmersionCourseForEN(pen, "11", "abc");
        assertThat(result).isNotNull();
        assertThat(result).isTrue();
    }

    @Test
    public void testCheckBlankLanguageCourse_returnsTrue_with_APICallSuccess() {
        final String courseCode = "main";
        final String courseLevel = "12";

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getCheckBlankLanguageCourse()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(Boolean.class)).thenReturn(Mono.just(Boolean.TRUE));

        val result = this.restUtils.checkBlankLanguageCourse(courseCode, courseLevel, "abc");
        assertThat(result).isNotNull();
        assertThat(result).isTrue();
    }

    @Test
    public void testCheckFrenchLanguageCourse_returnsTrue_with_APICallSuccess() {
        final String courseCode = "main";
        final String courseLevel = "12";

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getCheckFrenchLanguageCourse()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(Boolean.class)).thenReturn(Mono.just(Boolean.TRUE));

        val result = this.restUtils.checkFrenchLanguageCourse(courseCode, courseLevel, "abc");
        assertThat(result).isNotNull();
        assertThat(result).isTrue();
    }

    @Test
    public void testGetTranscriptStudentDemog_returnsObject_withAPICallSuccess() {
        final TranscriptStudentDemog transcriptStudentDemog = new TranscriptStudentDemog();
        transcriptStudentDemog.setStudNo("123456789");
        transcriptStudentDemog.setMincode("12345678");
        transcriptStudentDemog.setSchoolName("Test school");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getTswTranscriptStudentDemogByPenUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(TranscriptStudentDemog.class)).thenReturn(Mono.just(transcriptStudentDemog));

        val result = this.restUtils.getTranscriptStudentDemog(transcriptStudentDemog.getStudNo(), "123");
        assertThat(result).isNotNull();
        assertThat(result.getStudNo()).isEqualTo(transcriptStudentDemog.getStudNo());
    }

    @Test
    public void testGetTranscriptStudentIsGraduated_returnsObject_withAPICallSuccess() {
        final TranscriptStudentDemog transcriptStudentDemog = new TranscriptStudentDemog();
        transcriptStudentDemog.setStudNo("123456789");
        transcriptStudentDemog.setMincode("12345678");
        transcriptStudentDemog.setSchoolName("Test school");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getTraxStudentIsGraduatedByPenUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(Boolean.class)).thenReturn(Mono.just(Boolean.TRUE));

        val result = this.restUtils.getTranscriptStudentIsGraduated(transcriptStudentDemog.getStudNo(), "123");
        assertThat(result).isTrue();
    }

    @Test
    public void testGetTranscriptStudentCourses_returnsObject_withAPICallSuccess() {
        final TranscriptStudentCourse transcriptStudentCourse = new TranscriptStudentCourse();
        transcriptStudentCourse.setStudNo("123456789");
        transcriptStudentCourse.setCourseCode("Test");
        transcriptStudentCourse.setCourseLevel("11");
        transcriptStudentCourse.setCourseName("Test Course1");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getTswTranscriptStudentCoursesByPenUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<TranscriptStudentCourse>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(transcriptStudentCourse)));

        val results = this.restUtils.getTranscriptStudentCourses(transcriptStudentCourse.getStudNo(), "123");
        assertThat(results).isNotNull();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStudNo()).isEqualTo(transcriptStudentCourse.getStudNo());
        assertThat(results.get(0).getCourseCode()).isEqualTo(transcriptStudentCourse.getCourseCode());
        assertThat(results.get(0).getCourseLevel()).isEqualTo(transcriptStudentCourse.getCourseLevel());
    }

    @Test
    public void testGetGradProgramRulesByTraxReqNumber_givenValues_returnsGradRuleDetails_withAPICallSuccess() {
        final String traxReqNumber = "5";

        final GradRuleDetails ruleDetails = new GradRuleDetails();
        ruleDetails.setRuleCode("100");
        ruleDetails.setTraxReqNumber(traxReqNumber);
        ruleDetails.setProgramCode("Test Program");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getGradProgramRulesByTraxReqNumberUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<GradRuleDetails>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(ruleDetails)));

        val result = this.restUtils.getGradProgramRulesByTraxReqNumber(traxReqNumber, "abc");
        assertThat(result).isNotNull();
        assertThat(result.size() > 0).isTrue();
    }

    @Test
    public void testGetGradProgramCode_givenValues_returnsGraduationProgramCode_withAPICallSuccess() {
        final GraduationProgramCode program = new GraduationProgramCode();
        program.setProgramCode("Test");
        program.setDescription("Test Program");
        program.setProgramName("Test Name");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getGradProgramUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(GraduationProgramCode.class)).thenReturn(Mono.just(program));

        val result = this.restUtils.getGradProgramCode(program.getProgramCode(), "abc");
        assertThat(result).isNotNull();
        assertThat(result.getProgramName()).isEqualTo(program.getProgramName());
    }

    @Test
    public void testGetGradProgram_givenValues_returnsGradProgram_withAPICallSuccess() {
        final GradProgram program = new GradProgram();
        program.setProgramCode("Test");
        program.setProgramName("Test Name");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getProgramNameEndpoint(), program.getProgramCode()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(GradProgram.class)).thenReturn(Mono.just(program));

        val result = this.restUtils.getGradProgram(program.getProgramCode(), "abc");
        assertThat(result).isNotNull();
        assertThat(result.getProgramName()).isEqualTo(program.getProgramName());
    }

    @Test
    public void testGetProgramRules_givenValues_returnProgramRequirements_withAPICallSuccess() {
        final ProgramRequirement programRequirement = new ProgramRequirement();
        programRequirement.setGraduationProgramCode("2018-EN");
        programRequirement.setProgramRequirementID(UUID.randomUUID());

        final ProgramRequirementCode programRequirementCode = new ProgramRequirementCode();
        programRequirementCode.setProReqCode("101");
        programRequirementCode.setLabel("101 Label");
        programRequirementCode.setDescription("101 Test Description");

        final RequirementTypeCode requirementTypeCode = new RequirementTypeCode();
        requirementTypeCode.setReqTypeCode("M");
        requirementTypeCode.setLabel("Match");
        requirementTypeCode.setDescription("Algorithm matches a specific course to a specific requirement");

        programRequirementCode.setRequirementTypeCode(requirementTypeCode);
        programRequirement.setProgramRequirementCode(programRequirementCode);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getGradProgramRulesUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<ProgramRequirement>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(programRequirement)));

        val result = this.restUtils.getGradProgramRules(programRequirement.getGraduationProgramCode(), "abc");
        assertThat(result).isNotNull();
        assertThat(result.size() > 0).isTrue();
    }

    @Test
    public void testGetSpecialCase_givenValue_returnSpecialCase_withAPICallSuccess() {
        GradProgram gP = new GradProgram();
        gP.setProgramCode("2018-EN");
        gP.setProgramName("2018 Graduation Program");

        SpecialCase sp = new SpecialCase();
        sp.setSpCase("A");
        sp.setLabel("AEG");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
//        when(this.requestHeadersUriMock.uri(String.format(constants.getSpecialCase(),"A"))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getSpecialCase()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(SpecialCase.class)).thenReturn(Mono.just(sp));

        val result = restUtils.getSpecialCase("A", "abc");
        assertThat(result).isNotNull();
    }

    @Test
    public void testGetSchoolCategoryCode_givenValue_returnString_withAPICallSuccess() {
        CommonSchool commSch = new CommonSchool();
        commSch.setSchlNo("1231123");
        commSch.setSchoolCategoryCode("02");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
//        when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolCategoryCode(),"06011033"))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getSchoolCategoryCode()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(CommonSchool.class)).thenReturn(Mono.just(commSch));

        val result = restUtils.getSchoolCategoryCode("06011033", "abc");
        assertThat(result).isNotNull();
    }

    @Test
    public void testGetTranscriptReport_givenValues_returnByteArray_withAPICallSuccess() {
        byte[] bytesSAR = RandomUtils.nextBytes(20);

        ReportRequest reportParams = new ReportRequest();
        ReportOptions options = new ReportOptions();
        options.setReportFile("transcript");
        options.setReportName("Transcript Report.pdf");

        reportParams.setOptions(options);

        ReportData reportData = new ReportData();
        reportData.setTranscript(new Transcript());

        reportParams.setData(reportData);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getTranscriptReport())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

        val result = restUtils.getTranscriptReport(reportParams, "abc");
        assertThat(result).isNotNull();
    }

    @Test
    public void testSaveGradStudentTranscript() {
        String studentID = new UUID(1, 1).toString();
        String accessToken = "accessToken";
        boolean isGraduated= true;
        GradStudentTranscripts rep = new GradStudentTranscripts();
        rep.setStudentID(UUID.fromString(studentID));
//        rep.setPen(pen);
        byte[] bytesSAR = RandomUtils.nextBytes(20);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getUpdateGradStudentTranscript(),isGraduated))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GradStudentTranscripts.class)).thenReturn(Mono.just(rep));

        restUtils.saveGradStudentTranscript(rep, isGraduated, accessToken);
        verify(this.webClient, times(1)).post();
    }

    @Test
    public void testGetProgramCertificateTranscriptList() {
        List<ProgramCertificateTranscript> clist= new ArrayList<ProgramCertificateTranscript>();
        ProgramCertificateTranscript pc = new ProgramCertificateTranscript();
        pc.setCertificateTypeCode("E");
        pc.setSchoolCategoryCode(" ");
        clist.add(pc);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getCertList())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<ProgramCertificateTranscript>>(){})).thenReturn(Mono.just(clist));

        ProgramCertificateReq req = new ProgramCertificateReq();
        req.setProgramCode("Test");
        req.setOptionalProgram("OptionalTest");
        req.setSchoolCategoryCode(" ");

        val result = restUtils.getProgramCertificateTranscriptList(req, "123");
        assertThat(result).isNotNull();
    }
}
