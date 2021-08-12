package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.conv.ConvGradStudentEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.Student;
import ca.bc.gov.educ.api.dataconversion.repository.conv.ConvGradStudentRepository;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.GradConversionTestUtils;
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
public class DataConversionServiceTest {

    @Autowired
    DataConversionService dataConversionService;

    @Autowired
    ConvGradStudentRepository convGradStudentRepository;

    @MockBean
    RestUtils restUtils;

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
        convGradStudentRepository.deleteAll();
    }

    @Test
    public void convertStudent_whenGivenData_withoutSpecialProgram_thenReturnSuccess() throws Exception {
        gradConversionTestUtils.createConvGradStudents("mock_conv_grad_students.json");

        List<ConvGradStudentEntity> entities = convGradStudentRepository.findAll();
        assertThat(entities).isNotNull();
        assertThat(entities.size()).isEqualTo(2);

        UUID studentID = UUID.randomUUID();
        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen("111222333");
        when(this.restUtils.getStudentsByPen("111222333", "123")).thenReturn(Arrays.asList(penStudent));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-PF").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018").build();
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");
        dataConversionService.convertStudent(student, summary);

        entities = convGradStudentRepository.findAll();
        assertThat(entities).isNotNull();
        assertThat(entities.size()).isEqualTo(3);

        Optional<ConvGradStudentEntity> result = convGradStudentRepository.findById(studentID);
        assertThat(result).isNotNull();
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getStudentID()).isEqualTo(studentID);
        assertThat(result.get().getRecalculateGradStatus()).isEqualTo("Y");
    }

    @Test
    public void convertStudent_whenExceptionIsThrownInRestAPI_thenReturnNullWithErrorsInSummary() throws Exception {
        gradConversionTestUtils.createConvGradStudents("mock_conv_grad_students.json");

        List<ConvGradStudentEntity> entities = convGradStudentRepository.findAll();
        assertThat(entities).isNotNull();
        assertThat(entities.size()).isEqualTo(2);

        UUID studentID = UUID.randomUUID();
        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen("111222333");
        when(this.restUtils.getStudentsByPen("111222333", "123")).thenThrow(new RuntimeException("PEN Student API is failed!"));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-PF").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018").build();
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");

        var result = dataConversionService.convertStudent(student, summary);
        assertThat(result).isNull();
        assertThat(summary.getErrors().isEmpty()).isFalse();
        assertThat(summary.getErrors().get(0).getReason().startsWith("PEN Student API is failed")).isTrue();
    }

    @Test
    public void convertStudent_whenGivenPen_doesNotExistFromPENStudentAPI_thenReturnNullWithErrorsInSummary() throws Exception {
        gradConversionTestUtils.createConvGradStudents("mock_conv_grad_students.json");

        List<ConvGradStudentEntity> entities = convGradStudentRepository.findAll();
        assertThat(entities).isNotNull();
        assertThat(entities.size()).isEqualTo(2);

        UUID studentID = UUID.randomUUID();
        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen("111222333");
        when(this.restUtils.getStudentsByPen("333222111", "123")).thenReturn(Arrays.asList(penStudent));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-PF").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018").build();
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");
        var result = dataConversionService.convertStudent(student, summary);

        assertThat(result).isNull();
        assertThat(summary.getErrors().isEmpty()).isFalse();
        assertThat(summary.getErrors().get(0).getReason()).isEqualTo("PEN does not exist: PEN Student API returns empty response.");
    }
}
