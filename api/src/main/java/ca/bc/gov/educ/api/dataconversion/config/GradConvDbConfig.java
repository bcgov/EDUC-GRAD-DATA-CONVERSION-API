package ca.bc.gov.educ.api.dataconversion.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
            "ca.bc.gov.educ.api.dataconversion.repository.conv"
        },
        entityManagerFactoryRef = "convEntityManager",
        transactionManagerRef = "convTransactionManager"
)
@EnableTransactionManagement
public class GradConvDbConfig {
    // Hikari Pool
    @Value("${spring.db-connection.hikari.maximum-pool-size}")
    private int maxPoolSize;

    @Value("${spring.db-connection.hikari.connection-timeout}")
    private int connectionTimeout;

    @Value("${spring.db-connection.hikari.max-life-time}")
    private int maxLifetime;

    @Value("${spring.db-connection.driver-class}")
    private String driverClassName;

    @Value("${spring.db-connection.conv.pool-name}")
    private String convPoolName;

    // Connection String
    @Value("${spring.db-connection.url}")
    private String jdbcUrl;

    @Value("${spring.db-connection.conv.username}")
    private String convUsername;

    @Value("${spring.db-connection.conv.password}")
    private String convPassword;

    @Primary
    @Bean
    public DataSource convDataSource() {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(convUsername);
        config.setPassword(convPassword);
        config.setPoolName(convPoolName);

        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTimeout(connectionTimeout);

        return new HikariDataSource(config);
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean convEntityManager() {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(convDataSource());
        em.setPackagesToScan(new String[] {"ca.bc.gov.educ.api.dataconversion.entity.conv"});

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.Oracle12cDialect");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.show_sql", "true");

        em.setPersistenceUnitName("convPU");

        return em;
    }

    @Primary
    @Bean
    public PlatformTransactionManager convTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(convEntityManager().getObject());
        return transactionManager;
    }
}
