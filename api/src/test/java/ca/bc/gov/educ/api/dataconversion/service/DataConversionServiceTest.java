package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DataConversionServiceTest {

    @Autowired
    DataConversionService dataConversionService;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

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
    public void testLoadInitialRawGradStudentData() {
        ConvGradStudent obj = ConvGradStudent.builder()
                .pen("123456789")
                .schoolOfRecord("12345678")
                .schoolAtGrad("12345678")
                .studentGrade("12")
                .studentStatus("A")
                .archiveFlag("A")
                .graduationRequestYear("2020")
                .recalculateGradStatus("Y")
                .graduated(false)
            .build();
        List<ConvGradStudent> results = new ArrayList<>();
        results.add(obj);

        when(this.restUtils.getTraxStudentMasterDataByPen("123456789", "123")).thenReturn(results);

        var result = dataConversionService.getStudentMasterDataFromTrax("123456789", "123");
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        ConvGradStudent responseStudent = result.get(0);
        assertThat(responseStudent.getPen()).isEqualTo(obj.getPen());
    }

    @Test
    public void testLoadInitialRawGradCourseRestrictionsData() {
        CourseRestriction obj = CourseRestriction.builder()
                .mainCourse("main")
                .mainCourseLevel("12")
                .restrictedCourse("test")
                .restrictedCourseLevel("12")
            .build();
        List<CourseRestriction> results = new ArrayList<>();
        results.add(obj);

        when(this.restUtils.getTraxCourseRestrictions("123")).thenReturn(results);

        var result = dataConversionService.loadGradCourseRestrictionsDataFromTrax("123");
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        CourseRestriction responseCourseRestriction = result.get(0);
        assertThat(responseCourseRestriction.getMainCourse()).isEqualTo("main");
    }

    @Test
    public void testGetStudentDemographicsDataFromTrax() {
//        Object[] obj = new Object[] {
//                "123456789", "Test", "QA", "", Character.valueOf('A'),Character.valueOf('A'), "12345678", "12", "V4N3Y2", Character.valueOf('M'), "19800111",  BigDecimal.valueOf(202005), null, "            "
//        };
        Student obj = Student.builder()
                .pen("123456789")
                .legalFirstName("Test")
                .legalLastName("QA")
                .mincode("12345678")
                .gradeCode("12")
                .genderCode("M")
                .postalCode("V4N3Y2")
                .statusCode("A")
                .gradeYear("202005")
                .dob("19800111")
                .build();
        List<Student> results = new ArrayList<>();
        results.add(obj);

        when(this.restUtils.getTraxStudentDemographicsDataByPen("123456789", "123")).thenReturn(results);

        var result = dataConversionService.getStudentDemographicsDataFromTrax("123456789", "123");
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getPen()).isEqualTo("123456789");
    }

    @Test
    public void testReadTraxStudentAndAddNewPen_whenPenAlreadyExists() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "123456789";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        // Trax Student Input
        TraxStudentNo convGradStudent = new TraxStudentNo();
        convGradStudent.setStudNo(pen);

        Student obj = Student.builder()
                .pen("123456789")
                .legalFirstName("Test")
                .legalLastName("QA")
                .mincode("12345678")
                .gradeCode("12")
                .genderCode("M")
                .postalCode("V4N3Y2")
                .statusCode("A")
                .gradeYear("202005")
                .dob("19800111")
                .build();
        List<Student> results = new ArrayList<>();
        results.add(obj);

        when(this.restUtils.getTraxStudentDemographicsDataByPen(pen, "123")).thenReturn(results);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = dataConversionService.readTraxStudentAndAddNewPen(convGradStudent, summary);
        assertThat(result).isNotNull();
        assertThat(result.getStudNo()).isEqualTo(pen);
    }

    @Test
    public void testReadTraxStudentAndAddNewPen() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "123456789";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        // Trax Student Input
        TraxStudentNo convGradStudent = new TraxStudentNo();
        convGradStudent.setStudNo(pen);

        Student obj = Student.builder()
                .pen("123456789")
                .legalFirstName("Test")
                .legalLastName("QA")
                .mincode("12345678")
                .gradeCode("12")
                .genderCode("M")
                .postalCode("V4N3Y2")
                .statusCode("A")
                .gradeYear("202005")
                .dob("19800111")
                .build();
        List<Student> results = new ArrayList<>();
        results.add(obj);

        when(this.restUtils.getTraxStudentDemographicsDataByPen(pen, "123")).thenReturn(results);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(new ArrayList<>());
        when(this.restUtils.addNewPen(any(Student.class), eq("123"))).thenReturn(penStudent);

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = dataConversionService.readTraxStudentAndAddNewPen(convGradStudent, summary);
        assertThat(result).isNotNull();
        assertThat(result.getStudNo()).isEqualTo(pen);
    }

    @Test
    public void testReadTraxStudentAndAddNewPen_throwsException_whenAddNewPenAPIisInvoked() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "123456789";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        // Trax Student Input
        TraxStudentNo convGradStudent = new TraxStudentNo();
        convGradStudent.setStudNo(pen);

        Student obj = Student.builder()
                .pen("123456789")
                .legalFirstName("Test")
                .legalLastName("QA")
                .mincode("12345678")
                .gradeCode("12")
                .genderCode("M")
                .postalCode("V4N3Y2")
                .statusCode("A")
                .gradeYear("202005")
                .dob("19800111")
                .build();
        List<Student> results = new ArrayList<>();
        results.add(obj);

        when(this.restUtils.getTraxStudentDemographicsDataByPen(pen, "123")).thenReturn(results);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(new ArrayList<>());
        when(this.restUtils.addNewPen(any(Student.class), eq("123"))).thenThrow(new RuntimeException("Test Exception"));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = dataConversionService.readTraxStudentAndAddNewPen(convGradStudent, summary);
        assertThat(result).isNull();
    }

    @Test
    public void testReadTraxStudentAndAddNewPen_throwsException_whenGetStudentPenAPIisInvoked() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "123456789";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        // Trax Student Input
        TraxStudentNo convGradStudent = new TraxStudentNo();
        convGradStudent.setStudNo(pen);

        Student obj = Student.builder()
                .pen("123456789")
                .legalFirstName("Test")
                .legalLastName("QA")
                .mincode("12345678")
                .gradeCode("12")
                .genderCode("M")
                .postalCode("V4N3Y2")
                .statusCode("A")
                .gradeYear("202005")
                .dob("19800111")
                .build();
        List<Student> results = new ArrayList<>();
        results.add(obj);

        when(this.restUtils.getTraxStudentDemographicsDataByPen(pen, "123")).thenReturn(results);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenThrow(new RuntimeException("Test Exception"));
        when(this.restUtils.addNewPen(any(Student.class), eq("123"))).thenReturn(penStudent);

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = dataConversionService.readTraxStudentAndAddNewPen(convGradStudent, summary);
        assertThat(result).isNotNull();
        assertThat(result.getStudNo()).isEqualTo(pen);
    }

    @Test
    public void testReadTraxStudentAndAddNewPen_whenStudentIsMerged() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "123456789";

        // Trax Student Input
        TraxStudentNo convGradStudent = new TraxStudentNo();
        convGradStudent.setStudNo(pen);

        Student obj = Student.builder()
                .pen("123456789")
                .legalFirstName("Test")
                .legalLastName("QA")
                .mincode("12345678")
                .gradeCode("12")
                .genderCode("M")
                .postalCode("V4N3Y2")
                .statusCode("A")
                .gradeYear("202005")
                .dob("19800111")
                .build();
        List<Student> results = new ArrayList<>();
        results.add(obj);

        when(this.restUtils.getTraxStudentDemographicsDataByPen(pen, "123")).thenReturn(results);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(new ArrayList<>());

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = dataConversionService.readTraxStudentAndAddNewPen(convGradStudent, summary);
        assertThat(result).isNotNull();
        assertThat(result.getStudNo()).isEqualTo(pen);
    }
}
