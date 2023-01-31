package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.entity.student.*;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.School;
import ca.bc.gov.educ.api.dataconversion.model.tsw.SpecialCase;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.ReportData;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import ca.bc.gov.educ.api.dataconversion.repository.student.*;
import ca.bc.gov.educ.api.dataconversion.service.assessment.AssessmentService;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import ca.bc.gov.educ.api.dataconversion.service.student.ReportService;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.GradConversionTestUtils;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
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
public class StudentServiceWithMockRepositoryTest {

    @Autowired
    StudentService studentService;

    @MockBean
    GraduationStudentRecordRepository graduationStudentRecordRepository;

    @MockBean
    StudentOptionalProgramRepository studentOptionalProgramRepository;

    @MockBean
    StudentCareerProgramRepository studentCareerProgramRepository;

    @MockBean
    GraduationStudentRecordHistoryRepository graduationStudentRecordHistoryRepository;

    @MockBean
    StudentOptionalProgramHistoryRepository studentOptionalProgramHistoryRepository;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    CourseService courseService;

    @MockBean
    AssessmentService assessmentService;

    @MockBean
    ReportService reportService;

    @MockBean
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

    // NATS
    @MockBean
    private NatsConnection natsConnection;
    @MockBean
    private Subscriber subscriber;

    @Autowired
    private EducGradDataConversionApiConstants constants;

    @Autowired
    GradConversionTestUtils gradConversionTestUtils;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {
        graduationStudentRecordRepository.deleteAll();
    }

    @Test
    public void convertStudent_forSchoolValidation_whenTraxAPIisDown_thenReturnFailure() throws Exception {
        String pen = "111222333";
        String mincode = "222333";

        when(this.restUtils.checkSchoolExists(mincode, "123")).thenThrow(IllegalArgumentException.class);

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("SCCP")
                .studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("SCCP")
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo(ConversionResultType.FAILURE);
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("Grad Trax API is failed for");
    }

    @Test
    public void convertStudent_forSchoolValidation_whenGivenSchool_doesNotExist_thenReturnFailure() throws Exception {
        String pen = "111222333";
        String mincode = "222333";

        when(this.restUtils.checkSchoolExists(mincode, "123")).thenReturn(false);

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("SCCP")
                .studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("SCCP")
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("Invalid school of record");
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withSccpProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("SCCP");
        gradStudentEntity.setStudentGrade("11");
        gradStudentEntity.setStudentStatus("CUR");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.of(gradStudentEntity));
        when(this.graduationStudentRecordRepository.save(gradStudentEntity)).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(null);
        when(this.restUtils.checkSchoolExists("222333", "123")).thenReturn(true);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("SCCP")
                .studentStatus("A").schoolOfRecord("222333").graduationRequirementYear("SCCP")
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withAdultProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setDob("2000-06-30");
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("1950");
        gradStudentEntity.setStudentGrade("AD");
        gradStudentEntity.setStudentStatus("CUR");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.of(gradStudentEntity));
        when(this.graduationStudentRecordRepository.save(gradStudentEntity)).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(null);
        when(this.restUtils.checkSchoolExists("222333", "123")).thenReturn(true);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1950")
                .studentGrade("AD").studentStatus("A")
                .schoolOfRecord("222333").graduationRequirementYear("1950")
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1950");
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("2018-EN");
        gradStudentEntity.setStudentGrade("12");
        gradStudentEntity.setStudentStatus("CUR");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgramEntity specialProgramEntity = new StudentOptionalProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        StudentCareerProgramEntity careerProgramEntity = new StudentCareerProgramEntity();
        careerProgramEntity.setId(UUID.randomUUID());
        careerProgramEntity.setStudentID(studentID);
        careerProgramEntity.setCareerProgramCode("XC");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.of(gradStudentEntity));
        when(this.graduationStudentRecordRepository.save(gradStudentEntity)).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(null);
        when(this.restUtils.checkSchoolExists("222333", "123")).thenReturn(true);
        when(this.studentOptionalProgramRepository.save(specialProgramEntity)).thenReturn(specialProgramEntity);
        when(this.studentCareerProgramRepository.save(careerProgramEntity)).thenReturn(careerProgramEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN")
                .studentStatus("A").schoolOfRecord("222333").graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("2018");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertStudent_whenGivenData_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("2018-EN");
        gradStudentEntity.setStudentGrade("12");
        gradStudentEntity.setStudentStatus("CUR");

        GraduationStudentRecordHistoryEntity gradStudentHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(gradStudentEntity, gradStudentHistoryEntity);
//        gradStudentHistoryEntity.setHistoryID(UUID.randomUUID());
        gradStudentHistoryEntity.setActivityCode("DATACONVERT");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgramEntity specialProgramEntity = new StudentOptionalProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        StudentOptionalProgramHistoryEntity specialProgramHistoryEntity = new StudentOptionalProgramHistoryEntity();
        BeanUtils.copyProperties(specialProgramEntity, specialProgramHistoryEntity);
        specialProgramHistoryEntity.setStudentOptionalProgramID(specialProgramEntity.getId());
        specialProgramHistoryEntity.setActivityCode("DATACONVERT");

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgramEntity studentCareerProgramEntity = new StudentCareerProgramEntity();
        studentCareerProgramEntity.setId(UUID.randomUUID());
        studentCareerProgramEntity.setStudentID(studentID);
        studentCareerProgramEntity.setCareerProgramCode("XC");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.empty());
        when(this.graduationStudentRecordRepository.save(any(GraduationStudentRecordEntity.class))).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.checkSchoolExists("222333", "123")).thenReturn(true);
        when(this.studentOptionalProgramRepository.save(any(StudentOptionalProgramEntity.class))).thenReturn(specialProgramEntity);
        when(this.studentCareerProgramRepository.save(studentCareerProgramEntity)).thenReturn(studentCareerProgramEntity);
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.studentOptionalProgramHistoryRepository.save(specialProgramHistoryEntity)).thenReturn(specialProgramHistoryEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN")
                .studentStatus("A").schoolOfRecord("222333").graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("2018");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertStudent_whenGivenData_forMergedStatus_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("2018-EN");
        gradStudentEntity.setStudentGrade("12");
        gradStudentEntity.setStudentStatus("MER");

        GraduationStudentRecordHistoryEntity gradStudentHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(gradStudentEntity, gradStudentHistoryEntity);
//        gradStudentHistoryEntity.setHistoryID(UUID.randomUUID());
        gradStudentHistoryEntity.setActivityCode("DATACONVERT");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgramEntity specialProgramEntity = new StudentOptionalProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        StudentOptionalProgramHistoryEntity specialProgramHistoryEntity = new StudentOptionalProgramHistoryEntity();
        BeanUtils.copyProperties(specialProgramEntity, specialProgramHistoryEntity);
        specialProgramHistoryEntity.setStudentOptionalProgramID(specialProgramEntity.getId());
        specialProgramHistoryEntity.setActivityCode("DATACONVERT");

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgramEntity studentCareerProgramEntity = new StudentCareerProgramEntity();
        studentCareerProgramEntity.setId(UUID.randomUUID());
        studentCareerProgramEntity.setStudentID(studentID);
        studentCareerProgramEntity.setCareerProgramCode("XC");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.empty());
        when(this.graduationStudentRecordRepository.save(any(GraduationStudentRecordEntity.class))).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.checkSchoolExists("222333", "123")).thenReturn(true);
        when(this.studentOptionalProgramRepository.save(any(StudentOptionalProgramEntity.class))).thenReturn(specialProgramEntity);
        when(this.studentCareerProgramRepository.save(studentCareerProgramEntity)).thenReturn(studentCareerProgramEntity);
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.studentOptionalProgramHistoryRepository.save(specialProgramHistoryEntity)).thenReturn(specialProgramHistoryEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN")
                .studentStatus("M").schoolOfRecord("222333").graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("2018");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertStudent_whenGiven1996Data_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("1996-EN");
        gradStudentEntity.setStudentGrade("12");
        gradStudentEntity.setStudentStatus("CUR");

        GraduationStudentRecordHistoryEntity gradStudentHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(gradStudentEntity, gradStudentHistoryEntity);
//        gradStudentHistoryEntity.setHistoryID(UUID.randomUUID());
        gradStudentHistoryEntity.setActivityCode("DATACONVERT");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("1996-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgramEntity specialProgramEntity = new StudentOptionalProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        StudentOptionalProgramHistoryEntity specialProgramHistoryEntity = new StudentOptionalProgramHistoryEntity();
        BeanUtils.copyProperties(specialProgramEntity, specialProgramHistoryEntity);
        specialProgramHistoryEntity.setStudentOptionalProgramID(specialProgramEntity.getId());
        specialProgramHistoryEntity.setActivityCode("DATACONVERT");

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgramEntity studentCareerProgramEntity = new StudentCareerProgramEntity();
        studentCareerProgramEntity.setId(UUID.randomUUID());
        studentCareerProgramEntity.setStudentID(studentID);
        studentCareerProgramEntity.setCareerProgramCode("XC");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.empty());
        when(this.graduationStudentRecordRepository.save(any(GraduationStudentRecordEntity.class))).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.checkSchoolExists("222333", "123")).thenReturn(true);
        when(this.studentOptionalProgramRepository.save(any(StudentOptionalProgramEntity.class))).thenReturn(specialProgramEntity);
        when(this.studentCareerProgramRepository.save(studentCareerProgramEntity)).thenReturn(studentCareerProgramEntity);
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.studentOptionalProgramHistoryRepository.save(specialProgramHistoryEntity)).thenReturn(specialProgramHistoryEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("1996-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1996-EN")
                .studentStatus("A").schoolOfRecord("222333").graduationRequirementYear("1996")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1996");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertStudent_whenGiven1986Data_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("1986-EN");
        gradStudentEntity.setStudentGrade("12");
        gradStudentEntity.setStudentStatus("CUR");

        GraduationStudentRecordHistoryEntity gradStudentHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(gradStudentEntity, gradStudentHistoryEntity);
//        gradStudentHistoryEntity.setHistoryID(UUID.randomUUID());
        gradStudentHistoryEntity.setActivityCode("DATACONVERT");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("1986-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgramEntity specialProgramEntity = new StudentOptionalProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        StudentOptionalProgramHistoryEntity specialProgramHistoryEntity = new StudentOptionalProgramHistoryEntity();
        BeanUtils.copyProperties(specialProgramEntity, specialProgramHistoryEntity);
        specialProgramHistoryEntity.setStudentOptionalProgramID(specialProgramEntity.getId());
        specialProgramHistoryEntity.setActivityCode("DATACONVERT");

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgramEntity studentCareerProgramEntity = new StudentCareerProgramEntity();
        studentCareerProgramEntity.setId(UUID.randomUUID());
        studentCareerProgramEntity.setStudentID(studentID);
        studentCareerProgramEntity.setCareerProgramCode("XC");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.empty());
        when(this.graduationStudentRecordRepository.save(any(GraduationStudentRecordEntity.class))).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.checkSchoolExists("222333", "123")).thenReturn(true);
        when(this.studentOptionalProgramRepository.save(any(StudentOptionalProgramEntity.class))).thenReturn(specialProgramEntity);
        when(this.studentCareerProgramRepository.save(studentCareerProgramEntity)).thenReturn(studentCareerProgramEntity);
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.studentOptionalProgramHistoryRepository.save(specialProgramHistoryEntity)).thenReturn(specialProgramHistoryEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("1986-EN", "FI", "123")).thenReturn(specialProgram);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1986-EN")
                .studentStatus("A").schoolOfRecord("222333").graduationRequirementYear("1986")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1986");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertGraduatedStudent_whenGiven1986Data_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("1986-EN");
        gradStudentEntity.setStudentGrade("12");
        gradStudentEntity.setStudentStatus("CUR");
        gradStudentEntity.setHonoursStanding("Y");
        gradStudentEntity.setSchoolAtGrad(mincode);
        gradStudentEntity.setSchoolOfRecord(mincode);
        gradStudentEntity.setEnglishCert("E");
        gradStudentEntity.setFrenchCert("F");
        gradStudentEntity.setProgramCompletionDate(new Date(System.currentTimeMillis() - 600000L));

        GraduationStudentRecordHistoryEntity gradStudentHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(gradStudentEntity, gradStudentHistoryEntity);
//        gradStudentHistoryEntity.setHistoryID(UUID.randomUUID());
        gradStudentHistoryEntity.setActivityCode("DATACONVERT");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("1986-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgramEntity specialProgramEntity = new StudentOptionalProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        StudentOptionalProgramHistoryEntity specialProgramHistoryEntity = new StudentOptionalProgramHistoryEntity();
        BeanUtils.copyProperties(specialProgramEntity, specialProgramHistoryEntity);
        specialProgramHistoryEntity.setStudentOptionalProgramID(specialProgramEntity.getId());
        specialProgramHistoryEntity.setActivityCode("DATACONVERT");

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgramEntity studentCareerProgramEntity = new StudentCareerProgramEntity();
        studentCareerProgramEntity.setId(UUID.randomUUID());
        studentCareerProgramEntity.setStudentID(studentID);
        studentCareerProgramEntity.setCareerProgramCode("XC");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setSchoolName("Test School");
        tranStudentDemog.setStudentGrade("12");
        tranStudentDemog.setGradReqtYear("1986");
        tranStudentDemog.setUpdateDate(20220601L);

        // TSW
        TranscriptStudentCourse tswCourse1 = new TranscriptStudentCourse();
        tswCourse1.setStudNo(pen);
        tswCourse1.setReportType("1");
        tswCourse1.setCourseCode("Generic");
        tswCourse1.setCourseName("Generic Course Name");
        tswCourse1.setCourseLevel("12");
        tswCourse1.setFinalPercentage("91.00");
        tswCourse1.setFinalLG("A");
        tswCourse1.setCourseSession("202206");
        tswCourse1.setNumberOfCredits("4");
        tswCourse1.setUsedForGrad("4");
        tswCourse1.setFoundationReq("10");
        tswCourse1.setUpdateDate(20220601L);

        TranscriptStudentCourse tswCourse2 = new TranscriptStudentCourse();
        tswCourse2.setStudNo(pen);
        tswCourse2.setReportType("2");
        tswCourse2.setCourseCode("TestCourse");
        tswCourse2.setCourseName("Test Course Name");
        tswCourse2.setCourseLevel("12");
        tswCourse2.setFinalPercentage("92.00");
        tswCourse2.setFinalLG("A");
        tswCourse2.setCourseSession("202206");
        tswCourse2.setNumberOfCredits("4");
        tswCourse2.setUsedForGrad("4");
        tswCourse2.setFoundationReq("11");
        tswCourse2.setSpecialCase("E");
        tswCourse2.setUpdateDate(20220601L);

        TranscriptStudentCourse tswCourse3 = new TranscriptStudentCourse();
        tswCourse3.setStudNo(pen);
        tswCourse3.setReportType("2");
        tswCourse3.setCourseCode("TestCourse2");
        tswCourse3.setCourseName("Test Course2 Name");
        tswCourse3.setCourseLevel("12");
        tswCourse3.setFinalPercentage("XMT");
        tswCourse3.setFinalLG("A");
        tswCourse3.setCourseSession("202206");
        tswCourse3.setNumberOfCredits("4");
        tswCourse3.setUsedForGrad("4");
        tswCourse3.setFoundationReq("11");
        tswCourse3.setUpdateDate(20220601L);

        TranscriptStudentCourse tswAssessment = new TranscriptStudentCourse();
        tswAssessment.setStudNo(pen);
        tswAssessment.setReportType("3");
        tswAssessment.setCourseCode("TestAssmt");
        tswAssessment.setCourseName("Test Assessment Name");
        tswAssessment.setCourseLevel("12");
        tswAssessment.setFinalPercentage("XMT");
        tswAssessment.setCourseSession("202206");
        tswAssessment.setFoundationReq("15");
        tswAssessment.setUpdateDate(new Date(System.currentTimeMillis() - 100000L).getTime());

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        // Rule 10
        ProgramRequirement pr10 = new ProgramRequirement();
        pr10.setProgramRequirementID(UUID.randomUUID());
        pr10.setGraduationProgramCode("1986-EN");
        ProgramRequirementCode rule10 = new ProgramRequirementCode();
        rule10.setTraxReqNumber("10");
        rule10.setProReqCode("110");
        rule10.setLabel("Rule 10 Test Label");
        rule10.setDescription("Rule 10 Test Description");

        pr10.setProgramRequirementCode(rule10);

        GradRuleDetails gradRule10Details = new GradRuleDetails();
        gradRule10Details.setRuleCode("110");
        gradRule10Details.setTraxReqNumber("10");
        gradRule10Details.setProgramCode("1986-EN");
        gradRule10Details.setRequirementName("Rule 10 Test Label");

        // Rule 11
        ProgramRequirement pr11 = new ProgramRequirement();
        pr11.setProgramRequirementID(UUID.randomUUID());
        pr11.setGraduationProgramCode("1986-EN");
        ProgramRequirementCode rule11 = new ProgramRequirementCode();
        rule11.setTraxReqNumber("11");
        rule11.setProReqCode("111");
        rule11.setLabel("Rule 11 Test Label");
        rule11.setDescription("Rule 11 Test Description");

        pr11.setProgramRequirementCode(rule11);

        GradRuleDetails gradRule11Details = new GradRuleDetails();
        gradRule11Details.setRuleCode("111");
        gradRule11Details.setTraxReqNumber("11");
        gradRule11Details.setProgramCode("1986-EN");
        gradRule11Details.setRequirementName("Rule 11 Test Label");

        // Rule 15
        ProgramRequirement pr15 = new ProgramRequirement();
        pr15.setProgramRequirementID(UUID.randomUUID());
        pr15.setGraduationProgramCode("1986-EN");
        ProgramRequirementCode rule15 = new ProgramRequirementCode();
        rule15.setTraxReqNumber("15");
        rule15.setProReqCode("115");
        rule15.setLabel("Rule 15 Test Label");
        rule15.setDescription("Rule 15 Test Description");

        pr15.setProgramRequirementCode(rule15);

        GradRuleDetails gradRule15Details = new GradRuleDetails();
        gradRule15Details.setRuleCode("115");
        gradRule15Details.setTraxReqNumber("15");
        gradRule15Details.setProgramCode("1986-EN");
        gradRule15Details.setRequirementName("Rule 15 Test Label");

        // SpecialCase
        SpecialCase sc = new SpecialCase();
        sc.setSpCase("E");
        sc.setLabel("XMT");
        sc.setDescription("Exempt");
        sc.setPassFlag("Y");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.empty());
        when(this.graduationStudentRecordRepository.save(any(GraduationStudentRecordEntity.class))).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.checkSchoolExists(mincode, "123")).thenReturn(true);
        when(this.studentOptionalProgramRepository.findByStudentID(studentID)).thenReturn(Arrays.asList(specialProgramEntity));
        when(this.studentOptionalProgramRepository.save(any(StudentOptionalProgramEntity.class))).thenReturn(specialProgramEntity);
        when(this.studentCareerProgramRepository.save(studentCareerProgramEntity)).thenReturn(studentCareerProgramEntity);
        when(this.studentCareerProgramRepository.findByStudentID(studentID)).thenReturn(Arrays.asList(studentCareerProgramEntity));
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.studentOptionalProgramHistoryRepository.save(specialProgramHistoryEntity)).thenReturn(specialProgramHistoryEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgramByID(specialProgram.getOptionalProgramID(), "123")).thenReturn(specialProgram);
        when(this.restUtils.getOptionalProgram("1986-EN", "FI", "123")).thenReturn(specialProgram);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);
        when(this.restUtils.getSchoolGrad(mincode, "123")).thenReturn(school);
        when(this.restUtils.getGradProgramRules("1986-EN", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1986-EN")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .graduated(true)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("I")
                .slpDate("0")
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("1986")
                .programCodes(Arrays.asList("XC")).build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswCourse3, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.reportService.prepareTranscriptData(any(), any(), eq(summary.getAccessToken()))).thenReturn(new ReportData());

        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1986");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertGraduatedStudent_whenGiven2018Data_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("2018-EN");
        gradStudentEntity.setStudentGrade("12");
        gradStudentEntity.setStudentStatus("CUR");
        gradStudentEntity.setHonoursStanding("Y");
        gradStudentEntity.setSchoolAtGrad(mincode);
        gradStudentEntity.setSchoolOfRecord(mincode);
        gradStudentEntity.setEnglishCert("E");
        gradStudentEntity.setFrenchCert("F");
        gradStudentEntity.setProgramCompletionDate(new Date(System.currentTimeMillis() - 600000L));

        GraduationStudentRecordHistoryEntity gradStudentHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(gradStudentEntity, gradStudentHistoryEntity);
//        gradStudentHistoryEntity.setHistoryID(UUID.randomUUID());
        gradStudentHistoryEntity.setActivityCode("DATACONVERT");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgramEntity specialProgramEntity = new StudentOptionalProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        StudentOptionalProgramHistoryEntity specialProgramHistoryEntity = new StudentOptionalProgramHistoryEntity();
        BeanUtils.copyProperties(specialProgramEntity, specialProgramHistoryEntity);
        specialProgramHistoryEntity.setStudentOptionalProgramID(specialProgramEntity.getId());
        specialProgramHistoryEntity.setActivityCode("DATACONVERT");

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgramEntity studentCareerProgramEntity = new StudentCareerProgramEntity();
        studentCareerProgramEntity.setId(UUID.randomUUID());
        studentCareerProgramEntity.setStudentID(studentID);
        studentCareerProgramEntity.setCareerProgramCode("XC");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setSchoolName("Test School");
        tranStudentDemog.setStudentGrade("12");
        tranStudentDemog.setGradReqtYear("2018");
        tranStudentDemog.setUpdateDate(20220601L);

        // TSW
        TranscriptStudentCourse tswCourse1 = new TranscriptStudentCourse();
        tswCourse1.setStudNo(pen);
        tswCourse1.setReportType("1");
        tswCourse1.setCourseCode("Generic");
        tswCourse1.setCourseName("Generic Course Name");
        tswCourse1.setCourseLevel("12");
        tswCourse1.setFinalPercentage("91.00");
        tswCourse1.setFinalLG("A");
        tswCourse1.setCourseSession("202206");
        tswCourse1.setNumberOfCredits("4");
        tswCourse1.setUsedForGrad("4");
        tswCourse1.setFoundationReq("10");
        tswCourse1.setUpdateDate(20220601L);

        TranscriptStudentCourse tswCourse2 = new TranscriptStudentCourse();
        tswCourse2.setStudNo(pen);
        tswCourse2.setReportType("2");
        tswCourse2.setCourseCode("TestCourse");
        tswCourse2.setCourseName("Test Course Name");
        tswCourse2.setCourseLevel("12");
        tswCourse2.setFinalPercentage("92.00");
        tswCourse2.setFinalLG("A");
        tswCourse2.setCourseSession("202206");
        tswCourse2.setNumberOfCredits("4");
        tswCourse2.setUsedForGrad("4");
        tswCourse2.setFoundationReq("11");
        tswCourse2.setUpdateDate(20220601L);

        TranscriptStudentCourse tswAssessment = new TranscriptStudentCourse();
        tswAssessment.setStudNo(pen);
        tswAssessment.setReportType("3");
        tswAssessment.setCourseCode("TestAssmt");
        tswAssessment.setCourseName("Test Assessment Name");
        tswAssessment.setCourseLevel("12");
        tswAssessment.setFinalPercentage("XMT");
        tswAssessment.setCourseSession("202206");
        tswAssessment.setFoundationReq("15");
        tswAssessment.setUpdateDate(new Date(System.currentTimeMillis() - 100000L).getTime());

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        // Rule 10
        ProgramRequirement pr10 = new ProgramRequirement();
        pr10.setProgramRequirementID(UUID.randomUUID());
        pr10.setGraduationProgramCode("2018-EN");
        ProgramRequirementCode rule10 = new ProgramRequirementCode();
        rule10.setTraxReqNumber("10");
        rule10.setProReqCode("110");
        rule10.setLabel("Rule 10 Test Label");
        rule10.setDescription("Rule 10 Test Description");

        pr10.setProgramRequirementCode(rule10);

        GradRuleDetails gradRule10Details = new GradRuleDetails();
        gradRule10Details.setRuleCode("110");
        gradRule10Details.setTraxReqNumber("10");
        gradRule10Details.setProgramCode("2018-EN");
        gradRule10Details.setRequirementName("Rule 10 Test Label");

        // Rule 11
        ProgramRequirement pr11 = new ProgramRequirement();
        pr11.setProgramRequirementID(UUID.randomUUID());
        pr11.setGraduationProgramCode("2018-EN");
        ProgramRequirementCode rule11 = new ProgramRequirementCode();
        rule11.setTraxReqNumber("11");
        rule11.setProReqCode("111");
        rule11.setLabel("Rule 11 Test Label");
        rule11.setDescription("Rule 11 Test Description");

        pr11.setProgramRequirementCode(rule11);

        GradRuleDetails gradRule11Details = new GradRuleDetails();
        gradRule11Details.setRuleCode("111");
        gradRule11Details.setTraxReqNumber("11");
        gradRule11Details.setProgramCode("2018-EN");
        gradRule11Details.setRequirementName("Rule 11 Test Label");

        // Rule 15
        ProgramRequirement pr15 = new ProgramRequirement();
        pr15.setProgramRequirementID(UUID.randomUUID());
        pr15.setGraduationProgramCode("2018-EN");
        ProgramRequirementCode rule15 = new ProgramRequirementCode();
        rule15.setTraxReqNumber("15");
        rule15.setProReqCode("115");
        rule15.setLabel("Rule 15 Test Label");
        rule15.setDescription("Rule 15 Test Description");

        pr15.setProgramRequirementCode(rule15);

        GradRuleDetails gradRule15Details = new GradRuleDetails();
        gradRule15Details.setRuleCode("115");
        gradRule15Details.setTraxReqNumber("15");
        gradRule15Details.setProgramCode("2018-EN");
        gradRule15Details.setRequirementName("Rule 15 Test Label");

        // SpecialCase
        SpecialCase sc = new SpecialCase();
        sc.setSpCase("E");
        sc.setLabel("XMT");
        sc.setDescription("Exempt");
        sc.setPassFlag("Y");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.empty());
        when(this.graduationStudentRecordRepository.save(any(GraduationStudentRecordEntity.class))).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.checkSchoolExists(mincode, "123")).thenReturn(true);
        when(this.studentOptionalProgramRepository.findByStudentID(studentID)).thenReturn(Arrays.asList(specialProgramEntity));
        when(this.studentOptionalProgramRepository.save(any(StudentOptionalProgramEntity.class))).thenReturn(specialProgramEntity);
        when(this.studentCareerProgramRepository.save(studentCareerProgramEntity)).thenReturn(studentCareerProgramEntity);
        when(this.studentCareerProgramRepository.findByStudentID(studentID)).thenReturn(Arrays.asList(studentCareerProgramEntity));
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.studentOptionalProgramHistoryRepository.save(specialProgramHistoryEntity)).thenReturn(specialProgramHistoryEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgramByID(specialProgram.getOptionalProgramID(), "123")).thenReturn(specialProgram);
        when(this.restUtils.getOptionalProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);
        when(this.restUtils.getSchoolGrad(mincode, "123")).thenReturn(school);
        when(this.restUtils.getGradProgramRules("2018-EN", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .graduated(true)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("I")
                .slpDate("0")
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.reportService.prepareTranscriptData(any(), any(), eq(summary.getAccessToken()))).thenReturn(new ReportData());

        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("2018");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertGraduatedStudent_whenGiven2018PFData_witDualDogwoodProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "093333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("2018-PF");
        gradStudentEntity.setStudentGrade("12");
        gradStudentEntity.setStudentStatus("CUR");
        gradStudentEntity.setHonoursStanding("Y");
        gradStudentEntity.setSchoolAtGrad(mincode);
        gradStudentEntity.setSchoolOfRecord(mincode);
        gradStudentEntity.setEnglishCert("E");
        gradStudentEntity.setFrenchCert("S");
        gradStudentEntity.setProgramCompletionDate(new Date(System.currentTimeMillis() - 600000L));

        GraduationStudentRecordHistoryEntity gradStudentHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(gradStudentEntity, gradStudentHistoryEntity);
//        gradStudentHistoryEntity.setHistoryID(UUID.randomUUID());
        gradStudentHistoryEntity.setActivityCode("DATACONVERT");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-PF");
        specialProgram.setOptProgramCode("DD");
        specialProgram.setOptionalProgramName("Dual Dogwood");

        StudentOptionalProgramEntity specialProgramEntity = new StudentOptionalProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        StudentOptionalProgramHistoryEntity specialProgramHistoryEntity = new StudentOptionalProgramHistoryEntity();
        BeanUtils.copyProperties(specialProgramEntity, specialProgramHistoryEntity);
        specialProgramHistoryEntity.setStudentOptionalProgramID(specialProgramEntity.getId());
        specialProgramHistoryEntity.setActivityCode("DATACONVERT");

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgramEntity studentCareerProgramEntity = new StudentCareerProgramEntity();
        studentCareerProgramEntity.setId(UUID.randomUUID());
        studentCareerProgramEntity.setStudentID(studentID);
        studentCareerProgramEntity.setCareerProgramCode("XC");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setSchoolName("Test School");
        tranStudentDemog.setStudentGrade("12");
        tranStudentDemog.setGradReqtYear("2018");
        tranStudentDemog.setUpdateDate(20220601L);

        // TSW
        TranscriptStudentCourse tswCourse1 = new TranscriptStudentCourse();
        tswCourse1.setStudNo(pen);
        tswCourse1.setReportType("1");
        tswCourse1.setCourseCode("Generic");
        tswCourse1.setCourseName("Generic Course Name");
        tswCourse1.setCourseLevel("12");
        tswCourse1.setFinalPercentage("91.00");
        tswCourse1.setFinalLG("A");
        tswCourse1.setCourseSession("202206");
        tswCourse1.setNumberOfCredits("4");
        tswCourse1.setUsedForGrad("4");
        tswCourse1.setFoundationReq("10");
        tswCourse1.setUpdateDate(20220601L);

        TranscriptStudentCourse tswCourse2 = new TranscriptStudentCourse();
        tswCourse2.setStudNo(pen);
        tswCourse2.setReportType("2");
        tswCourse2.setCourseCode("TestCourse");
        tswCourse2.setCourseName("Test Course Name");
        tswCourse2.setCourseLevel("12");
        tswCourse2.setFinalPercentage("92.00");
        tswCourse2.setFinalLG("A");
        tswCourse2.setCourseSession("202206");
        tswCourse2.setNumberOfCredits("4");
        tswCourse2.setUsedForGrad("4");
        tswCourse2.setFoundationReq("11");
        tswCourse2.setUpdateDate(20220601L);

        TranscriptStudentCourse tswAssessment = new TranscriptStudentCourse();
        tswAssessment.setStudNo(pen);
        tswAssessment.setReportType("3");
        tswAssessment.setCourseCode("TestAssmt");
        tswAssessment.setCourseName("Test Assessment Name");
        tswAssessment.setCourseLevel("12");
        tswAssessment.setFinalPercentage("XMT");
        tswAssessment.setCourseSession("202206");
        tswAssessment.setFoundationReq("15");
        tswAssessment.setUpdateDate(new Date(System.currentTimeMillis() - 100000L).getTime());

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        // Rule 10
        ProgramRequirement pr10 = new ProgramRequirement();
        pr10.setProgramRequirementID(UUID.randomUUID());
        pr10.setGraduationProgramCode("2018-PF");
        ProgramRequirementCode rule10 = new ProgramRequirementCode();
        rule10.setTraxReqNumber("10");
        rule10.setProReqCode("110");
        rule10.setLabel("Rule 10 Test Label");
        rule10.setDescription("Rule 10 Test Description");

        pr10.setProgramRequirementCode(rule10);

        GradRuleDetails gradRule10Details = new GradRuleDetails();
        gradRule10Details.setRuleCode("110");
        gradRule10Details.setTraxReqNumber("10");
        gradRule10Details.setProgramCode("2018-PF");
        gradRule10Details.setRequirementName("Rule 10 Test Label");

        // Rule 11
        ProgramRequirement pr11 = new ProgramRequirement();
        pr11.setProgramRequirementID(UUID.randomUUID());
        pr11.setGraduationProgramCode("2018-PF");
        ProgramRequirementCode rule11 = new ProgramRequirementCode();
        rule11.setTraxReqNumber("11");
        rule11.setProReqCode("111");
        rule11.setLabel("Rule 11 Test Label");
        rule11.setDescription("Rule 11 Test Description");

        pr11.setProgramRequirementCode(rule11);

        GradRuleDetails gradRule11Details = new GradRuleDetails();
        gradRule11Details.setRuleCode("111");
        gradRule11Details.setTraxReqNumber("11");
        gradRule11Details.setProgramCode("2018-PF");
        gradRule11Details.setRequirementName("Rule 11 Test Label");

        // Rule 15
        ProgramRequirement pr15 = new ProgramRequirement();
        pr15.setProgramRequirementID(UUID.randomUUID());
        pr15.setGraduationProgramCode("2018-PF");
        ProgramRequirementCode rule15 = new ProgramRequirementCode();
        rule15.setTraxReqNumber("15");
        rule15.setProReqCode("115");
        rule15.setLabel("Rule 15 Test Label");
        rule15.setDescription("Rule 15 Test Description");

        pr15.setProgramRequirementCode(rule15);

        GradRuleDetails gradRule15Details = new GradRuleDetails();
        gradRule15Details.setRuleCode("115");
        gradRule15Details.setTraxReqNumber("15");
        gradRule15Details.setProgramCode("2018-PF");
        gradRule15Details.setRequirementName("Rule 15 Test Label");

        // SpecialCase
        SpecialCase sc = new SpecialCase();
        sc.setSpCase("E");
        sc.setLabel("XMT");
        sc.setDescription("Exempt");
        sc.setPassFlag("Y");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.empty());
        when(this.graduationStudentRecordRepository.save(any(GraduationStudentRecordEntity.class))).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.checkSchoolExists(mincode, "123")).thenReturn(true);
        when(this.studentOptionalProgramRepository.findByStudentID(studentID)).thenReturn(Arrays.asList(specialProgramEntity));
        when(this.studentOptionalProgramRepository.save(any(StudentOptionalProgramEntity.class))).thenReturn(specialProgramEntity);
        when(this.studentOptionalProgramRepository.findByStudentIDAndOptionalProgramID(studentID, specialProgram.getOptionalProgramID())).thenReturn(Optional.of(specialProgramEntity));
        when(this.studentCareerProgramRepository.save(studentCareerProgramEntity)).thenReturn(studentCareerProgramEntity);
        when(this.studentCareerProgramRepository.findByStudentID(studentID)).thenReturn(Arrays.asList(studentCareerProgramEntity));
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.studentOptionalProgramHistoryRepository.save(specialProgramHistoryEntity)).thenReturn(specialProgramHistoryEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgramByID(specialProgram.getOptionalProgramID(), "123")).thenReturn(specialProgram);
        when(this.restUtils.getOptionalProgram("2018-PF", "DD", "123")).thenReturn(specialProgram);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);
        when(this.restUtils.getSchoolGrad(mincode, "123")).thenReturn(school);
        when(this.restUtils.getGradProgramRules("2018-PF", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-PF")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .graduated(true)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("I")
                .slpDate("0")
                .englishCert("E")
                .frenchCert("S")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.reportService.prepareTranscriptData(any(), any(), eq(summary.getAccessToken()))).thenReturn(new ReportData());

        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("2018");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertGraduatedStudent_forAdult1950_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("1950");
        gradStudentEntity.setStudentGrade("AD");
        gradStudentEntity.setStudentStatus("CUR");

        GraduationStudentRecordHistoryEntity gradStudentHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(gradStudentEntity, gradStudentHistoryEntity);
//        gradStudentHistoryEntity.setHistoryID(UUID.randomUUID());
        gradStudentHistoryEntity.setActivityCode("DATACONVERT");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setSchoolName("Test School");
        tranStudentDemog.setStudentGrade("AD");
        tranStudentDemog.setGradReqtYear("1950");
        tranStudentDemog.setGradMessage("Test Message. Student has successfully completed the Programme Francophone.");
        tranStudentDemog.setUpdateDate(20220601L);

        // TSW
        TranscriptStudentCourse tswCourse1 = new TranscriptStudentCourse();
        tswCourse1.setStudNo(pen);
        tswCourse1.setReportType("1");
        tswCourse1.setCourseCode("Generic");
        tswCourse1.setCourseName("Generic Course Name");
        tswCourse1.setCourseLevel("12");
        tswCourse1.setFinalPercentage("91.00");
        tswCourse1.setFinalLG("A");
        tswCourse1.setCourseSession("202206");
        tswCourse1.setNumberOfCredits("4");
        tswCourse1.setUsedForGrad("4");
        tswCourse1.setFoundationReq("10");
        tswCourse1.setUpdateDate(20220601L);

        TranscriptStudentCourse tswCourse2 = new TranscriptStudentCourse();
        tswCourse2.setStudNo(pen);
        tswCourse2.setReportType("2");
        tswCourse2.setCourseCode("TestCourse");
        tswCourse2.setCourseName("Test Course Name");
        tswCourse2.setCourseLevel("12");
        tswCourse2.setFinalPercentage("92.00");
        tswCourse2.setFinalLG("A");
        tswCourse2.setCourseSession("202206");
        tswCourse2.setNumberOfCredits("4");
        tswCourse2.setUsedForGrad("4");
        tswCourse2.setFoundationReq("11");
        tswCourse2.setUpdateDate(20220601L);

        TranscriptStudentCourse tswAssessment = new TranscriptStudentCourse();
        tswAssessment.setStudNo(pen);
        tswAssessment.setReportType("3");
        tswAssessment.setCourseCode("TestAssmt");
        tswAssessment.setCourseName("Test Assessment Name");
        tswAssessment.setCourseLevel("12");
        tswAssessment.setFinalPercentage("XMT");
        tswAssessment.setCourseSession("202206");
        tswAssessment.setFoundationReq("15");
        tswAssessment.setUpdateDate(new Date(System.currentTimeMillis() - 100000L).getTime());

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        // Rule 10
        ProgramRequirement pr10 = new ProgramRequirement();
        pr10.setProgramRequirementID(UUID.randomUUID());
        pr10.setGraduationProgramCode("1950");
        ProgramRequirementCode rule10 = new ProgramRequirementCode();
        rule10.setTraxReqNumber("10");
        rule10.setProReqCode("110");
        rule10.setLabel("Rule 10 Test Label");
        rule10.setDescription("Rule 10 Test Description");

        pr10.setProgramRequirementCode(rule10);

        GradRuleDetails gradRule10Details = new GradRuleDetails();
        gradRule10Details.setRuleCode("110");
        gradRule10Details.setTraxReqNumber("10");
        gradRule10Details.setProgramCode("1950");
        gradRule10Details.setRequirementName("Rule 10 Test Label");

        // Rule 11
        ProgramRequirement pr11 = new ProgramRequirement();
        pr11.setProgramRequirementID(UUID.randomUUID());
        pr11.setGraduationProgramCode("1950");
        ProgramRequirementCode rule11 = new ProgramRequirementCode();
        rule11.setTraxReqNumber("11");
        rule11.setProReqCode("111");
        rule11.setLabel("Rule 11 Test Label");
        rule11.setDescription("Rule 11 Test Description");

        pr11.setProgramRequirementCode(rule11);

        GradRuleDetails gradRule11Details = new GradRuleDetails();
        gradRule11Details.setRuleCode("111");
        gradRule11Details.setTraxReqNumber("11");
        gradRule11Details.setProgramCode("1950");
        gradRule11Details.setRequirementName("Rule 11 Test Label");

        // Rule 15
        ProgramRequirement pr15 = new ProgramRequirement();
        pr15.setProgramRequirementID(UUID.randomUUID());
        pr15.setGraduationProgramCode("1950");
        ProgramRequirementCode rule15 = new ProgramRequirementCode();
        rule15.setTraxReqNumber("15");
        rule15.setProReqCode("115");
        rule15.setLabel("Rule 15 Test Label");
        rule15.setDescription("Rule 15 Test Description");

        pr15.setProgramRequirementCode(rule15);

        GradRuleDetails gradRule15Details = new GradRuleDetails();
        gradRule15Details.setRuleCode("115");
        gradRule15Details.setTraxReqNumber("15");
        gradRule15Details.setProgramCode("1950");
        gradRule15Details.setRequirementName("Rule 15 Test Label");

        // SpecialCase
        SpecialCase sc = new SpecialCase();
        sc.setSpCase("E");
        sc.setLabel("XMT");
        sc.setDescription("Exempt");
        sc.setPassFlag("Y");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.of(gradStudentEntity));
        when(this.graduationStudentRecordRepository.save(gradStudentEntity)).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.checkSchoolExists(mincode, "123")).thenReturn(true);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.restUtils.getSchoolGrad(mincode, "123")).thenReturn(school);
        when(this.restUtils.getGradProgramRules("2018-EN", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("1950")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .graduated(true)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("I")
                .slpDate("0")
                .englishCert("E")
                .frenchCert("F")
                .studentStatus("A")
                .studentGrade("AD")
                .schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("1950")
                .programCodes(new ArrayList<>())
                .build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1950");

    }

    @Test
    public void convertGraduatedStudent_forSCCP_whenSlpDate_isWrongFormat_thenReturnFailure() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("SCCP");
        gradStudentEntity.setStudentGrade("11");
        gradStudentEntity.setStudentStatus("CUR");

        GraduationStudentRecordHistoryEntity gradStudentHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(gradStudentEntity, gradStudentHistoryEntity);
//        gradStudentHistoryEntity.setHistoryID(UUID.randomUUID());
        gradStudentHistoryEntity.setActivityCode("DATACONVERT");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setSchoolName("Test School");
        tranStudentDemog.setStudentGrade("11");
        tranStudentDemog.setGradReqtYear("SCCP");
        tranStudentDemog.setGradMessage("Test Message. Student has successfully completed the Programme Francophone.");
        tranStudentDemog.setUpdateDate(20220601L);

        // TSW
        TranscriptStudentCourse tswCourse1 = new TranscriptStudentCourse();
        tswCourse1.setStudNo(pen);
        tswCourse1.setReportType("1");
        tswCourse1.setCourseCode("Generic");
        tswCourse1.setCourseName("Generic Course Name");
        tswCourse1.setCourseLevel("12");
        tswCourse1.setFinalPercentage("91.00");
        tswCourse1.setFinalLG("A");
        tswCourse1.setCourseSession("202206");
        tswCourse1.setNumberOfCredits("4");
        tswCourse1.setUsedForGrad("4");
        tswCourse1.setFoundationReq("10");
        tswCourse1.setUpdateDate(20220601L);

        TranscriptStudentCourse tswCourse2 = new TranscriptStudentCourse();
        tswCourse2.setStudNo(pen);
        tswCourse2.setReportType("2");
        tswCourse2.setCourseCode("TestCourse");
        tswCourse2.setCourseName("Test Course Name");
        tswCourse2.setCourseLevel("12");
        tswCourse2.setFinalPercentage("92.00");
        tswCourse2.setFinalLG("A");
        tswCourse2.setCourseSession("202206");
        tswCourse2.setNumberOfCredits("4");
        tswCourse2.setUsedForGrad("4");
        tswCourse2.setFoundationReq("11");
        tswCourse2.setUpdateDate(20220601L);

        TranscriptStudentCourse tswAssessment = new TranscriptStudentCourse();
        tswAssessment.setStudNo(pen);
        tswAssessment.setReportType("3");
        tswAssessment.setCourseCode("TestAssmt");
        tswAssessment.setCourseName("Test Assessment Name");
        tswAssessment.setCourseLevel("12");
        tswAssessment.setFinalPercentage("XMT");
        tswAssessment.setCourseSession("202206");
        tswAssessment.setFoundationReq("15");
        tswAssessment.setUpdateDate(new Date(System.currentTimeMillis() - 100000L).getTime());

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        // Rule 10
        ProgramRequirement pr10 = new ProgramRequirement();
        pr10.setProgramRequirementID(UUID.randomUUID());
        pr10.setGraduationProgramCode("SCCP");
        ProgramRequirementCode rule10 = new ProgramRequirementCode();
        rule10.setTraxReqNumber("10");
        rule10.setProReqCode("110");
        rule10.setLabel("Rule 10 Test Label");
        rule10.setDescription("Rule 10 Test Description");

        pr10.setProgramRequirementCode(rule10);

        GradRuleDetails gradRule10Details = new GradRuleDetails();
        gradRule10Details.setRuleCode("110");
        gradRule10Details.setTraxReqNumber("10");
        gradRule10Details.setProgramCode("SCCP");
        gradRule10Details.setRequirementName("Rule 10 Test Label");

        // Rule 11
        ProgramRequirement pr11 = new ProgramRequirement();
        pr11.setProgramRequirementID(UUID.randomUUID());
        pr11.setGraduationProgramCode("SCCP");
        ProgramRequirementCode rule11 = new ProgramRequirementCode();
        rule11.setTraxReqNumber("11");
        rule11.setProReqCode("111");
        rule11.setLabel("Rule 11 Test Label");
        rule11.setDescription("Rule 11 Test Description");

        pr11.setProgramRequirementCode(rule11);

        GradRuleDetails gradRule11Details = new GradRuleDetails();
        gradRule11Details.setRuleCode("111");
        gradRule11Details.setTraxReqNumber("11");
        gradRule11Details.setProgramCode("SCCP");
        gradRule11Details.setRequirementName("Rule 11 Test Label");

        // Rule 15
        ProgramRequirement pr15 = new ProgramRequirement();
        pr15.setProgramRequirementID(UUID.randomUUID());
        pr15.setGraduationProgramCode("SCCP");
        ProgramRequirementCode rule15 = new ProgramRequirementCode();
        rule15.setTraxReqNumber("15");
        rule15.setProReqCode("115");
        rule15.setLabel("Rule 15 Test Label");
        rule15.setDescription("Rule 15 Test Description");

        pr15.setProgramRequirementCode(rule15);

        GradRuleDetails gradRule15Details = new GradRuleDetails();
        gradRule15Details.setRuleCode("115");
        gradRule15Details.setTraxReqNumber("15");
        gradRule15Details.setProgramCode("SCCP");
        gradRule15Details.setRequirementName("Rule 15 Test Label");

        // SpecialCase
        SpecialCase sc = new SpecialCase();
        sc.setSpCase("E");
        sc.setLabel("XMT");
        sc.setDescription("Exempt");
        sc.setPassFlag("Y");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.of(gradStudentEntity));
        when(this.graduationStudentRecordRepository.save(gradStudentEntity)).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.checkSchoolExists(mincode, "123")).thenReturn(true);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.restUtils.getSchoolGrad(mincode, "123")).thenReturn(school);
        when(this.restUtils.getGradProgramRules("2018-EN", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("SCCP")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .slpDate("202206")
                .sccDate("20220601")
                .graduated(true)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("I")
                .englishCert("E")
                .frenchCert("F")
                .studentStatus("A")
                .studentGrade("11")
                .schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("SCCP")
                .programCodes(new ArrayList<>())
                .build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("Bad data : slp_date format");
    }

    @Test
    public void testAddStudentCareerProgram() {
        UUID studentID = UUID.randomUUID();
        String careerProgramCode = "XC";

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);

        when(this.studentCareerProgramRepository.findByStudentIDAndCareerProgramCode(studentID, careerProgramCode)).thenReturn(Optional.empty());

        boolean exceptionIsThrown = false;
        try {
            studentService.addStudentCareerProgram(careerProgramCode, requestStudent);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testRemoveStudentCareerProgram() {
        UUID studentID = UUID.randomUUID();
        String careerProgramCode = "XC";

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);

        StudentCareerProgramEntity studentCareerProgram = new StudentCareerProgramEntity();
        studentCareerProgram.setCareerProgramCode(careerProgramCode);
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setId(UUID.randomUUID());

        when(this.studentCareerProgramRepository.findByStudentIDAndCareerProgramCode(studentID, careerProgramCode)).thenReturn(Optional.of(studentCareerProgram));

        boolean exceptionIsThrown = false;
        try {
            studentService.removeStudentCareerProgram(careerProgramCode, requestStudent);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testExistCareerProgram() {
        UUID studentID = UUID.randomUUID();
        String careerProgramCode = "XC";

        StudentCareerProgramEntity studentCareerProgram = new StudentCareerProgramEntity();
        studentCareerProgram.setCareerProgramCode(careerProgramCode);
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setId(UUID.randomUUID());

        when(this.studentCareerProgramRepository.findByStudentID(studentID)).thenReturn(Arrays.asList(studentCareerProgram));

        var result = studentService.existsCareerProgram(studentID);
        assertThat(result).isTrue();
    }

    @Test
    public void testAddStudentOptionalProgram() {
        String program = "2018-EN";

        UUID studentID = UUID.randomUUID();
        String optionalProgramCode = "FI";

        OptionalProgram optionalProgram = new OptionalProgram();
        optionalProgram.setOptionalProgramID(UUID.randomUUID());
        optionalProgram.setOptProgramCode(optionalProgramCode);
        optionalProgram.setOptionalProgramName("French Immersion");
        optionalProgram.setGraduationProgramCode(program);

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);

        when(this.restUtils.getOptionalProgram(eq(program), eq(optionalProgramCode), any())).thenReturn(optionalProgram);
        when(this.studentOptionalProgramRepository.findByStudentIDAndOptionalProgramID(studentID, optionalProgram.getOptionalProgramID())).thenReturn(Optional.empty());

        boolean exceptionIsThrown = false;
        try {
            studentService.addStudentOptionalProgram(optionalProgramCode, requestStudent, "accessToken");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testRemoveStudentOptionalProgram() {
        String program = "2018-EN";

        UUID studentID = UUID.randomUUID();
        String optionalProgramCode = "FI";

        OptionalProgram optionalProgram = new OptionalProgram();
        optionalProgram.setOptionalProgramID(UUID.randomUUID());
        optionalProgram.setOptProgramCode(optionalProgramCode);
        optionalProgram.setOptionalProgramName("French Immersion");
        optionalProgram.setGraduationProgramCode(program);

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);

        StudentOptionalProgramEntity studentOptionalProgramEntity = new StudentOptionalProgramEntity();
        studentOptionalProgramEntity.setOptionalProgramID(optionalProgram.getOptionalProgramID());
        studentOptionalProgramEntity.setStudentID(studentID);
        studentOptionalProgramEntity.setId(UUID.randomUUID());

        when(this.restUtils.getOptionalProgram(eq(program), eq(optionalProgramCode), any())).thenReturn(optionalProgram);
        when(this.studentOptionalProgramRepository.findByStudentIDAndOptionalProgramID(studentID, optionalProgram.getOptionalProgramID())).thenReturn(Optional.of(studentOptionalProgramEntity));

        boolean exceptionIsThrown = false;
        try {
            studentService.removeStudentOptionalProgram(optionalProgramCode, requestStudent, "accessToken");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testSaveGraduationStudent_whenENisChangedToPF_then_returnAPICallSuccess() {
        String program = "2018-EN";

        UUID studentID = UUID.randomUUID();

        GraduationStudentRecordEntity graduationStudentRecordEntity = new GraduationStudentRecordEntity();
        graduationStudentRecordEntity.setStudentID(studentID);
        graduationStudentRecordEntity.setProgram(program);
        graduationStudentRecordEntity.setStudentGrade("11");
        graduationStudentRecordEntity.setStudentStatus("A");
        graduationStudentRecordEntity.setSchoolOfRecord("222336");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.of(graduationStudentRecordEntity));

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);
        requestStudent.setNewProgram("2018-PF");
        requestStudent.setNewStudentGrade("12");
        requestStudent.setNewSchoolOfRecord("333456");
        requestStudent.setNewSchoolAtGrad("333456");
        requestStudent.setAddDualDogwood(true);

        boolean exceptionIsThrown = false;
        try {
            studentService.saveGraduationStudent(requestStudent, "accessToken");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testSaveGraduationStudent_whenPFisChangedToEN_then_returnAPICallSuccess() {
        String program = "2018-PF";

        UUID studentID = UUID.randomUUID();

        GraduationStudentRecordEntity graduationStudentRecordEntity = new GraduationStudentRecordEntity();
        graduationStudentRecordEntity.setStudentID(studentID);
        graduationStudentRecordEntity.setProgram(program);
        graduationStudentRecordEntity.setStudentGrade("11");
        graduationStudentRecordEntity.setStudentStatus("A");
        graduationStudentRecordEntity.setSchoolOfRecord("222336");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.of(graduationStudentRecordEntity));

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);
        requestStudent.setNewProgram("2018-EN");
        requestStudent.setNewStudentGrade("12");
        requestStudent.setNewSchoolOfRecord("333456");
        requestStudent.setNewSchoolAtGrad("333456");
        requestStudent.setDeleteDualDogwood(true);

        boolean exceptionIsThrown = false;
        try {
            studentService.saveGraduationStudent(requestStudent, "accessToken");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testLoadStudentData_whenPENStudentAPIisDown_returnsNull() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        when(this.restUtils.getStudentsByPen(pen, "123")).thenThrow(IllegalArgumentException.class);

        var result = studentService.loadStudentData(pen, "123");
        assertThat(result).isNull();
    }

    @Test
    public void testLoadStudentData_whenPENStudentIsNotFound_returnsNull() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(new ArrayList<>());

        var result = studentService.loadStudentData(pen, "123");
        assertThat(result).isNull();
    }

    @Test
    public void testLoadStudentData_withGivenData_returnsStudentGradDTO_withAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("2018-EN");
        gradStudentEntity.setStudentGrade("12");
        gradStudentEntity.setStudentStatus("CUR");
        gradStudentEntity.setHonoursStanding("Y");
        gradStudentEntity.setSchoolAtGrad(mincode);
        gradStudentEntity.setSchoolOfRecord(mincode);
        gradStudentEntity.setEnglishCert("E");
        gradStudentEntity.setFrenchCert("F");
        gradStudentEntity.setProgramCompletionDate(new Date(System.currentTimeMillis() - 600000L));

        StudentOptionalProgramEntity studentOptionalProgramEntity = new StudentOptionalProgramEntity();
        studentOptionalProgramEntity.setStudentID(studentID);
        studentOptionalProgramEntity.setPen(pen);
        studentOptionalProgramEntity.setOptionalProgramID(UUID.randomUUID());
        studentOptionalProgramEntity.setStudentID(UUID.randomUUID());

        OptionalProgram optionalProgram = new OptionalProgram();
        optionalProgram.setOptionalProgramID(studentOptionalProgramEntity.getOptionalProgramID());
        optionalProgram.setOptProgramCode("FI");
        optionalProgram.setOptionalProgramName("French Immersion");

        StudentCareerProgramEntity studentCareerProgramEntity = new StudentCareerProgramEntity();
        studentCareerProgramEntity.setStudentID(studentID);
        studentCareerProgramEntity.setCareerProgramCode("XC");
        studentCareerProgramEntity.setId(UUID.randomUUID());

        StudentCourse studentCourse = new StudentCourse();
        studentCourse.setPen(pen);
        studentCourse.setCourseCode("TestCourse");
        studentCourse.setCourseLevel("12");
        studentCourse.setCredits(Integer.valueOf("4"));

        StudentAssessment studentAssessment = new StudentAssessment();
        studentAssessment.setPen(pen);
        studentAssessment.setAssessmentCode("TestAssmt");
        studentAssessment.setAssessmentName("Test Assessment");

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.of(gradStudentEntity));
        when(this.restUtils.getOptionalProgramByID(optionalProgram.getOptionalProgramID(), "123")).thenReturn(optionalProgram);
        when(this.studentOptionalProgramRepository.findByStudentID(studentID)).thenReturn(Arrays.asList(studentOptionalProgramEntity));
        when(this.studentCareerProgramRepository.findByStudentID(studentID)).thenReturn(Arrays.asList(studentCareerProgramEntity));
        when(this.courseService.getStudentCourses(pen, "123")).thenReturn(Arrays.asList(studentCourse));
        when(this.assessmentService.getStudentAssessments(pen, "123")).thenReturn(Arrays.asList(studentAssessment));

        var result = studentService.loadStudentData(pen, "123");
        assertThat(result).isNotNull();
        assertThat(result.getStudentID()).isEqualTo(studentID);
        assertThat(result.getProgramCodes()).isNotEmpty();
        assertThat(result.getProgramCodes()).hasSize(2);
        assertThat(result.getCourses()).isNotEmpty();
        assertThat(result.getCourses()).hasSize(1);
        assertThat(result.getAssessments()).isNotEmpty();
        assertThat(result.getAssessments()).hasSize(1);
    }

    @Test
    public void testTriggerGraduationBatchRun_withMergedStatus_returnsNoBatchRun() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("2018-EN");
        gradStudentEntity.setStudentGrade("12");
        gradStudentEntity.setStudentStatus("MER");
        gradStudentEntity.setHonoursStanding("Y");
        gradStudentEntity.setSchoolAtGrad(mincode);
        gradStudentEntity.setSchoolOfRecord(mincode);
        gradStudentEntity.setEnglishCert("E");
        gradStudentEntity.setFrenchCert("F");
        gradStudentEntity.setProgramCompletionDate(new Date(System.currentTimeMillis() - 600000L));

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.of(gradStudentEntity));
        boolean isExceptionThrown = false;
        try {
            studentService.triggerGraduationBatchRun(studentID, "Y", "Y");
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

    @Test
    public void testTriggerGraduationBatchRun_withCurrentStatus_returnsNoBatchRun() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);
        gradStudentEntity.setProgram("2018-EN");
        gradStudentEntity.setStudentGrade("12");
        gradStudentEntity.setStudentStatus("CUR");
        gradStudentEntity.setHonoursStanding("Y");
        gradStudentEntity.setSchoolAtGrad(mincode);
        gradStudentEntity.setSchoolOfRecord(mincode);
        gradStudentEntity.setEnglishCert("E");
        gradStudentEntity.setFrenchCert("F");
        gradStudentEntity.setProgramCompletionDate(new Date(System.currentTimeMillis() - 600000L));

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.of(gradStudentEntity));

        boolean isExceptionThrown = false;
        try {
            studentService.triggerGraduationBatchRun(studentID, "Y", "Y");
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

    @Test
    public void testClearMaps() {
        boolean isExceptionThrown = false;
        try {
            studentService.clearMaps();
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

}
