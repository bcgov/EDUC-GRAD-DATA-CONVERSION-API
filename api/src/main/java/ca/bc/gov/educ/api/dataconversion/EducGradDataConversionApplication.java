package ca.bc.gov.educ.api.dataconversion;

import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRestrictionEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStatusEntity;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @Configuration
    static
    class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
        /**
         * Instantiates a new Web security configuration.
         * This makes sure that security context is propagated to async threads as well.
         */
        public WebSecurityConfiguration() {
            super();
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        }
        @Override
        public void configure(WebSecurity web) {
            web.ignoring().antMatchers("/api/v1/api-docs-ui.html",
                    "/api/v1/swagger-ui/**", "/api/v1/api-docs/**",
                    "/actuator/health","/actuator/prometheus", "/health");
        }
    }
}
