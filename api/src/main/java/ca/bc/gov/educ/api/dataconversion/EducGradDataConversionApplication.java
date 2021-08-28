package ca.bc.gov.educ.api.dataconversion;

import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRestrictionEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStatusEntity;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@SpringBootApplication
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class EducGradDataConversionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducGradDataConversionApplication.class, args);
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.typeMap(GraduationStatusEntity.class, ConvGradStudent.class);
        modelMapper.typeMap(ConvGradStudent.class, GraduationStatusEntity.class);

        modelMapper.typeMap(CourseRestrictionEntity.class, GradCourseRestriction.class);
        modelMapper.typeMap(GradCourseRestriction.class, CourseRestrictionEntity.class);

        return modelMapper;
    }
}
