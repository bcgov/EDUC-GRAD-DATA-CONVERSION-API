package ca.bc.gov.educ.api.dataconversion.controller;

import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.service.DataConversionService;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class DataConversionControllerTest {

    @Mock
    private DataConversionService dataConversionService;

    @InjectMocks
    DataConversionController dataConversionController;

    @Test
    public void testRunCourseRestrictionsDataConversionJob() {
        GradCourseRestriction gradCourseRestriction1 = new GradCourseRestriction();
        gradCourseRestriction1.setMainCourse("main");
        gradCourseRestriction1.setMainCourseLevel("12");
        gradCourseRestriction1.setRestrictedCourse("rest");
        gradCourseRestriction1.setRestrictedCourseLevel("12");

        GradCourseRestriction gradCourseRestriction2 = new GradCourseRestriction();
        gradCourseRestriction2.setMainCourse("CLEA");
        gradCourseRestriction2.setMainCourseLevel("12");
        gradCourseRestriction2.setRestrictedCourse("CLEB");
        gradCourseRestriction2.setRestrictedCourseLevel("12");

        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setTableName("GRAD_COURSE_RESTRICTIONS");

        Mockito.when(dataConversionService.loadInitialRawGradCourseRestrictionsData(true)).thenReturn(Arrays.asList(gradCourseRestriction1, gradCourseRestriction2));
        var result = dataConversionController.runCourseRestrictionsDataConversionJob(true);
        Mockito.verify(dataConversionService).loadInitialRawGradCourseRestrictionsData(true);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        ConversionSummaryDTO responseSummary = result.getBody();
        assertThat(responseSummary.getReadCount()).isEqualTo(2L);
    }

}
