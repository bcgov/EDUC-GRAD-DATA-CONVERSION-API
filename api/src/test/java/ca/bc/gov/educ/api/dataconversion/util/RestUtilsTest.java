package ca.bc.gov.educ.api.dataconversion.util;

import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.StudentAssessment;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    public void testTokenFallbackMethod_givenException_shouldReturnNull(){
        val result = this.restUtils.rtGetTokenFallback(new HttpServerErrorException(HttpStatus.I_AM_A_TEAPOT));
        assertThat(result).isNull();
    }

    @Test
    public void testGetStudentFallbackMethod_givenException_shouldReturnNull(){
        val result = this.restUtils.rtGetStudentGradStatusFallback(new HttpServerErrorException(HttpStatus.I_AM_A_TEAPOT));
        assertThat(result).isNull();
    }

    @Test
    public void testSaveStudentFallbackMethod_givenException_shouldReturnNull(){
        val result = this.restUtils.rtSaveStudentGradStatusFallback(new HttpServerErrorException(HttpStatus.I_AM_A_TEAPOT));
        assertThat(result).isNull();
    }

    @Test
    public void testUpdateStudentFallbackMethod_givenException_shouldReturnNull(){
        val result = this.restUtils.rtUpdateStudentGradStatusFallback(new HttpServerErrorException(HttpStatus.I_AM_A_TEAPOT));
        assertThat(result).isNull();
    }

    @Test
    public void testGetStudentNotesByStudentId_returnsTrue_with_APICallSuccess() {

        final String studentID = "0a614e84-7e27-1815-817e-fad384090090";
        final String noteID = "0a614e84-7e27-1815-817e-fad384090090";
        String accessToken = "Bearer accesstoken";
        StudentNote note = new StudentNote();
        note.setStudentID(studentID);
        note.setNote("Sample Note");
        note.setId(UUID.fromString(noteID));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGradStudentNotesByStudentID(), studentID)))
                .thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<StudentNote>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(note)));

        val result = this.restUtils.getStudentNotesByStudentId(studentID, accessToken);
        assertThat(result).isNotNull();
        assertThat(result.size() > 0).isTrue();
        assertThat(result.get(0).getStudentID()).isEqualTo(studentID);
    }
}
