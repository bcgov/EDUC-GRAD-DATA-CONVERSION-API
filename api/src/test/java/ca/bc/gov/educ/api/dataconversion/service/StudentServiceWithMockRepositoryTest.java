package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.student.*;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.School;
import ca.bc.gov.educ.api.dataconversion.model.tsw.SpecialCase;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.ReportData;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import ca.bc.gov.educ.api.dataconversion.repository.student.*;
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

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getRecalculateGradStatus()).isEqualTo("Y");
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

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getRecalculateGradStatus()).isEqualTo("Y");
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

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1996-EN").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("1996")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getRecalculateGradStatus()).isEqualTo("Y");
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

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1986-EN").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("1986")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getRecalculateGradStatus()).isEqualTo("Y");
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
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.studentOptionalProgramHistoryRepository.save(specialProgramHistoryEntity)).thenReturn(specialProgramHistoryEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgramByID(specialProgram.getOptionalProgramID(), "123")).thenReturn(specialProgram);
        when(this.restUtils.getOptionalProgram("1986-EN", "FI", "123")).thenReturn(specialProgram);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);
        when(this.restUtils.getTranscriptStudentDemog(pen, "123")).thenReturn(tranStudentDemog);
        when(this.restUtils.getSchoolGrad(mincode, "123")).thenReturn(school);
        when(this.restUtils.getTranscriptStudentCourses(pen, "123")).thenReturn(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));
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
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequestYear("1986")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

//        // Graduation Data
//        GraduationData graduationData = studentService.buildGraduationData(student, gradStudentEntity, penStudent, summary);
//        // Report Data
//        ca.bc.gov.educ.api.dataconversion.model.tsw.report.GraduationData gradDataForReport = new ca.bc.gov.educ.api.dataconversion.model.tsw.report.GraduationData();
//        BeanUtils.copyProperties(graduationData, gradDataForReport);
//
//        ReportData reportData = new ReportData();
//        reportData.setGraduationData(gradDataForReport);
//        // Transcript
//        Transcript transcript = new Transcript();
//        reportData.setTranscript(transcript);

        when(this.reportService.prepareTranscriptData(any(), any(), eq(summary.getAccessToken()))).thenReturn(new ReportData());
//        doNothing().when(this.reportService).saveStudentTranscriptReportJasper(any(), eq(summary.getAccessToken()), studentID, true);

        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getRecalculateGradStatus()).isNull();
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
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.studentOptionalProgramHistoryRepository.save(specialProgramHistoryEntity)).thenReturn(specialProgramHistoryEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgramByID(specialProgram.getOptionalProgramID(), "123")).thenReturn(specialProgram);
        when(this.restUtils.getOptionalProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);
        when(this.restUtils.getTranscriptStudentDemog(pen, "123")).thenReturn(tranStudentDemog);
        when(this.restUtils.getSchoolGrad(mincode, "123")).thenReturn(school);
        when(this.restUtils.getTranscriptStudentCourses(pen, "123")).thenReturn(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));
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
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

//        // Graduation Data
//        GraduationData graduationData = studentService.buildGraduationData(student, gradStudentEntity, penStudent, summary);
//        // Report Data
//        ca.bc.gov.educ.api.dataconversion.model.tsw.report.GraduationData gradDataForReport = new ca.bc.gov.educ.api.dataconversion.model.tsw.report.GraduationData();
//        BeanUtils.copyProperties(graduationData, gradDataForReport);
//
//        ReportData reportData = new ReportData();
//        reportData.setGraduationData(gradDataForReport);
//        // Transcript
//        Transcript transcript = new Transcript();
//        reportData.setTranscript(transcript);

        when(this.reportService.prepareTranscriptData(any(), any(), eq(summary.getAccessToken()))).thenReturn(new ReportData());
//        doNothing().when(this.reportService).saveStudentTranscriptReportJasper(any(), eq(summary.getAccessToken()), studentID, true);

        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getRecalculateGradStatus()).isNull();
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

}
