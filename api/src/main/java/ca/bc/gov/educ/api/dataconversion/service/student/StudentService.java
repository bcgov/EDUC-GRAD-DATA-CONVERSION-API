package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.conv.ConvGradStudentEntity;
import ca.bc.gov.educ.api.dataconversion.entity.conv.ConvGradStudentSpecialProgramEntity;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.ConvGradStudentRepository;
import ca.bc.gov.educ.api.dataconversion.repository.conv.ConvGradStudentSpecialProgramRepository;
import ca.bc.gov.educ.api.dataconversion.repository.student.GraduationStatusRepository;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class StudentService {

    private final GraduationStatusRepository graduationStatusRepository;

    @Autowired
    public StudentService(GraduationStatusRepository graduationStatusRepository) {
        this.graduationStatusRepository = graduationStatusRepository;
    }

}
