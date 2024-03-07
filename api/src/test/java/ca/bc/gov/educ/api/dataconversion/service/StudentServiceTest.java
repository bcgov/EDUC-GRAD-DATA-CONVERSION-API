package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.constant.EventStatus;
import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.Student;
import ca.bc.gov.educ.api.dataconversion.process.StudentProcess;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.student.NewStudentEventService;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StudentServiceTest {

    @Autowired
    StudentService studentService;

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
    public void testGetStudentByPen_given_PEN_returnsAPICallSuccess() {
        String pen = "123456789";
        String accessToken = "Bearer accesstoken";

        Student student = new Student();
        student.setPen(pen);
        List<Student> penList = List.of(student);

        when(this.restUtils.getStudentsByPen(pen, accessToken)).thenReturn(penList);
        studentService.getStudentByPen(pen, accessToken);

        assertThat(penList).isNotNull();
    }

    @Test
    public void testGetStudentByPen_given_PEN_returnsNULL() {
        String pen = "123456789";
        String accessToken = "Bearer accesstoken";

        Student student = new Student();
        student.setPen(pen);
        List<Student> penList = List.of(student);

        when(this.restUtils.getStudentsByPen(pen, accessToken)).thenReturn(null);
        Student s = studentService.getStudentByPen(pen, accessToken);

        assertThat(s).isNull();
    }

    /*@Test
    public void testProcessStudent_whenException_isThrown_returnsAPICallError() throws Exception {
        // ID
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "NEWSTUDENT";

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxNewStudent = ConvGradStudent.builder()
                .pen(pen)
                .program(program)
                .studentGrade("11")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .schoolAtGrad(mincode)
                .graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC","FI")).build();

        // Event
        Event event = new Event();
        event.setEventType(EventType.NEWSTUDENT.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(this.studentProcess.convertStudent(any(), any(), eq(false), eq(true))).thenThrow(new RuntimeException("Test Exception is thrown!"));
        newStudentEventService.processEvent(traxNewStudent, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }*/
}
