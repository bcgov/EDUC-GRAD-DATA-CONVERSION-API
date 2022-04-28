package ca.bc.gov.educ.api.dataconversion;

import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStudentRecordEntity;
import ca.bc.gov.educ.api.dataconversion.model.CourseRestriction;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootApplication
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableScheduling
@EnableRetry
@EnableSchedulerLock(defaultLockAtMostFor = "1s")
public class EducGradDataConversionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducGradDataConversionApplication.class, args);
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.typeMap(GraduationStudentRecordEntity.class, ConvGradStudent.class);
        modelMapper.typeMap(ConvGradStudent.class, GraduationStudentRecordEntity.class);

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
        @Override
        protected void configure(final HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .anyRequest()
                    .authenticated()
                    .and()
                    .oauth2ResourceServer()
                    .jwt();
        }
    }

    /**
     * Lock provider lock provider.
     *
     * @param jdbcTemplate       the jdbc template
     * @param transactionManager the transaction manager
     * @return the lock provider
     */
    @Bean
    public LockProvider lockProvider(@Autowired JdbcTemplate jdbcTemplate, @Autowired PlatformTransactionManager transactionManager) {
        return new JdbcTemplateLockProvider(jdbcTemplate, transactionManager, "EVENT_SHEDLOCK");
    }
}
