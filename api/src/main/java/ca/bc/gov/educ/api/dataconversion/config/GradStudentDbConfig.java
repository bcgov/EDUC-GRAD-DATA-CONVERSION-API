package ca.bc.gov.educ.api.dataconversion.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@Profile("!test")
@EnableJpaRepositories(
        basePackages = {
                "ca.bc.gov.educ.api.dataconversion.repository.student"
        },
        entityManagerFactoryRef = "studentEntityManager",
        transactionManagerRef = "studentTransactionManager"
)
@EnableTransactionManagement
public class GradStudentDbConfig {
    // Hikari Pool
    @Value("${spring.db-connection.hikari.maximum-pool-size}")
    private int maxPoolSize;

    @Value("${spring.db-connection.hikari.connection-timeout}")
    private int connectionTimeout;

    @Value("${spring.db-connection.hikari.max-life-time}")
    private int maxLifetime;

    @Value("${spring.db-connection.driver-class}")
    private String driverClassName;

    @Value("${spring.db-connection.student.pool-name}")
    private String studentPoolName;

    // Connection String
    @Value("${spring.db-connection.url}")
    private String jdbcUrl;

    @Value("${spring.db-connection.student.username}")
    private String studentUsername;

    @Value("${spring.db-connection.student.password}")
    private String studentPassword;

    @Bean
    public DataSource studentDataSource() {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(studentUsername);
        config.setPassword(studentPassword);
        config.setPoolName(studentPoolName);

        config.setMinimumIdle(2);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTimeout(connectionTimeout);

        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean studentEntityManager() {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(studentDataSource());
        em.setPackagesToScan(new String[] {"ca.bc.gov.educ.api.dataconversion.entity.student"});

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.Oracle12cDialect");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.show_sql", "true");
        em.setJpaPropertyMap(properties);

        em.setPersistenceUnitName("studentPU");

        return em;
    }

    @Bean
    public PlatformTransactionManager studentTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(studentEntityManager().getObject());
        return transactionManager;
    }
}
