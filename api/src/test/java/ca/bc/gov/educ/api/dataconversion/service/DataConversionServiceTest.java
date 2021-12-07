package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.model.*;
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
                BigDecimal.ZERO, null, null, null, null, null
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.traxStudentsLoadRepository.loadInitialStudentRawData()).thenReturn(results);

        var result = dataConversionService.loadInitialRawGradStudentData();
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

        var result = dataConversionService.loadInitialRawGradCourseRestrictionsData();
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        GradCourseRestriction responseCourseRestriction = result.get(0);
        assertThat(responseCourseRestriction.getMainCourse()).isEqualTo("main");
    }

}
