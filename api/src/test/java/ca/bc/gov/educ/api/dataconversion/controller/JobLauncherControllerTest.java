package ca.bc.gov.educ.api.dataconversion.controller;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;


import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class JobLauncherControllerTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobRegistry jobRegistry;

    @InjectMocks
    private JobLauncherController jobLauncherController;

    @Test
    public void testLaunchGradStudent_DataConversionJob_thenReturnError() {
        boolean exceptionIsThrown = false;
        try {
            jobLauncherController.launchStudentDataConversionJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isTrue();
    }

    @Test
    public void testLaunchGradStudentWithPartitions_DataConversionJob_thenReturnError() {
        boolean exceptionIsThrown = false;
        try {
            jobLauncherController.launchStudentDataConversionPartitionJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();
    }



    @Test
    public void testLaunchCourseRestriction_DataConversionJob_thenReturnError() {
        boolean exceptionIsThrown = false;
        try {
            jobLauncherController.launchCourseRestrictionDataConversionJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();
    }

    @Test
    public void testLaunchCourseRequirement_DataConversionJob_thenReturnError() {
        boolean exceptionIsThrown = false;
        try {
            jobLauncherController.launchCourseRequirementDataConversionJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();
    }

    @Test
    public void testReadTraxAndAddNewPenIfNotExists_Job_thenReturnError() {
        boolean exceptionIsThrown = false;
        try {
            jobLauncherController.launchPenUpdatesJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();
    }
}
