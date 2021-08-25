package ca.bc.gov.educ.api.dataconversion.util;

import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStatusEntity;
import ca.bc.gov.educ.api.dataconversion.mappers.ConvGradStudentMapper;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.repository.course.GradCourseRestrictionRepository;
import ca.bc.gov.educ.api.dataconversion.repository.student.GraduationStatusRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Component
@Profile("test")
public class GradConversionTestUtils {
    @Autowired
    GraduationStatusRepository graduationStatusRepository;

    @Autowired
    private ConvGradStudentMapper mapper;

    public static GraduationStatusEntity populateIdAndAuditColumns(final GraduationStatusEntity entity) {
        if (entity.getStudentID() == null) {
            entity.setStudentID(UUID.randomUUID());
        }
        if (entity.getCreateUser() == null) {
            entity.setCreateUser(EducGradDataConversionApiConstants.DEFAULT_CREATED_BY);
        }
        if (entity.getUpdateUser() == null) {
            entity.setUpdateUser(EducGradDataConversionApiConstants.DEFAULT_UPDATED_BY);
        }

        entity.setCreateDate(new Date(System.currentTimeMillis()));
        entity.setUpdateDate(new Date(System.currentTimeMillis()));

        return entity;
    }

    public List<GraduationStatusEntity> createGradStudents(final String jsonFileName) throws IOException {
        final File file = new File(
                Objects.requireNonNull(GradConversionTestUtils.class.getClassLoader().getResource(jsonFileName)).getFile()
        );
        final List<ConvGradStudent> models = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        final var entities = models.stream().map(mapper::toEntity)
                .collect(toList()).stream().map(GradConversionTestUtils::populateIdAndAuditColumns).collect(toList());

        graduationStatusRepository.saveAll(entities);
        return entities;
    }


}
