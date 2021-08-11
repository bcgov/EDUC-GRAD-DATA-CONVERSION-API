package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.GraduationStatusEntity;
import ca.bc.gov.educ.api.dataconversion.model.LoadStudentData;
import ca.bc.gov.educ.api.dataconversion.model.Student;
import ca.bc.gov.educ.api.dataconversion.repository.GraduationStatusRepository;
import ca.bc.gov.educ.api.dataconversion.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.dataconversion.rest.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GradStudentServiceTest {

    @Autowired
    GradStudentService gradStudentService;

    @Autowired
    EducGradBatchGraduationApiConstants constants;

    @MockBean
    private GraduationStatusRepository graduationStatusRepository;

    @MockBean
    private RestUtils restUtils;

    @Test
    public void testGetStudentByPenFromStudentAPI() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "123456789";

        LoadStudentData loadStudentData = new LoadStudentData();
        loadStudentData.setPen(pen);

        Student student = new Student();
        student.setPen(pen);
        student.setStudentID(studentID.toString());

        GraduationStatusEntity studentEntity = new GraduationStatusEntity();
        studentEntity.setStudentID(studentID);
        studentEntity.setPen(pen);

        when(restUtils.getStudentsByPen(pen, "accessToken")).thenReturn(Arrays.asList(student));
        when(graduationStatusRepository.findById(pen)).thenReturn(Optional.empty());
        when(graduationStatusRepository.save(any(GraduationStatusEntity.class))).thenReturn(studentEntity);

        gradStudentService.getStudentByPenFromStudentAPI(Arrays.asList(loadStudentData), "accessToken");
        Mockito.verify(graduationStatusRepository).save(any(GraduationStatusEntity.class));
    }

}
