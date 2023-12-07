package ca.bc.gov.educ.api.dataconversion.process;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import ca.bc.gov.educ.api.dataconversion.constant.StudentLoadType;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.School;
import ca.bc.gov.educ.api.dataconversion.model.tsw.SpecialCase;
import ca.bc.gov.educ.api.dataconversion.model.tsw.report.ReportData;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentBaseService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiUtils;
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
public class StudentProcessTest {

    @Autowired
    StudentProcess studentProcess;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    CourseProcess courseProcess;

    @MockBean
    AssessmentProcess assessmentProcess;

    @MockBean
    ReportProcess reportProcess;

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

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void convertStudent_forSchoolValidation_whenGivenSchool_doesNotExist_thenReturnFailure() throws Exception {
        String pen = "111222333";
        String mincode = "222333";

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("SCCP")
                .archiveFlag("A").studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("SCCP")
                .studentLoadType(StudentLoadType.UNGRAD)
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("Invalid school of record");
    }

    @Test
    public void convertGraduatedStudent_forSchoolValidation_whenGivenSPMSchool_doesNotExist_thenReturnFailure() throws Exception {
        String pen = "111222333";
        String mincode = "222333";

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("SCCP")
                .studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("SCCP")
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("School does not exist in SPM School data");
    }

    @Test
    public void convertStudent_forPenStudentValidation_whenGivenPen_doesNotExist_thenReturnFailure() throws Exception {
        String pen = "111222333";
        String mincode = "222333";

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(new ArrayList<>());

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("SCCP")
                .archiveFlag("A").studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("SCCP")
                .transcriptSchool(school)
                .studentLoadType(StudentLoadType.UNGRAD)
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("PEN does not exist:");
    }

    @Test
    public void convertStudent_forPenStudentValidation_whenPENAPI_isDown_thenReturnFailure() throws Exception {
        String pen = "111222333";
        String mincode = "222333";

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        when(this.restUtils.getStudentsByPen(pen, "123")).thenThrow(new RuntimeException("Test"));

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("SCCP")
                .studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("SCCP")
                .transcriptSchool(school)
                .studentLoadType(StudentLoadType.UNGRAD)
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("PEN Student API is failed:");
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withSccpProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("SCCP");
        gradStudent.setStudentGrade("11");
        gradStudent.setStudentStatus("CUR");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(null);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("SCCP")
                .archiveFlag("A").studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("SCCP")
                .transcriptSchool(school)
                .studentLoadType(StudentLoadType.UNGRAD)
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withAdultProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setDob("2000-06-30");
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1950");
        gradStudent.setStudentGrade("AD");
        gradStudent.setStudentStatus("CUR");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(null);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1950")
                .studentGrade("AD").archiveFlag("A").studentStatus("A")
                .schoolOfRecord(mincode).graduationRequirementYear("1950")
                .transcriptSchool(school)
                .studentLoadType(StudentLoadType.UNGRAD)
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1950");
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(null);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN")
                .archiveFlag("A").studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("2018")
                .transcriptSchool(school).studentLoadType(StudentLoadType.UNGRAD)
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

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
        String mincode = "222333";

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN")
                .archiveFlag("A").studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("2018")
                .transcriptSchool(school).studentLoadType(StudentLoadType.UNGRAD)
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

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
        String mincode = "222333";

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1986-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("MER");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("1986-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourseForEN(pen, "11", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("1986-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1986-EN")
                .studentStatus("M").schoolOfRecord(mincode).graduationRequirementYear("1986").studentLoadType(StudentLoadType.UNGRAD)
                .transcriptSchool(school).programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1986");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertStudent_whenGiven1996Data_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1996-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("1996-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "11", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("1996-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1996-EN")
                .archiveFlag("A").studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("1996").studentLoadType(StudentLoadType.UNGRAD)
                .transcriptSchool(school).programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

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
        String mincode = "222333";

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1986-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("1986-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourseForEN(pen, "11", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("1986-EN", "FI", "123")).thenReturn(specialProgram);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1986-EN")
                .archiveFlag("A").studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("1986").studentLoadType(StudentLoadType.UNGRAD)
                .transcriptSchool(school).programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1986");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertArchivedStudent_whenGiven1986Data_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1986-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("ARC");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("1986-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourseForEN(pen, "11", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("1986-EN", "FI", "123")).thenReturn(specialProgram);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1986-EN")
                .archiveFlag("I").studentStatus("A").schoolOfRecord(mincode).graduationRequirementYear("1986").studentLoadType(StudentLoadType.UNGRAD)
                .transcriptSchool(school).programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1986");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertGraduatedStudent_whenGiven1950Data_withoutProgramCompletionDate_thenReturnFailure() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1950");
        gradStudent.setStudentGrade("AD");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("N");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1950")
                .programCompletionDate(null)
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate("0")
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("AD")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("1950")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .build();

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo(ConversionResultType.FAILURE);
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("Bad data: grad_date is null for");
    }

    @Test
    public void convertGraduatedStudent_whenGiven1950DataFor2Programs_withoutSccDate_thenReturnFailure() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1950");
        gradStudent.setStudentGrade("AD");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("N");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1950")
                .programCompletionDate(null)
                .studentLoadType(StudentLoadType.GRAD_TWO)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate("0")
                .sccDate(null)
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("AD")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("1950")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .build();

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo(ConversionResultType.FAILURE);
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("Bad data for graduated - 2 programs");
    }


    @Test
    public void convertGraduatedStudent_whenGivenSCCPData_withoutSlpDate_thenReturnFailure() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("SCCP");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("N");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("SCCP")
                .programCompletionDate(null)
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate(null)
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("SCCP")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .build();

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo(ConversionResultType.FAILURE);
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("Bad data: slp_date is null for SCCP");
    }

    @Test
    public void convertGraduatedStudent_whenGraduationStudentRecordIsRetrievedAndGradStudentAPIisDown_thenThrowsExceptionWithFailure() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("SCCP");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("N");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenThrow(new RuntimeException("GRAD Student API is down!"));
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("SCCP")
                .programCompletionDate(null)
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate("20200118")
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("SCCP")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .build();

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo(ConversionResultType.FAILURE);
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("Grad Student API is failed for");
    }

    @Test
    public void convertGraduatedStudent_whenGraduationStudentRecordIsCreatedAndGradStudentAPIisDown_thenThrowsExceptionWithFailure() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("SCCP");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("N");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("12");
        tranStudentDemog.setGradReqtYear("SCCP");
        tranStudentDemog.setUpdateDate(20220601L);

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenThrow(new RuntimeException("Grad Student API is down!"));
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("SCCP")
                .programCompletionDate(null)
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate("20200118")
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("SCCP")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .transcriptStudentDemog(tranStudentDemog)
                .build();

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo(ConversionResultType.FAILURE);
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("Grad Student API is failed for");
    }

    @Test
    public void convertGraduatedStudent_whenGraduationStudentRecordIsUpdatedAndGradStudentAPIisDown_thenThrowsExceptionWithFailure() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("SCCP");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("N");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("12");
        tranStudentDemog.setGradReqtYear("SCCP");
        tranStudentDemog.setUpdateDate(20220601L);

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenThrow(new RuntimeException("Grad Student API is down!"));
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("SCCP")
                .programCompletionDate(null)
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate("20200118")
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("SCCP")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .transcriptStudentDemog(tranStudentDemog)
                .build();

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo(ConversionResultType.FAILURE);
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("Grad Student API is failed for");
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

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1986-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setGraduationProgramCode("1986-EN");
        optionalProgram1.setOptProgramCode("FI");
        optionalProgram1.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramID(optionalProgram1.getOptionalProgramID());
        studentOptionalProgram1.setOptionalProgramName(optionalProgram1.getOptionalProgramName());
        studentOptionalProgram1.setPen(pen);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setGraduationProgramCode("1986-EN");
        optionalProgram2.setOptProgramCode("CP");
        optionalProgram2.setOptionalProgramName("Career Program");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramID(optionalProgram2.getOptionalProgramID());
        studentOptionalProgram2.setOptionalProgramName(optionalProgram2.getOptionalProgramName());
        studentOptionalProgram2.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram1.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(optionalProgram1.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
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
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

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

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentOptionalProgram1, studentOptionalProgram2));
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram1);
        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentCareerProgram));
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgramByID(optionalProgram1.getOptionalProgramID(), "123")).thenReturn(optionalProgram1);
        when(this.restUtils.getOptionalProgram("1986-EN", "FI", "123")).thenReturn(optionalProgram1);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);
        when(this.restUtils.getGradProgramRules("1986-EN", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1986-EN")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate("0")
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("1986")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(Arrays.asList("XC")).build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswCourse3, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.reportProcess.prepareTranscriptData(any(), any(), any(), eq(summary.getAccessToken()))).thenReturn(new ReportData());

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1986");
        assertThat(result.getProgram()).isEqualTo(optionalProgram1.getGraduationProgramCode());

    }

    @Test
    public void convertGraduatedStudent_whenGiven2004Data_withInternationalBaccalaureateDiplomaProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2004-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setGraduationProgramCode("2004-EN");
        optionalProgram1.setOptProgramCode("BD");
        optionalProgram1.setOptionalProgramName("International Baccalaureate Diploma Program ");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramID(optionalProgram1.getOptionalProgramID());
        studentOptionalProgram1.setOptionalProgramName(optionalProgram1.getOptionalProgramName());
        studentOptionalProgram1.setOptionalProgramCode("BD");
        studentOptionalProgram1.setPen(pen);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setGraduationProgramCode("2004-EN");
        optionalProgram2.setOptProgramCode("CP");
        optionalProgram2.setOptionalProgramName("Career Program");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramID(optionalProgram2.getOptionalProgramID());
        studentOptionalProgram2.setOptionalProgramName(optionalProgram2.getOptionalProgramName());
        studentOptionalProgram2.setOptionalProgramCode("CP");
        studentOptionalProgram2.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram1.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(optionalProgram1.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("12");
        tranStudentDemog.setGradReqtYear("2004");
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
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

        // Rule 10
        ProgramRequirement pr10 = new ProgramRequirement();
        pr10.setProgramRequirementID(UUID.randomUUID());
        pr10.setGraduationProgramCode("2004-EN");
        ProgramRequirementCode rule10 = new ProgramRequirementCode();
        rule10.setTraxReqNumber("10");
        rule10.setProReqCode("110");
        rule10.setLabel("Rule 10 Test Label");
        rule10.setDescription("Rule 10 Test Description");

        pr10.setProgramRequirementCode(rule10);

        GradRuleDetails gradRule10Details = new GradRuleDetails();
        gradRule10Details.setRuleCode("110");
        gradRule10Details.setTraxReqNumber("10");
        gradRule10Details.setProgramCode("2004-EN");
        gradRule10Details.setRequirementName("Rule 10 Test Label");

        // Rule 11
        ProgramRequirement pr11 = new ProgramRequirement();
        pr11.setProgramRequirementID(UUID.randomUUID());
        pr11.setGraduationProgramCode("2004-EN");
        ProgramRequirementCode rule11 = new ProgramRequirementCode();
        rule11.setTraxReqNumber("11");
        rule11.setProReqCode("111");
        rule11.setLabel("Rule 11 Test Label");
        rule11.setDescription("Rule 11 Test Description");

        pr11.setProgramRequirementCode(rule11);

        GradRuleDetails gradRule11Details = new GradRuleDetails();
        gradRule11Details.setRuleCode("111");
        gradRule11Details.setTraxReqNumber("11");
        gradRule11Details.setProgramCode("2004-EN");
        gradRule11Details.setRequirementName("Rule 11 Test Label");

        // Rule 15
        ProgramRequirement pr15 = new ProgramRequirement();
        pr15.setProgramRequirementID(UUID.randomUUID());
        pr15.setGraduationProgramCode("2004-EN");
        ProgramRequirementCode rule15 = new ProgramRequirementCode();
        rule15.setTraxReqNumber("15");
        rule15.setProReqCode("115");
        rule15.setLabel("Rule 15 Test Label");
        rule15.setDescription("Rule 15 Test Description");

        pr15.setProgramRequirementCode(rule15);

        GradRuleDetails gradRule15Details = new GradRuleDetails();
        gradRule15Details.setRuleCode("115");
        gradRule15Details.setTraxReqNumber("15");
        gradRule15Details.setProgramCode("2004-EN");
        gradRule15Details.setRequirementName("Rule 15 Test Label");

        // SpecialCase
        SpecialCase sc = new SpecialCase();
        sc.setSpCase("E");
        sc.setLabel("XMT");
        sc.setDescription("Exempt");
        sc.setPassFlag("Y");

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentOptionalProgram1, studentOptionalProgram2));
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram1);
        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentCareerProgram));
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgramByID(optionalProgram1.getOptionalProgramID(), "123")).thenReturn(optionalProgram1);
        when(this.restUtils.getOptionalProgram("2004-EN", "FI", "123")).thenReturn(optionalProgram1);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);
        when(this.restUtils.getGradProgramRules("2004-EN", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2004-EN")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate("0")
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("2004")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(Arrays.asList("XC", "BD")).build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswCourse3, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.reportProcess.prepareTranscriptData(any(), any(), any(), eq(summary.getAccessToken()))).thenReturn(new ReportData());

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("2004");
        assertThat(result.getProgram()).isEqualTo(optionalProgram1.getGraduationProgramCode());

    }

    @Test
    public void convertGraduatedStudent_whenGiven2004Data_withInternationalBaccalaureateCertificateProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2004-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setGraduationProgramCode("2004-EN");
        optionalProgram1.setOptProgramCode("BC");
        optionalProgram1.setOptionalProgramName("International Baccalaureate Certificate Program ");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramID(optionalProgram1.getOptionalProgramID());
        studentOptionalProgram1.setOptionalProgramName(optionalProgram1.getOptionalProgramName());
        studentOptionalProgram1.setOptionalProgramCode("BC");
        studentOptionalProgram1.setPen(pen);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setGraduationProgramCode("2004-EN");
        optionalProgram2.setOptProgramCode("CP");
        optionalProgram2.setOptionalProgramName("Career Program");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramID(optionalProgram2.getOptionalProgramID());
        studentOptionalProgram2.setOptionalProgramName(optionalProgram2.getOptionalProgramName());
        studentOptionalProgram2.setOptionalProgramCode("CP");
        studentOptionalProgram2.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram1.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(optionalProgram1.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("12");
        tranStudentDemog.setGradReqtYear("2004");
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
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

        // Rule 10
        ProgramRequirement pr10 = new ProgramRequirement();
        pr10.setProgramRequirementID(UUID.randomUUID());
        pr10.setGraduationProgramCode("2004-EN");
        ProgramRequirementCode rule10 = new ProgramRequirementCode();
        rule10.setTraxReqNumber("10");
        rule10.setProReqCode("110");
        rule10.setLabel("Rule 10 Test Label");
        rule10.setDescription("Rule 10 Test Description");

        pr10.setProgramRequirementCode(rule10);

        GradRuleDetails gradRule10Details = new GradRuleDetails();
        gradRule10Details.setRuleCode("110");
        gradRule10Details.setTraxReqNumber("10");
        gradRule10Details.setProgramCode("2004-EN");
        gradRule10Details.setRequirementName("Rule 10 Test Label");

        // Rule 11
        ProgramRequirement pr11 = new ProgramRequirement();
        pr11.setProgramRequirementID(UUID.randomUUID());
        pr11.setGraduationProgramCode("2004-EN");
        ProgramRequirementCode rule11 = new ProgramRequirementCode();
        rule11.setTraxReqNumber("11");
        rule11.setProReqCode("111");
        rule11.setLabel("Rule 11 Test Label");
        rule11.setDescription("Rule 11 Test Description");

        pr11.setProgramRequirementCode(rule11);

        GradRuleDetails gradRule11Details = new GradRuleDetails();
        gradRule11Details.setRuleCode("111");
        gradRule11Details.setTraxReqNumber("11");
        gradRule11Details.setProgramCode("2004-EN");
        gradRule11Details.setRequirementName("Rule 11 Test Label");

        // Rule 15
        ProgramRequirement pr15 = new ProgramRequirement();
        pr15.setProgramRequirementID(UUID.randomUUID());
        pr15.setGraduationProgramCode("2004-EN");
        ProgramRequirementCode rule15 = new ProgramRequirementCode();
        rule15.setTraxReqNumber("15");
        rule15.setProReqCode("115");
        rule15.setLabel("Rule 15 Test Label");
        rule15.setDescription("Rule 15 Test Description");

        pr15.setProgramRequirementCode(rule15);

        GradRuleDetails gradRule15Details = new GradRuleDetails();
        gradRule15Details.setRuleCode("115");
        gradRule15Details.setTraxReqNumber("15");
        gradRule15Details.setProgramCode("2004-EN");
        gradRule15Details.setRequirementName("Rule 15 Test Label");

        // SpecialCase
        SpecialCase sc = new SpecialCase();
        sc.setSpCase("E");
        sc.setLabel("XMT");
        sc.setDescription("Exempt");
        sc.setPassFlag("Y");

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentOptionalProgram1, studentOptionalProgram2));
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram1);
        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentCareerProgram));
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgramByID(optionalProgram1.getOptionalProgramID(), "123")).thenReturn(optionalProgram1);
        when(this.restUtils.getOptionalProgram("2004-EN", "FI", "123")).thenReturn(optionalProgram1);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);
        when(this.restUtils.getGradProgramRules("2004-EN", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2004-EN")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate("0")
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("2004")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(Arrays.asList("XC", "BC")).build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswCourse3, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.reportProcess.prepareTranscriptData(any(), any(), any(), eq(summary.getAccessToken()))).thenReturn(new ReportData());

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("2004");
        assertThat(result.getProgram()).isEqualTo(optionalProgram1.getGraduationProgramCode());

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

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setGraduationProgramCode("2018-EN");
        optionalProgram1.setOptProgramCode("FI");
        optionalProgram1.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramID(optionalProgram1.getOptionalProgramID());
        studentOptionalProgram1.setOptionalProgramName(optionalProgram1.getOptionalProgramName());
        studentOptionalProgram1.setOptionalProgramCode("FI");
        studentOptionalProgram1.setPen(pen);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setGraduationProgramCode("2018-EN");
        optionalProgram2.setOptProgramCode("CP");
        optionalProgram2.setOptionalProgramName("Career Program");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramID(optionalProgram2.getOptionalProgramID());
        studentOptionalProgram2.setOptionalProgramName(optionalProgram2.getOptionalProgramName());
        studentOptionalProgram2.setOptionalProgramCode("CP");
        studentOptionalProgram2.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram1.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(optionalProgram1.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("12");
        tranStudentDemog.setGradReqtYear("2018");
        tranStudentDemog.setUpdateDate(20220601L);
        tranStudentDemog.setGradMessage(StudentBaseService.TSW_FI_GRAD_MSG);

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
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

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

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentOptionalProgram1, studentOptionalProgram2));
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram1);
        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentCareerProgram));
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgramByID(optionalProgram1.getOptionalProgramID(), "123")).thenReturn(optionalProgram1);
        when(this.restUtils.getOptionalProgram("2018-EN", "FI", "123")).thenReturn(optionalProgram1);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);
        when(this.restUtils.getGradProgramRules("2018-EN", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate("0")
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("2018")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(Arrays.asList("XC")).build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.reportProcess.prepareTranscriptData(any(), any(), any(), eq(summary.getAccessToken()))).thenReturn(new ReportData());

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("2018");
        assertThat(result.getProgram()).isEqualTo(optionalProgram1.getGraduationProgramCode());

    }

    @Test
    public void convertGraduatedStudent_whenGiven1996Data_withAdvancedPlacementProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1996-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setGraduationProgramCode("1996-EN");
        optionalProgram1.setOptProgramCode("AD");
        optionalProgram1.setOptionalProgramName("Advanced Placement");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramID(optionalProgram1.getOptionalProgramID());
        studentOptionalProgram1.setOptionalProgramName(optionalProgram1.getOptionalProgramName());
        studentOptionalProgram1.setOptionalProgramCode("AD");
        studentOptionalProgram1.setPen(pen);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setGraduationProgramCode("1996-EN");
        optionalProgram2.setOptProgramCode("CP");
        optionalProgram2.setOptionalProgramName("Career Program");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramID(optionalProgram2.getOptionalProgramID());
        studentOptionalProgram2.setOptionalProgramName(optionalProgram2.getOptionalProgramName());
        studentOptionalProgram2.setOptionalProgramCode("CP");
        studentOptionalProgram2.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram1.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(optionalProgram1.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("12");
        tranStudentDemog.setGradReqtYear("1996");
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
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

        // Rule 10
        ProgramRequirement pr10 = new ProgramRequirement();
        pr10.setProgramRequirementID(UUID.randomUUID());
        pr10.setGraduationProgramCode("1996-EN");
        ProgramRequirementCode rule10 = new ProgramRequirementCode();
        rule10.setTraxReqNumber("10");
        rule10.setProReqCode("110");
        rule10.setLabel("Rule 10 Test Label");
        rule10.setDescription("Rule 10 Test Description");

        pr10.setProgramRequirementCode(rule10);

        GradRuleDetails gradRule10Details = new GradRuleDetails();
        gradRule10Details.setRuleCode("110");
        gradRule10Details.setTraxReqNumber("10");
        gradRule10Details.setProgramCode("1996-EN");
        gradRule10Details.setRequirementName("Rule 10 Test Label");

        // Rule 11
        ProgramRequirement pr11 = new ProgramRequirement();
        pr11.setProgramRequirementID(UUID.randomUUID());
        pr11.setGraduationProgramCode("1996-EN");
        ProgramRequirementCode rule11 = new ProgramRequirementCode();
        rule11.setTraxReqNumber("11");
        rule11.setProReqCode("111");
        rule11.setLabel("Rule 11 Test Label");
        rule11.setDescription("Rule 11 Test Description");

        pr11.setProgramRequirementCode(rule11);

        GradRuleDetails gradRule11Details = new GradRuleDetails();
        gradRule11Details.setRuleCode("111");
        gradRule11Details.setTraxReqNumber("11");
        gradRule11Details.setProgramCode("1996-EN");
        gradRule11Details.setRequirementName("Rule 11 Test Label");

        // Rule 15
        ProgramRequirement pr15 = new ProgramRequirement();
        pr15.setProgramRequirementID(UUID.randomUUID());
        pr15.setGraduationProgramCode("1996-EN");
        ProgramRequirementCode rule15 = new ProgramRequirementCode();
        rule15.setTraxReqNumber("15");
        rule15.setProReqCode("115");
        rule15.setLabel("Rule 15 Test Label");
        rule15.setDescription("Rule 15 Test Description");

        pr15.setProgramRequirementCode(rule15);

        GradRuleDetails gradRule15Details = new GradRuleDetails();
        gradRule15Details.setRuleCode("115");
        gradRule15Details.setTraxReqNumber("15");
        gradRule15Details.setProgramCode("1996-EN");
        gradRule15Details.setRequirementName("Rule 15 Test Label");

        // SpecialCase
        SpecialCase sc = new SpecialCase();
        sc.setSpCase("E");
        sc.setLabel("XMT");
        sc.setDescription("Exempt");
        sc.setPassFlag("Y");

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentOptionalProgram1, studentOptionalProgram2));
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram1);
        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentCareerProgram));
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgramByID(optionalProgram1.getOptionalProgramID(), "123")).thenReturn(optionalProgram1);
        when(this.restUtils.getOptionalProgram("1996-EN", "FI", "123")).thenReturn(optionalProgram1);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);
        when(this.restUtils.getGradProgramRules("1996-EN", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1996-EN")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate(null)
                .englishCert("E")
                .frenchCert("F")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("1996")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(Arrays.asList("XC","AD")).build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.reportProcess.prepareTranscriptData(any(), any(), any(), eq(summary.getAccessToken()))).thenReturn(new ReportData());

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1996");
        assertThat(result.getProgram()).isEqualTo(optionalProgram1.getGraduationProgramCode());

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

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-PF");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));
        gradStudent.setStudentCitizenship("C");

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setGraduationProgramCode("2018-PF");
        optionalProgram1.setOptProgramCode("DD");
        optionalProgram1.setOptionalProgramName("Dual Dogwood");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramID(optionalProgram1.getOptionalProgramID());
        studentOptionalProgram1.setOptionalProgramName(optionalProgram1.getOptionalProgramName());
        studentOptionalProgram1.setOptionalProgramCode("DD");
        studentOptionalProgram1.setPen(pen);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setGraduationProgramCode("2018-PF");
        optionalProgram2.setOptProgramCode("CP");
        optionalProgram2.setOptionalProgramName("Career Program");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramID(optionalProgram2.getOptionalProgramID());
        studentOptionalProgram2.setOptionalProgramName(optionalProgram2.getOptionalProgramName());
        studentOptionalProgram2.setOptionalProgramCode("CP");
        studentOptionalProgram2.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram1.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(optionalProgram1.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("12");
        tranStudentDemog.setGradReqtYear("2018");
        tranStudentDemog.setUpdateDate(20220601L);
        tranStudentDemog.setGradMessage(StudentBaseService.TSW_PF_GRAD_MSG);

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
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

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

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.restUtils.getCareerProgram("XC", "123")).thenReturn(careerProgram);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentOptionalProgram1, studentOptionalProgram2));
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram1);
        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentCareerProgram));
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgramByID(optionalProgram1.getOptionalProgramID(), "123")).thenReturn(optionalProgram1);
        when(this.restUtils.getOptionalProgram("2018-PF", "DD", "123")).thenReturn(optionalProgram1);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);
        when(this.restUtils.getGradProgramRules("2018-PF", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-PF")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate(null)
                .englishCert("E")
                .frenchCert("S")
                .studentGrade("12")
                .studentStatus("A").schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("2018")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(Arrays.asList("XC")).build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.reportProcess.prepareTranscriptData(any(), any(), any(), eq(summary.getAccessToken()))).thenReturn(new ReportData());

        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("2018");
        assertThat(result.getProgram()).isEqualTo(optionalProgram1.getGraduationProgramCode());

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
        penStudent.setDob("2000-06-30");

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1950");
        gradStudent.setStudentGrade("AD");
        gradStudent.setStudentStatus("CUR");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("AD");
        tranStudentDemog.setGradReqtYear("1950");
        tranStudentDemog.setGradMessage(StudentBaseService.TSW_PF_GRAD_MSG);
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
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

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

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradProgramRules("1950", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("1950")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate(null)
                .englishCert("E")
                .frenchCert("F")
                .studentStatus("A")
                .studentGrade("AD")
                .schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("1950")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(new ArrayList<>())
                .build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1950");

    }

    @Test
    public void convertGraduatedStudent_forAdult1950_whenAssessmentIsLTE10AndProficiencyScoreIsGreaterThanZero_thenReturnSuccess_withRMIndicator() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);
        penStudent.setDob("2000-06-30");

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1950");
        gradStudent.setStudentGrade("AD");
        gradStudent.setStudentStatus("CUR");

        StudentAssessment studentAssessment = new StudentAssessment();
        studentAssessment.setPen(pen);
        studentAssessment.setAssessmentCode("LTE10");
        studentAssessment.setAssessmentName("Literacy English 10");
        studentAssessment.setSessionDate("202206");
        studentAssessment.setProficiencyScore(Double.valueOf("2.0"));

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("AD");
        tranStudentDemog.setGradReqtYear("1950");
        tranStudentDemog.setGradMessage(StudentBaseService.TSW_PF_GRAD_MSG);
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

        TranscriptStudentCourse tswAssessment1 = new TranscriptStudentCourse();
        tswAssessment1.setStudNo(pen);
        tswAssessment1.setReportType("3");
        tswAssessment1.setCourseCode("TestAssmt");
        tswAssessment1.setCourseName("Test Assessment Name");
        tswAssessment1.setCourseLevel("12");
        tswAssessment1.setFinalPercentage("XMT");
        tswAssessment1.setCourseSession("202206");
        tswAssessment1.setFoundationReq("15");
        tswAssessment1.setUpdateDate(new Date(System.currentTimeMillis() - 100000L).getTime());

        TranscriptStudentCourse tswAssessment2 = new TranscriptStudentCourse();
        tswAssessment2.setStudNo(pen);
        tswAssessment2.setReportType("3");
        tswAssessment2.setCourseCode("LTE10");
        tswAssessment2.setCourseName("Literacy English 10");
        tswAssessment2.setCourseLevel("10");
        tswAssessment2.setFinalPercentage("RM");
        tswAssessment2.setCourseSession("202206");
        tswAssessment2.setFoundationReq("15");
        tswAssessment2.setUpdateDate(new Date(System.currentTimeMillis() - 100000L).getTime());

        School school = new School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

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

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradProgramRules("1950", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));
        when(this.restUtils.getStudentAssessmentsByPenAndAssessmentCode(pen, "LTE10", "123")).thenReturn(Arrays.asList(studentAssessment));

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("1950")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate(null)
                .englishCert("E")
                .frenchCert("F")
                .studentStatus("A")
                .studentGrade("AD")
                .schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("1950")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(new ArrayList<>())
                .build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment1, tswAssessment2));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1950");

    }

    @Test
    public void convertGraduatedStudent_for1950NotInAD_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);
        penStudent.setDob("2000-06-30");

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1950");
        gradStudent.setStudentGrade("AN");
        gradStudent.setStudentStatus("CUR");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("AN");
        tranStudentDemog.setGradReqtYear("1950");
        tranStudentDemog.setGradMessage(StudentBaseService.TSW_PF_GRAD_MSG);
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
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

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

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradProgramRules("1950", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("1950")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate(null)
                .englishCert("E")
                .frenchCert("F")
                .studentStatus("A")
                .studentGrade("AN")
                .schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("1950")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(new ArrayList<>())
                .build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

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

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("SCCP");
        gradStudent.setStudentGrade("11");
        gradStudent.setStudentStatus("CUR");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("11");
        tranStudentDemog.setGradReqtYear("SCCP");
        tranStudentDemog.setGradMessage(StudentBaseService.TSW_PF_GRAD_MSG);
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
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

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

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradProgramRules("2018-EN", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("SCCP")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .slpDate("202206")
                .sccDate("20220601")
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .englishCert("E")
                .frenchCert("F")
                .studentStatus("A")
                .studentGrade("11")
                .schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("SCCP")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(new ArrayList<>())
                .build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("Bad data : slp_date format");
    }

    @Test
    public void convertGraduatedStudent_forSCCP_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("SCCP");
        gradStudent.setStudentGrade("11");
        gradStudent.setStudentStatus("CUR");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("11");
        tranStudentDemog.setGradReqtYear("SCCP");
        tranStudentDemog.setGradMessage(StudentBaseService.TSW_PF_GRAD_MSG);
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
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

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

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradProgramRules("2018-EN", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("SCCP")
                .programCompletionDate(new Date(System.currentTimeMillis() - 100000L))
                .slpDate("20220601")
                .sccDate("20220601")
                .studentLoadType(StudentLoadType.GRAD_ONE)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .englishCert("E")
                .frenchCert("F")
                .studentStatus("A")
                .studentGrade("11")
                .schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("SCCP")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(new ArrayList<>())
                .build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
        assertThat(summary.getErrors()).isEmpty();
    }

    @Test
    public void convertStudentFor2Programs_withAdult1950AndSCCP_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);
        penStudent.setDob("2000-06-30");

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1950");
        gradStudent.setStudentGrade("AD");
        gradStudent.setStudentStatus("CUR");

        // TSW
        TranscriptStudentDemog tranStudentDemog = new TranscriptStudentDemog();
        tranStudentDemog.setStudNo(pen);
        tranStudentDemog.setMincode(mincode);
        tranStudentDemog.setStudentGrade("AD");
        tranStudentDemog.setGradReqtYear("1950");
        tranStudentDemog.setGradMessage(StudentBaseService.TSW_PF_GRAD_MSG);
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
        school.setTranscriptEligibility("Y");
        school.setCertificateEligibility("Y");

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

        ResponseObj responseObj = new ResponseObj();
        responseObj.setAccess_token("123");
        responseObj.setRefresh_token("123");

        when(this.restUtils.getTokenResponseObject()).thenReturn(responseObj);
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradProgramRules("1950", "123")).thenReturn(Arrays.asList(pr10, pr11, pr15));
        when(this.restUtils.getAllSpecialCases("123")).thenReturn(Arrays.asList(sc));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule10Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule10Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule11Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule11Details));
        when(this.restUtils.getGradProgramRulesByTraxReqNumber(gradRule15Details.getTraxReqNumber(), "123")).thenReturn(Arrays.asList(gradRule15Details));

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("1950")
                .programCompletionDate(null)
                .studentLoadType(StudentLoadType.GRAD_TWO)
                .gpa("3.5")
                .honoursStanding("Y")
                .archiveFlag("A")
                .slpDate(null)
                .sccDate("20230308")
                .englishCert("E")
                .frenchCert("F")
                .studentStatus("A")
                .studentGrade("AD")
                .schoolOfRecord(mincode).schoolAtGrad(mincode)
                .graduationRequirementYear("1950")
                .transcriptSchool(school)
                .certificateSchool(school)
                .transcriptSchoolCategoryCode("02")
                .certificateSchoolCategoryCode("02")
                .programCodes(new ArrayList<>())
                .build();
        student.setTranscriptStudentDemog(tranStudentDemog);
        student.setTranscriptStudentCourses(Arrays.asList(tswCourse1, tswCourse2, tswAssessment));

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1950");

    }

    @Test
    public void testAddStudentCareerProgram() {
        UUID studentID = UUID.randomUUID();
        String careerProgramCode = "XC";

        boolean exceptionIsThrown = false;
        try {
            studentProcess.addStudentCareerProgram(careerProgramCode, studentID, "123");
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

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setCareerProgramCode(careerProgramCode);
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setId(UUID.randomUUID());

        boolean exceptionIsThrown = false;
        try {
            studentProcess.removeStudentCareerProgram(careerProgramCode, requestStudent, "123");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testExistCareerProgram() {
        UUID studentID = UUID.randomUUID();
        String careerProgramCode = "XC";

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setCareerProgramCode(careerProgramCode);
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setId(UUID.randomUUID());

        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentCareerProgram));

        var result = studentProcess.existsCareerProgram(studentID, "123");
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

        boolean exceptionIsThrown = false;
        try {
            studentProcess.addStudentOptionalProgram(optionalProgramCode, requestStudent, true, "123");
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

        when(this.restUtils.getOptionalProgram(eq(program), eq(optionalProgramCode), any())).thenReturn(optionalProgram);

        boolean exceptionIsThrown = false;
        try {
            studentProcess.removeStudentOptionalProgram(optionalProgramCode, requestStudent, "123");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testSaveGraduationStudent_whenENisChangedToPF_then_returnAPICallSuccess() {
        String program = "2018-EN";
        String pen = "111222333";
        UUID studentID = UUID.randomUUID();

        GraduationStudentRecord graduationStudentRecord = new GraduationStudentRecord();
        graduationStudentRecord.setStudentID(studentID);
        graduationStudentRecord.setProgram(program);
        graduationStudentRecord.setStudentGrade("11");
        graduationStudentRecord.setStudentStatus("CUR");
        graduationStudentRecord.setSchoolOfRecord("222336");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setOptionalProgramID(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramCode("FI");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setOptionalProgramID(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramCode("BD");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(graduationStudentRecord);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(List.of(studentOptionalProgram1, studentOptionalProgram2));

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);
        requestStudent.setNewProgram("2018-PF");
        requestStudent.setNewStudentGrade("12");
        requestStudent.setNewSchoolOfRecord("333456");
        requestStudent.setNewStudentStatus("ARC");
        requestStudent.setNewRecalculateGradStatus("Y");
        requestStudent.setNewRecalculateProjectedGrad("Y");
        requestStudent.setAddDualDogwood(true);

        boolean exceptionIsThrown = false;
        try {
            studentProcess.saveGraduationStudent(pen, requestStudent, EventType.UPD_GRAD, "123");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testSaveGraduationStudent_whenPFisChangedToEN_then_returnAPICallSuccess() {
        String program = "2018-PF";
        String pen = "111222333";
        UUID studentID = UUID.randomUUID();

        GraduationStudentRecord graduationStudentRecord = new GraduationStudentRecord();
        graduationStudentRecord.setStudentID(studentID);
        graduationStudentRecord.setProgram(program);
        graduationStudentRecord.setStudentGrade("11");
        graduationStudentRecord.setStudentStatus("CUR");
        graduationStudentRecord.setSchoolOfRecord("222336");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setOptionalProgramID(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramCode("DD");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setOptionalProgramID(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramCode("BD");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(graduationStudentRecord);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(List.of(studentOptionalProgram1, studentOptionalProgram2));

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);
        requestStudent.setNewProgram("2018-EN");
        requestStudent.setNewStudentGrade("12");
        requestStudent.setNewSchoolOfRecord("333456");
        requestStudent.setNewStudentStatus("ARC");
        requestStudent.setNewRecalculateGradStatus("Y");
        requestStudent.setNewRecalculateProjectedGrad("Y");
        requestStudent.setAddFrenchImmersion(true);

        boolean exceptionIsThrown = false;
        try {
            studentProcess.saveGraduationStudent(pen, requestStudent, EventType.UPD_GRAD, "123");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testSaveGraduationStudent_whenENisChangedTo1950Adult_then_returnAPICallSuccess() {
        String program = "2018-EN";
        String pen = "111222333";
        UUID studentID = UUID.randomUUID();

        GraduationStudentRecord graduationStudentRecord = new GraduationStudentRecord();
        graduationStudentRecord.setStudentID(studentID);
        graduationStudentRecord.setProgram(program);
        graduationStudentRecord.setStudentGrade("11");
        graduationStudentRecord.setStudentStatus("CUR");
        graduationStudentRecord.setSchoolOfRecord("222336");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(graduationStudentRecord);

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);
        requestStudent.setNewProgram("1950");
        requestStudent.setNewStudentGrade("AD");
        requestStudent.setNewSchoolOfRecord("333456");
        requestStudent.setNewStudentStatus("ARC");
        requestStudent.setNewRecalculateGradStatus("Y");
        requestStudent.setNewRecalculateProjectedGrad("Y");
        requestStudent.setNewAdultStartDate("2010-01-01");
        requestStudent.setNewCitizenship("C");
        requestStudent.setNewGradDate("2022/06/01");

        boolean exceptionIsThrown = false;
        try {
            studentProcess.saveGraduationStudent(pen, requestStudent, EventType.UPD_GRAD, "123");
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

        var result = studentProcess.loadStudentData(pen, "123");
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

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNull();
    }

    @Test
    public void testLoadStudentData_whenGraduationStudentIsNotFound_returnsNull() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNull();
    }

    @Test
    public void testLoadStudentData_whenGradStudentAPIisDown_returnsNull() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenThrow(new RuntimeException("GRAD Student API is down!"));

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNull();
    }

    @Test
    public void testLoadStudentData_whenGradStudentAPIisDownForStudentOptionalPrograms_returnsSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenThrow(new RuntimeException("GRAD Student API is donw!"));

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNotNull();
    }

    @Test
    public void testLoadStudentData_whenGradStudentAPIisDownForStudentCareerPrograms_returnsSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setGraduationProgramCode("2018-EN");
        optionalProgram1.setOptProgramCode("FI");
        optionalProgram1.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramID(optionalProgram1.getOptionalProgramID());
        studentOptionalProgram1.setOptionalProgramName(optionalProgram1.getOptionalProgramName());
        studentOptionalProgram1.setOptionalProgramCode("FI");
        studentOptionalProgram1.setPen(pen);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setGraduationProgramCode("2018-EN");
        optionalProgram2.setOptProgramCode("CP");
        optionalProgram2.setOptionalProgramName("Career Program");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramID(optionalProgram2.getOptionalProgramID());
        studentOptionalProgram2.setOptionalProgramName(optionalProgram2.getOptionalProgramName());
        studentOptionalProgram2.setOptionalProgramCode("CP");
        studentOptionalProgram2.setPen(pen);

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentOptionalProgram1, studentOptionalProgram2));
        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenThrow(new RuntimeException("GRAD Student API is donw!"));

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNotNull();
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

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setGraduationProgramCode("2018-EN");
        optionalProgram1.setOptProgramCode("FI");
        optionalProgram1.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramID(optionalProgram1.getOptionalProgramID());
        studentOptionalProgram1.setOptionalProgramName(optionalProgram1.getOptionalProgramName());
        studentOptionalProgram1.setOptionalProgramCode("FI");
        studentOptionalProgram1.setPen(pen);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setGraduationProgramCode("2018-EN");
        optionalProgram2.setOptProgramCode("CP");
        optionalProgram2.setOptionalProgramName("Career Program");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramID(optionalProgram2.getOptionalProgramID());
        studentOptionalProgram2.setOptionalProgramName(optionalProgram2.getOptionalProgramName());
        studentOptionalProgram2.setOptionalProgramCode("CP");
        studentOptionalProgram2.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

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
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);

        when(this.restUtils.getOptionalProgramByID(optionalProgram1.getOptionalProgramID(), "123")).thenReturn(optionalProgram1);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentOptionalProgram1, studentOptionalProgram2));
        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentCareerProgram));
        when(this.courseProcess.getStudentCourses(pen, "123")).thenReturn(Arrays.asList(studentCourse));
        when(this.assessmentProcess.getStudentAssessments(pen, "123")).thenReturn(Arrays.asList(studentAssessment));

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNotNull();
        assertThat(result.getStudentID()).isEqualTo(studentID);
        assertThat(result.getProgramCodes()).isNotEmpty();
        assertThat(result.getProgramCodes()).hasSize(3);
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

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("MER");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        boolean isExceptionThrown = false;
        try {
            studentProcess.triggerGraduationBatchRun(EventType.XPROGRAM, studentID, pen, "Y", "Y", "123");
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

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolAtGrad(mincode);
        gradStudent.setSchoolOfRecord(mincode);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);

        boolean isExceptionThrown = false;
        try {
            studentProcess.triggerGraduationBatchRun(EventType.XPROGRAM, studentID, pen, "Y", "Y", "123");
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

    @Test
    public void testClearMaps() {
        boolean isExceptionThrown = false;
        try {
            studentProcess.clearMaps();
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

}
