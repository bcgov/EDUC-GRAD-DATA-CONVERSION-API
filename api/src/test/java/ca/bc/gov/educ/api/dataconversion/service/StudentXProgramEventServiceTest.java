package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.constant.EventStatus;
import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.process.StudentProcess;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentXProgramEventService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StudentXProgramEventServiceTest {

    @Autowired
    StudentXProgramEventService studentXProgramEventService;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    StudentProcess studentProcess;

    @MockBean
    RestUtils restUtils;

    // NATS
    @MockBean
    private NatsConnection natsConnection;
    @MockBean
    private Subscriber subscriber;

    @Autowired
    private EducGradDataConversionApiConstants constants;

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_XPROGRAM_whenCourseIsSame_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "XPROGRAM";

        // TraxXProgramDTO
        TraxXProgramDTO traxXProgramUpdate = new TraxXProgramDTO();
        traxXProgramUpdate.setPen(pen);
        traxXProgramUpdate.setProgramList(Arrays.asList("XC", "FI"));

        // Event
        Event event = new Event();
        event.setEventType(EventType.XPROGRAM.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("FI");
        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentProcess.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC","FI")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentXProgramEventService.processEvent(traxXProgramUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_XPROGRAM_whenNewCareerProgramIsRemoved_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "XPROGRAM";

        // TraxXProgramDTO
        TraxXProgramDTO traxXProgramUpdate = new TraxXProgramDTO();
        traxXProgramUpdate.setPen(pen);
        traxXProgramUpdate.setProgramList(new ArrayList<>());

        // Event
        Event event = new Event();
        event.setEventType(EventType.XPROGRAM.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");

        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentProcess.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        when(this.restUtils.getOptionalProgram(eq(program), eq("XC"), any())).thenReturn(null);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequirementYear("2018")
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentXProgramEventService.processEvent(traxXProgramUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_XPROGRAM_whenNewCareerProgramIsAdded_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "XPROGRAM";

        // TraxXProgramDTO
        TraxXProgramDTO traxXProgramUpdate = new TraxXProgramDTO();
        traxXProgramUpdate.setPen(pen);
        traxXProgramUpdate.setProgramList(Arrays.asList("XC"));

        // Event
        Event event = new Event();
        event.setEventType(EventType.XPROGRAM.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        // None

        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentProcess.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        when(this.restUtils.getOptionalProgram(eq(program), eq("XC"), any())).thenReturn(null);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentXProgramEventService.processEvent(traxXProgramUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_XPROGRAM_whenCourseIsRemoved_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "XPROGRAM";

        // TraxXProgramDTO
        TraxXProgramDTO traxXProgramUpdate = new TraxXProgramDTO();
        traxXProgramUpdate.setPen(pen);
        traxXProgramUpdate.setProgramList(Arrays.asList("ID"));

        // Event
        Event event = new Event();
        event.setEventType(EventType.XPROGRAM.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("CUR");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("ID");

        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentProcess.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        when(this.restUtils.getOptionalProgram(eq(program), eq("XC"), any())).thenReturn(null);

        OptionalProgram optionalProgram = new OptionalProgram();
        optionalProgram.setOptionalProgramID(UUID.randomUUID());
        optionalProgram.setOptProgramCode("ID");
        optionalProgram.setOptionalProgramName("IB Diploma");
        optionalProgram.setGraduationProgramCode(program);
        when(this.restUtils.getOptionalProgram(eq(program), eq("ID"), any())).thenReturn(optionalProgram);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequirementYear("2018")
                .programCodes(Arrays.asList("ID")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentXProgramEventService.processEvent(traxXProgramUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_XPROGRAM_whenNewCourseIsAddedAndRemoved_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "XPROGRAM";

        // TraxXProgramDTO
        TraxXProgramDTO traxXProgramUpdate = new TraxXProgramDTO();
        traxXProgramUpdate.setPen(pen);
        traxXProgramUpdate.setProgramList(Arrays.asList("XC","AD"));

        // Event
        Event event = new Event();
        event.setEventType(EventType.XPROGRAM.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("CUR");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("ID");
        currentStudent.getProgramCodes().add("FI");
        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentProcess.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        when(this.restUtils.getOptionalProgram(eq(program), eq("XC"), any())).thenReturn(null);

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setOptProgramCode("FI");
        optionalProgram1.setOptionalProgramName("French Immersion");
        optionalProgram1.setGraduationProgramCode(program);
        when(this.restUtils.getOptionalProgram(eq(program), eq("FI"), any())).thenReturn(optionalProgram1);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setOptProgramCode("AD");
        optionalProgram2.setOptionalProgramName("Advance Placement");
        optionalProgram2.setGraduationProgramCode(program);
        when(this.restUtils.getOptionalProgram(eq(program), eq("AD"), any())).thenReturn(optionalProgram2);

        OptionalProgram optionalProgram3 = new OptionalProgram();
        optionalProgram3.setOptionalProgramID(UUID.randomUUID());
        optionalProgram3.setOptProgramCode("ID");
        optionalProgram3.setOptionalProgramName("IB Diploma");
        optionalProgram3.setGraduationProgramCode(program);
        when(this.restUtils.getOptionalProgram(eq(program), eq("ID"), any())).thenReturn(optionalProgram3);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC","AD")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentXProgramEventService.processEvent(traxXProgramUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForXPrograms_whenException_isThrown_returnsAPICallError() throws Exception {
        // ID
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "XPROGRAM";

        // ConvGradStudent = traxStudent with the recent updated info
        TraxXProgramDTO traxXProgram = TraxXProgramDTO.builder()
                        .programList(Arrays.asList("XC", "SN"))
                        .build();
        traxXProgram.setPen(pen);

        // Event
        Event event = new Event();
        event.setEventType(EventType.XPROGRAM.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(this.studentProcess.convertStudent(any(), any(), eq(false), eq(true))).thenThrow(new RuntimeException("Test Exception is thrown!"));

        studentXProgramEventService.processEvent(traxXProgram, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.DB_COMMITTED.name());
    }
}
