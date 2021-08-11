package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.model.AlgorithmResponse;
import ca.bc.gov.educ.api.dataconversion.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ConversionError;
import ca.bc.gov.educ.api.dataconversion.model.GraduationStatus;
import ca.bc.gov.educ.api.dataconversion.rest.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GradAlgorithmServiceTest {

    @Autowired
    GradAlgorithmService gradAlgorithmService;

    @MockBean
    RestUtils restUtils;

    @Test
    public void testProcessStudent() {
        GraduationStatus item = new GraduationStatus();
        item.setStudentID(UUID.randomUUID());
        item.setPen("123456789");

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setAccessToken("123");

        AlgorithmResponse response = new AlgorithmResponse();
        response.setGraduationStatus(item);

        when(restUtils.runGradAlgorithm(eq(item.getStudentID()), eq(summary.getAccessToken()))).thenReturn(response);
        var result =  gradAlgorithmService.processStudent(item, summary);
        assertThat(result).isNotNull();
        assertThat(result.getStudentID()).isEqualTo(item.getStudentID());
    }

    @Test
    public void testProcessStudent_whenExceptionOccurs_thenReturnNullWithErrorsInSummary() {
        GraduationStatus item = new GraduationStatus();
        item.setStudentID(UUID.randomUUID());
        item.setPen("123456789");

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setAccessToken("123");

        AlgorithmResponse response = new AlgorithmResponse();
        response.setGraduationStatus(item);

        when(restUtils.runGradAlgorithm(item.getStudentID(), eq(any(String.class)))).thenThrow(new RuntimeException("Unexpected Exception is occurred!"));
        var result =  gradAlgorithmService.processStudent(item, summary);
        assertThat(result).isNull();
        assertThat(summary.getErrors().isEmpty()).isFalse();
        ConversionError error = summary.getErrors().get(0);
        assertThat(error.getPen()).isEqualTo(item.getPen());
        assertThat(error.getReason().startsWith("Unexpected Exception is occurred:")).isTrue();
    }
}
