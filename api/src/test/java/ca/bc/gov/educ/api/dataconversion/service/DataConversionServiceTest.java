package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.trax.TraxStudentEntity;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import ca.bc.gov.educ.api.dataconversion.repository.trax.TraxStudentsLoadRepository;
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

import java.math.BigDecimal;
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
    TraxStudentsLoadRepository traxStudentsLoadRepository;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    RestUtils restUtils;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testLoadInitialRawGradStudentData() {
        Object[] obj = new Object[] {
               "123456789", "12345678", "12345678", "12", Character.valueOf('A'),Character.valueOf('A'), "2020", Character.valueOf('Y'),
                BigDecimal.ZERO, null, null, null, null, null, null, null, null
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.traxStudentsLoadRepository.loadAllTraxStudents()).thenReturn(results);

        var result = dataConversionService.loadGradStudentsDataFromTrax();
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        ConvGradStudent responseStudent = result.get(0);
        assertThat(responseStudent.getPen()).isEqualTo(obj[0]);
    }

    @Test
    public void testLoadInitialRawGradCourseRestrictionsData() {
        Object[] obj = new Object[] {
                "main", "12", "test", "12", null, null
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.traxStudentsLoadRepository.loadInitialCourseRestrictionRawData()).thenReturn(results);

        var result = dataConversionService.loadGradCourseRestrictionsDataFromTrax();
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        GradCourseRestriction responseCourseRestriction = result.get(0);
        assertThat(responseCourseRestriction.getMainCourse()).isEqualTo("main");
    }

    @Test
    public void testGetStudentDemographicsDataFromTrax() {
        Object[] obj = new Object[] {
                "123456789", "Test", "QA", "", Character.valueOf('A'),Character.valueOf('A'), "12345678", "12", "V4N3Y2", Character.valueOf('M'), "19800111",  BigDecimal.valueOf(202005), null, "            "
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.traxStudentsLoadRepository.loadStudentDemographicsData("123456789")).thenReturn(results);

        var result = dataConversionService.getStudentDemographicsDataFromTrax("123456789");
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
        TraxStudentEntity convGradStudent = new TraxStudentEntity();
        convGradStudent.setStudNo(pen);

        Object[] obj = new Object[] {
                pen, "Test", "QA", "", Character.valueOf('A'),Character.valueOf('A'), "12345678", "12", "V4N3Y2", Character.valueOf('M'), "19800111",  BigDecimal.valueOf(202005), null, "            "
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.traxStudentsLoadRepository.loadStudentDemographicsData(pen)).thenReturn(results);
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
        TraxStudentEntity convGradStudent = new TraxStudentEntity();
        convGradStudent.setStudNo(pen);

        Object[] obj = new Object[] {
                pen, "Test", "QA", "", Character.valueOf('A'),Character.valueOf('A'), "12345678", "12", "V4N3Y2", Character.valueOf('M'), "19800111",  BigDecimal.valueOf(202005), null, "            "
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.traxStudentsLoadRepository.loadStudentDemographicsData(pen)).thenReturn(results);
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
        TraxStudentEntity convGradStudent = new TraxStudentEntity();
        convGradStudent.setStudNo(pen);

        Object[] obj = new Object[] {
                pen, "Test", "QA", "", Character.valueOf('A'),Character.valueOf('A'), "12345678", "12", "V4N3Y2", Character.valueOf('M'), "19800111",  BigDecimal.valueOf(202005), null, "            "
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.traxStudentsLoadRepository.loadStudentDemographicsData(pen)).thenReturn(results);
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
        TraxStudentEntity convGradStudent = new TraxStudentEntity();
        convGradStudent.setStudNo(pen);

        Object[] obj = new Object[] {
                pen, "Test", "QA", "", Character.valueOf('A'),Character.valueOf('A'), "12345678", "12", "V4N3Y2", Character.valueOf('M'), "19800111",  BigDecimal.valueOf(202005), null, "            "
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.traxStudentsLoadRepository.loadStudentDemographicsData(pen)).thenReturn(results);
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
        TraxStudentEntity convGradStudent = new TraxStudentEntity();
        convGradStudent.setStudNo(pen);

        Object[] obj = new Object[] {
                pen, "Test", "QA", "", Character.valueOf('M'),Character.valueOf('A'), "12345678", "12", "V4N3Y2", Character.valueOf('M'), "19800111",  BigDecimal.valueOf(202005), "987654321", "            "
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.traxStudentsLoadRepository.loadStudentDemographicsData(pen)).thenReturn(results);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(new ArrayList<>());

        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        var result = dataConversionService.readTraxStudentAndAddNewPen(convGradStudent, summary);
        assertThat(result).isNotNull();
        assertThat(result.getStudNo()).isEqualTo(pen);
    }
}
