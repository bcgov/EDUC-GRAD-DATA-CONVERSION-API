package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.ConvGradCourseRestrictionRepository;
import ca.bc.gov.educ.api.dataconversion.repository.conv.ConvGradStudentRepository;
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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DataConversionServiceTest {

    @Autowired
    DataConversionService dataConversionService;

    @MockBean
    ConvGradStudentRepository convGradStudentRepository;

    @MockBean
    ConvGradCourseRestrictionRepository convGradCourseRestrictionRepository;

    @MockBean
    RestUtils restUtils;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {
        convGradStudentRepository.deleteAll();
    }

    @Test
    public void testLoadInitialRawGradStudentData() {
        Object[] obj = new Object[] {
               "123456789", "12345678", "12345678", "12", Character.valueOf('A'), "2020", Character.valueOf('Y')
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.convGradStudentRepository.loadInitialRawData()).thenReturn(results);

        var result = dataConversionService.loadInitialRawGradStudentData(true);
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

        when(this.convGradCourseRestrictionRepository.loadInitialRawData()).thenReturn(results);

        var result = dataConversionService.loadInitialRawGradCourseRestrictionsData(true);
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        GradCourseRestriction responseCourseRestriction = result.get(0);
        assertThat(responseCourseRestriction.getMainCourse()).isEqualTo("main");
    }

}
