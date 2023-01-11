package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.constant.EventStatus;
import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import ca.bc.gov.educ.api.dataconversion.entity.conv.Event;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
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
public class NewStudentEventServiceTest {

    @Autowired
    NewStudentEventService newStudentEventService;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    StudentService studentService;

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
    public void testProcessStudentForGrad2018ENProgram_givenNew_STUDENT_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "NEWSTUDENT";

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxNewStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("11")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .schoolAtGrad(mincode)
                .graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC","FI")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        // Event
        Event event = new Event();
        event.setEventType(EventType.NEWSTUDENT.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        newStudentEventService.processEvent(traxNewStudent, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }
}
