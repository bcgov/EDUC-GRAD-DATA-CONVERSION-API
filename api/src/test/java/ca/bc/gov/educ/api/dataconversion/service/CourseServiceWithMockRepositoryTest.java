package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.course.GradCourseRestrictionEntity;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.course.GradCourseRestrictionRepository;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class CourseServiceWithMockRepositoryTest {

    @Autowired
    CourseService courseService;

    @MockBean
    GradCourseRestrictionRepository gradCourseRestrictionRepository;

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
        gradCourseRestrictionRepository.deleteAll();
    }

    @Test
    public void testConvertCourseRestriction() {
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");

        GradCourseRestriction courseRestriction = new GradCourseRestriction(
                "main", "12", "rest", "12", null, null
        );

        GradCourseRestrictionEntity gradCourseRestrictionEntity = new GradCourseRestrictionEntity();
        gradCourseRestrictionEntity.setCourseRestrictionId(UUID.randomUUID());
        gradCourseRestrictionEntity.setMainCourse("main");
        gradCourseRestrictionEntity.setMainCourseLevel("12");
        gradCourseRestrictionEntity.setRestrictedCourse("rest");
        gradCourseRestrictionEntity.setRestrictedCourseLevel("12");

        when(this.gradCourseRestrictionRepository.findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel("main", "12", "rest", "12")).thenReturn(Optional.empty());
        when(this.gradCourseRestrictionRepository.save(gradCourseRestrictionEntity)).thenReturn(gradCourseRestrictionEntity);
        courseService.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getAddedCount()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRestriction_whenGivenRecordExists() {
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");

        GradCourseRestriction courseRestriction = new GradCourseRestriction(
                "main", "12", "rest", "12", null, null
        );

        GradCourseRestrictionEntity gradCourseRestrictionEntity = new GradCourseRestrictionEntity();
        gradCourseRestrictionEntity.setCourseRestrictionId(UUID.randomUUID());
        gradCourseRestrictionEntity.setMainCourse("main");
        gradCourseRestrictionEntity.setMainCourseLevel("12");
        gradCourseRestrictionEntity.setRestrictedCourse("rest");
        gradCourseRestrictionEntity.setRestrictedCourseLevel("12");

        when(this.gradCourseRestrictionRepository.findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel("main", "12", "rest", "12")).thenReturn(Optional.of(gradCourseRestrictionEntity));
        when(this.gradCourseRestrictionRepository.save(gradCourseRestrictionEntity)).thenReturn(gradCourseRestrictionEntity);
        courseService.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getUpdatedCount()).isEqualTo(1L);
    }

    @Test
    public void testLoadInitialRawGradCourseRestrictionsData() {
        Object[] obj = new Object[] {
                "main", "12", "test", "12", null, null
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.gradCourseRestrictionRepository.loadInitialRawData()).thenReturn(results);

        var result = courseService.loadInitialRawGradCourseRestrictionsData(true);
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        GradCourseRestriction responseCourseRestriction = result.get(0);
        assertThat(responseCourseRestriction.getMainCourse()).isEqualTo("main");
    }
}
