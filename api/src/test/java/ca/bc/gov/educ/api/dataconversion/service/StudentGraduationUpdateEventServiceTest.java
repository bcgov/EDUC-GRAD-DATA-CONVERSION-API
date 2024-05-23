package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.constant.EventStatus;
import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentGraduationUpdateEventService;
import ca.bc.gov.educ.api.dataconversion.process.StudentProcess;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StudentGraduationUpdateEventServiceTest {

    @Autowired
    StudentGraduationUpdateEventService studentGraduationUpdateEventService;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    StudentProcess studentProcess;

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

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_STUDENT_returnsAPICallFailure() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setStudentGrade("12");
        traxGraduationUpdate.setSchoolOfRecord(mincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
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

        ResponseObj res = new ResponseObj();
        res.setAccess_token("accessToken");
        res.setRefresh_token("refreshToken");
        when(this.restUtils.getTokenResponseObject()).thenReturn(res);
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_STUDENT_whenGradeAndSchoolAreChanged_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setCitizenship("C");
        traxGraduationUpdate.setStudentGrade("11");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
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
        currentStudent.setCitizenship("C");
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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessArchivedStudentForGrad2018ENProgram_givenUpdated_Archived_STUDENT_whenGradeAndSchoolAreChanged_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setCitizenship("C");
        traxGraduationUpdate.setStudentGrade("11");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("I");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("ARC");
        currentStudent.setCitizenship("C");
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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessCurrentGraduatedStudentForGrad2018ENProgram_givenUpdated_Archived_STUDENT_whenGradeAndSchoolAreChanged_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setCitizenship("C");
        traxGraduationUpdate.setStudentGrade("11");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("I");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
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
        currentStudent.setCitizenship("C");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        currentStudent.setGradDate("2022/02");
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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessCurrentStudentForGrad2018ENProgram_givenUpdated_Archived_STUDENT_whenGradeAndSchoolAreChanged_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setCitizenship("C");
        traxGraduationUpdate.setStudentGrade("11");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("I");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
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
        currentStudent.setCitizenship("C");
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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessArchivedStudentForGrad2018ENProgram_givenUpdated_Current_STUDENT_whenGradeAndSchoolAreChangedAndStatusIsArchived_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setCitizenship("C");
        traxGraduationUpdate.setStudentGrade("11");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("ARC");
        currentStudent.setCitizenship("C");
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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_STUDENT_whenSlpDateAndCitizenshipAreChanged_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newCitizenship = "C";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setStudentGrade("12");
        traxGraduationUpdate.setSchoolOfRecord(mincode);
        traxGraduationUpdate.setCitizenship(newCitizenship);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
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
        currentStudent.setCitizenship(null);
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        currentStudent.setGradDate(null);
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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_STUDENT_whenProgramIsChangedTo2018PF_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newMincode = "093444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setStudentGrade("12");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
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
        currentStudent.getProgramCodes().add("DD");
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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018PFProgram_givenUpdated_STUDENT_whenProgramIsChangedTo2018EN_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-PF";
        String mincode = "093333";

        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setStudentGrade("12");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
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
        currentStudent.getProgramCodes().add("DD");
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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentFor1950AdultProgram_givenUpdated_STUDENT_whenProgramIsChangedTo2018EN_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "1950";
        String mincode = "111222";

        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setStudentGrade("12");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
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
        currentStudent.getProgramCodes().add("DD");
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
        when(this.studentProcess.hasAnyFrenchImmersionCourse(eq("2018-EN"), eq(pen), any())).thenReturn(true);
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentFor2018EN_givenUpdated_STUDENT_whenProgramIsChangedTo1950Adult_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("1950");
        traxGraduationUpdate.setStudentGrade("AD");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        currentStudent.setBirthday("2000-01-01");
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("CUR");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("DD");

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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentFor2018ENProgram_givenUpdated_STUDENT_whenProgramIsChangedToSCCP_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newMincode = "333444";
        String newSlpDate = "202006";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("SCCP");
        traxGraduationUpdate.setStudentGrade("12");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setSlpDate(newSlpDate);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        currentStudent.setBirthday("2000-01-01");
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("CUR");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("DD");

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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentFor2018PFProgram_givenUpdated_STUDENT_whenProgramIsChangedToSCCP_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-PF";
        String mincode = "093333";

        String newMincode = "333444";
        String newSlpDate = "202006";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("SCCP");
        traxGraduationUpdate.setStudentGrade("12");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setSlpDate(newSlpDate);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        currentStudent.setBirthday("2000-01-01");
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("CUR");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("DD");

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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForSCCPProgram_givenUpdated_STUDENT_whenProgramIsChangedTo2018EN_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "SCCP";
        String mincode = "222333";

        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setStudentGrade("12");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        currentStudent.setBirthday("2000-01-01");
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("CUR");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        currentStudent.setGradDate("2022/01");
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("DD");

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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_whenException_isThrown_returnsAPICallError() throws Exception {
        // ID
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "UPD_GRAD";

        TraxGraduationUpdateDTO traxStudentUpdate = new TraxGraduationUpdateDTO();
        traxStudentUpdate.setPen(pen);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(this.studentProcess.convertStudent(any(), any(), eq(false), eq(true))).thenThrow(new RuntimeException("Test Exception is thrown!"));

        studentGraduationUpdateEventService.processEvent(traxStudentUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.DB_COMMITTED.name());
    }

    @Test
    public void testProcessGraduatedStudentForGrad2018ENProgram_givenUpdated_STUDENT_whenProgramIsChangedTo2018PF_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newMincode = "093444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setStudentGrade("12");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
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
        currentStudent.setGradDate("2022/06");
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("DD");
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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessGraduatedStudentForSCCP_givenUpdated_STUDENT_whenSlpDateIsChanged_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "SCCP";
        String mincode = "093333";

        String newSlpDate = "202006";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("SCCP");
        traxGraduationUpdate.setStudentGrade("12");
        traxGraduationUpdate.setSchoolOfRecord(mincode);
        traxGraduationUpdate.setSlpDate(newSlpDate);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        currentStudent.setBirthday("2000-01-01");
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("CUR");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        currentStudent.setGradDate("2022/06");
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("DD");

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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessGraduatedStudentForGrad2018ENProgram_givenUpdated_STUDENT_whenGradeAndSchoolAreChanged_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setCitizenship("C");
        traxGraduationUpdate.setStudentGrade("11");
        traxGraduationUpdate.setSchoolOfRecord(newMincode);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
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
        currentStudent.setCitizenship("C");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        currentStudent.setGradDate("2022/06");
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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessGraduatedStudentForGrad2018ENProgram_givenUpdated_STUDENT_whenCitizenshipIsChanged_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newCitizenship = "C";

        String updateType = "UPD_GRAD";

        // TraxGraduationUpdateDTO
        TraxGraduationUpdateDTO traxGraduationUpdate = new TraxGraduationUpdateDTO();
        traxGraduationUpdate.setPen(pen);
        traxGraduationUpdate.setGraduationRequirementYear("2018");
        traxGraduationUpdate.setStudentGrade("12");
        traxGraduationUpdate.setSchoolOfRecord(mincode);
        traxGraduationUpdate.setCitizenship(newCitizenship);
        traxGraduationUpdate.setStudentStatus("A");
        traxGraduationUpdate.setArchiveFlag("A");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_GRAD.name());
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
        currentStudent.setCitizenship(null);
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        currentStudent.setGradDate("2022/06");
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
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentGraduationUpdateEventService.processEvent(traxGraduationUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

}
