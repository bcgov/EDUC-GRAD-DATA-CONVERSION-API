package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRestrictionEntity;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.course.CourseRestrictionRepository;
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
    CourseRestrictionRepository courseRestrictionRepository;

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
        courseRestrictionRepository.deleteAll();
    }

    @Test
    public void testConvertCourseRestriction() {
        ConversionBaseSummaryDTO summary = new ConversionBaseSummaryDTO();
        summary.setAccessToken("123");

        GradCourseRestriction courseRestriction = new GradCourseRestriction(
                "main", "12", "rest", "12", null, null
        );

        CourseRestrictionEntity courseRestrictionEntity = new CourseRestrictionEntity();
        courseRestrictionEntity.setCourseRestrictionId(UUID.randomUUID());
        courseRestrictionEntity.setMainCourse("main");
        courseRestrictionEntity.setMainCourseLevel("12");
        courseRestrictionEntity.setRestrictedCourse("rest");
        courseRestrictionEntity.setRestrictedCourseLevel("12");

        when(this.courseRestrictionRepository.findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel("main", "12", "rest", "12")).thenReturn(Optional.empty());
        when(this.courseRestrictionRepository.save(courseRestrictionEntity)).thenReturn(courseRestrictionEntity);
        courseService.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getAddedCount()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRestriction_whenGivenRecordExists() {
        ConversionBaseSummaryDTO summary = new ConversionBaseSummaryDTO();
        summary.setAccessToken("123");

        GradCourseRestriction courseRestriction = new GradCourseRestriction(
                "main", "12", "rest", "12", null, null
        );

        CourseRestrictionEntity courseRestrictionEntity = new CourseRestrictionEntity();
        courseRestrictionEntity.setCourseRestrictionId(UUID.randomUUID());
        courseRestrictionEntity.setMainCourse("main");
        courseRestrictionEntity.setMainCourseLevel("12");
        courseRestrictionEntity.setRestrictedCourse("rest");
        courseRestrictionEntity.setRestrictedCourseLevel("12");

        when(this.courseRestrictionRepository.findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel("main", "12", "rest", "12")).thenReturn(Optional.of(courseRestrictionEntity));
        when(this.courseRestrictionRepository.save(courseRestrictionEntity)).thenReturn(courseRestrictionEntity);
        courseService.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getUpdatedCount()).isEqualTo(1L);
    }

}
