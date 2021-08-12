package ca.bc.gov.educ.api.dataconversion.service.course;

import ca.bc.gov.educ.api.dataconversion.entity.course.GradCourseRestrictionEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.repository.course.GradCourseRestrictionRepository;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import ca.bc.gov.educ.api.dataconversion.util.DateConversionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CourseService {

    private final GradCourseRestrictionRepository gradCourseRestrictionRepository;

    @Autowired
    public CourseService(GradCourseRestrictionRepository gradCourseRestrictionRepository) {
        this.gradCourseRestrictionRepository = gradCourseRestrictionRepository;
    }

    @Transactional(transactionManager = "courseTransactionManager")
    public void convertCourseRestriction(GradCourseRestriction courseRestriction, ConversionSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        Optional<GradCourseRestrictionEntity> optional =  gradCourseRestrictionRepository.findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel(
                courseRestriction.getMainCourse(), courseRestriction.getMainCourseLevel(), courseRestriction.getRestrictedCourse(), courseRestriction.getRestrictedCourseLevel());

        GradCourseRestrictionEntity entity = optional.orElseGet(GradCourseRestrictionEntity::new);
        convertCourseRestrictionData(courseRestriction, entity);
        gradCourseRestrictionRepository.save(entity);
        if (optional.isPresent()) {
            summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
        } else {
            summary.setAddedCount(summary.getAddedCount() + 1L);
        }
    }

    @Transactional(readOnly = true, transactionManager = "courseTransactionManager")
    public List<GradCourseRestriction> loadInitialRawGradCourseRestrictionsData(boolean purge) {
        if (purge) {
            gradCourseRestrictionRepository.deleteAll();
            gradCourseRestrictionRepository.flush();
        }
        List<GradCourseRestriction> courseRestrictions = new ArrayList<>();
        List<Object[]> results = gradCourseRestrictionRepository.loadInitialRawData();
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

    @Transactional(transactionManager = "courseTransactionManager")
    public void removeGradCourseRestriction(String mainCourseCode, String restrictedCourseCode, ConversionSummaryDTO summary) {
        List<GradCourseRestrictionEntity> removalList = gradCourseRestrictionRepository.findByMainCourseAndRestrictedCourse(mainCourseCode, restrictedCourseCode);
        removalList.forEach(c -> {
            gradCourseRestrictionRepository.delete(c);
            summary.setAddedCount(summary.getAddedCount() - 1L);
        });
    }

    private void convertCourseRestrictionData(GradCourseRestriction courseRestriction, GradCourseRestrictionEntity courseRestrictionEntity) {
        if (courseRestrictionEntity.getCourseRestrictionId() == null) {
            courseRestrictionEntity.setCourseRestrictionId(UUID.randomUUID());
        }
        courseRestrictionEntity.setMainCourse(courseRestriction.getMainCourse());
        courseRestrictionEntity.setMainCourseLevel(courseRestriction.getMainCourseLevel());
        courseRestrictionEntity.setRestrictedCourse(courseRestriction.getRestrictedCourse());
        courseRestrictionEntity.setRestrictedCourseLevel(courseRestriction.getRestrictedCourseLevel());
        // data conversion
        if (StringUtils.isNotBlank(courseRestriction.getRestrictionStartDate())) {
            Date start = DateConversionUtils.convertStringToDate(courseRestriction.getRestrictionStartDate());
            if (start != null) {
                courseRestrictionEntity.setRestrictionStartDate(start);
            }
        }
        if (StringUtils.isNotBlank(courseRestriction.getRestrictionEndDate())) {
            Date end = DateConversionUtils.convertStringToDate(courseRestriction.getRestrictionEndDate());
            if (end != null) {
                courseRestrictionEntity.setRestrictionEndDate(end);
            }
        }
    }

    @Transactional(readOnly = true, transactionManager = "courseTransactionManager")
    public boolean isFrenchImmersionCourse(String pen) {
        if (this.gradCourseRestrictionRepository.countFrenchImmersionCourses(pen) > 0L) {
            return true;
        }
        return false;
    }
}
