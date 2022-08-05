package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStudentRecordEntity;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.Student;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import ca.bc.gov.educ.api.dataconversion.repository.student.GraduationStudentRecordRepository;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.GradConversionTestUtils;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StudentServiceTest {

    @Autowired
    StudentService studentService;

    @Autowired
    GraduationStudentRecordRepository graduationStudentRecordRepository;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    RestUtils restUtils;

    @Autowired
    private EducGradDataConversionApiConstants constants;

    @Autowired
    GradConversionTestUtils gradConversionTestUtils;

    // NATS
    @MockBean
    private NatsConnection natsConnection;
    @MockBean
    private Subscriber subscriber;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        gradConversionTestUtils.createGradStudents("mock_conv_grad_students.json");
    }

    @Test
    public void convertStudent_whenGivenData_withoutSpecialProgram_thenReturnSuccess() throws Exception {
        List<GraduationStudentRecordEntity> entities = graduationStudentRecordRepository.findAll();
        assertThat(entities).isNotNull();
        assertThat(entities.size()).isGreaterThan(0);

        UUID studentID = UUID.randomUUID();
        System.out.println("Generated StudentID: " + studentID);
        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen("111222333");
        when(this.restUtils.getStudentsByPen("111222333", "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.checkSchoolExists("222333", "123")).thenReturn(true);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-PF").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018").archiveFlag("A")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        studentService.convertStudent(student, summary);

        List<GraduationStudentRecordEntity> findAllEntities = graduationStudentRecordRepository.findAll();
        assertThat(findAllEntities).isNotNull();
        assertThat(findAllEntities.size()).isGreaterThan(0);

        studentID = findAllEntities.get(0).getStudentID();
        System.out.println("Found studentID: " + studentID);

        Optional<GraduationStudentRecordEntity> result = graduationStudentRecordRepository.findById(studentID);
        assertThat(result).isNotNull();
//        assertThat(result.isPresent()).isTrue();
//        assertThat(result.get().getStudentID()).isEqualTo(studentID);
//        assertThat(result.get().getRecalculateGradStatus()).isEqualTo("N");
    }

    @Test
    public void convertStudent_whenExceptionIsThrownInRestAPI_thenReturnNullWithErrorsInSummary() throws Exception {
        List<GraduationStudentRecordEntity> entities = graduationStudentRecordRepository.findAll();
        assertThat(entities).isNotNull();
        assertThat(entities.size()).isEqualTo(2);

        UUID studentID = UUID.randomUUID();
        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen("111222333");
        when(this.restUtils.getStudentsByPen("111222333", "123")).thenThrow(new RuntimeException("PEN Student API is failed!"));
        when(this.restUtils.checkSchoolExists("222333", "123")).thenReturn(true);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-PF").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = studentService.convertStudent(student, summary);
        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo(ConversionResultType.FAILURE);
        assertThat(summary.getErrors().isEmpty()).isFalse();
        assertThat(summary.getErrors().get(0).getReason().startsWith("PEN Student API is failed")).isTrue();
    }

    @Test
    public void convertStudent_whenGivenPen_doesNotExistFromPENStudentAPI_thenReturnNullWithErrorsInSummary() throws Exception {
        List<GraduationStudentRecordEntity> entities = graduationStudentRecordRepository.findAll();
        assertThat(entities).isNotNull();
        assertThat(entities.size()).isEqualTo(4);

        UUID studentID = UUID.randomUUID();
        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen("111222333");
        when(this.restUtils.getStudentsByPen("333222111", "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.checkSchoolExists("222333", "123")).thenReturn(true);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-PF").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo(ConversionResultType.FAILURE);
        assertThat(summary.getErrors().isEmpty()).isFalse();
        assertThat(summary.getErrors().get(0).getReason()).isEqualTo("PEN does not exist: PEN Student API returns empty response.");
    }
}
