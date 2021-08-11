package ca.bc.gov.educ.api.dataconversion;

import ca.bc.gov.educ.api.dataconversion.entity.GradCourseRestrictionEntity;
import ca.bc.gov.educ.api.dataconversion.entity.ConvGradStudentEntity;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import org.modelmapper.ModelMapper;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class EducGradDataConversionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducGradDataConversionApplication.class, args);
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.typeMap(ConvGradStudentEntity.class, ConvGradStudent.class);
        modelMapper.typeMap(ConvGradStudent.class, ConvGradStudentEntity.class);

        modelMapper.typeMap(GradCourseRestrictionEntity.class, GradCourseRestriction.class);
        modelMapper.typeMap(GradCourseRestriction.class, GradCourseRestrictionEntity.class);

        return modelMapper;
    }
}
