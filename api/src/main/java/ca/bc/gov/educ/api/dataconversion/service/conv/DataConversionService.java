package ca.bc.gov.educ.api.dataconversion.service.conv;

import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.ConvGradCourseRestrictionRepository;
import ca.bc.gov.educ.api.dataconversion.repository.conv.ConvGradStudentRepository;
import ca.bc.gov.educ.api.dataconversion.repository.conv.ConvGradStudentSpecialProgramRepository;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DataConversionService {
    private final ConvGradStudentRepository convGradStudentRepository;
    private final ConvGradCourseRestrictionRepository convGradCourseRestrictionRepository;

    @Autowired
    public DataConversionService(ConvGradStudentRepository convGradStudentRepository, ConvGradCourseRestrictionRepository convGradCourseRestrictionRepository) {
        this.convGradStudentRepository = convGradStudentRepository;
        this.convGradCourseRestrictionRepository = convGradCourseRestrictionRepository;
    }

    @Transactional(readOnly = true, transactionManager = "convTransactionManager")
    public List<ConvGradStudent> loadInitialRawGradStudentData(boolean purge) {
        if (purge) {
            convGradStudentRepository.deleteAll();
            convGradStudentRepository.flush();
        }
        List<ConvGradStudent> students = new ArrayList<>();
        List<Object[]> results = convGradStudentRepository.loadInitialRawData();
        results.forEach(result -> {
            String pen = (String) result[0];
            String schoolOfRecord = (String) result[1];
            String schoolAtGrad = (String) result[2];
            String studentGrade = (String) result[3];
            Character studentStatus = (Character) result[4];
            String graduationRequestYear = (String) result[5];
            Character recalculateGradStatus = (Character) result[6];
            ConvGradStudent student = new ConvGradStudent(
                    pen, null, null, null, null,
                    recalculateGradStatus.toString(), null, schoolOfRecord, schoolAtGrad, studentGrade,
                    studentStatus != null? studentStatus.toString() : null, graduationRequestYear);
            students.add(student);
        });

        return students;
    }

    @Transactional(readOnly = true, transactionManager = "convTransactionManager")
    public List<GradCourseRestriction> loadInitialRawGradCourseRestrictionsData(boolean purge) {
        if (purge) {
            convGradCourseRestrictionRepository.deleteAll();
            convGradCourseRestrictionRepository.flush();
        }
        List<GradCourseRestriction> courseRestrictions = new ArrayList<>();
        List<Object[]> results = convGradCourseRestrictionRepository.loadInitialRawData();
        results.forEach(result -> {
            String mainCourse = (String) result[0];
            String mainCourseLevel = (String) result[1];
            String restrictedCourse = (String) result[2];
            String restrictedCourseLevel = (String) result[3];
            String startDate = (String) result[4];
            String endDate = (String) result[5];
            GradCourseRestriction courseRestriction = new GradCourseRestriction(
                    mainCourse, mainCourseLevel, restrictedCourse, restrictedCourseLevel, startDate, endDate);
            courseRestrictions.add(courseRestriction);
        });
        return courseRestrictions;
    }
}
