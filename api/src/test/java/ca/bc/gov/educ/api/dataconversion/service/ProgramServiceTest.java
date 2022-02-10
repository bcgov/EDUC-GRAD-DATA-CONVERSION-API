package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.program.CareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.repository.program.CareerProgramRepository;
import ca.bc.gov.educ.api.dataconversion.service.program.ProgramService;
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

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class ProgramServiceTest {

    @Autowired
    ProgramService programService;

    @MockBean
    CareerProgramRepository careerProgramRepository;

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
        careerProgramRepository.deleteAll();
    }

    @Test
    public void testGetCareerProgramCode() {
        CareerProgramEntity entity = new CareerProgramEntity();
        entity.setCode("CS");
        entity.setDescription("Career Skill Test");
        entity.setStartDate(new Date());

        when(this.careerProgramRepository.findById("CS")).thenReturn(Optional.of(entity));

        var result = programService.getCareerProgramCode("CS");
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("CS");
    }

    @Test
    public void testGetCareerProgramCode_whenNoDataExists() {
        when(this.careerProgramRepository.findById("CS")).thenReturn(Optional.empty());

        var result = programService.getCareerProgramCode("CS");
        assertThat(result).isNull();
    }
}
