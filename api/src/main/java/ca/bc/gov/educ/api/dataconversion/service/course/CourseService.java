package ca.bc.gov.educ.api.dataconversion.service.course;

import ca.bc.gov.educ.api.dataconversion.entity.course.GradCourseRestrictionEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionError;
import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.repository.course.GradCourseRestrictionRepository;
import ca.bc.gov.educ.api.dataconversion.util.DateConversionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CourseService {

    private static final List<Pair<String, String>> IGNORE_LIST = new ArrayList<>() {{
        add(Pair.of("CLEA", "CLEB"));
        add(Pair.of("CLEA", "CLEBF"));
        add(Pair.of("CLEAF", "CLEB"));
        add(Pair.of("CLEAF", "CLEBF"));
        add(Pair.of("CLEB", "CLEA"));
        add(Pair.of("CLEB", "CLEAF"));
        add(Pair.of("CLEBF", "CLEA"));
        add(Pair.of("CLEBF", "CLEAF"));
    }};

    private final GradCourseRestrictionRepository gradCourseRestrictionRepository;

    @Autowired
    public CourseService(GradCourseRestrictionRepository gradCourseRestrictionRepository) {
        this.gradCourseRestrictionRepository = gradCourseRestrictionRepository;
    }

    @Transactional(transactionManager = "courseTransactionManager")
    public GradCourseRestriction convertCourseRestriction(GradCourseRestriction courseRestriction, ConversionSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        if (isInvalidData(courseRestriction.getMainCourse(), courseRestriction.getRestrictedCourse())) {
            ConversionError error = new ConversionError();
            error.setItem(courseRestriction.getMainCourse() + " " + courseRestriction.getRestrictedCourse());
            error.setReason("Skip invalid data");
            summary.getErrors().add(error);
            return null;
        }
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
        return courseRestriction;
    }

    private boolean isInvalidData(String mainCourseCode, String restrictedCourseCode) {
        Pair<String, String> pair = Pair.of(mainCourseCode, restrictedCourseCode);
        return IGNORE_LIST.contains(pair);
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
