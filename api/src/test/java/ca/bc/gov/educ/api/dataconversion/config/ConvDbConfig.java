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
@Profile("test")
@EnableJpaRepositories(
        basePackages = {
                "ca.bc.gov.educ.api.dataconversion.repository.conv"
        },
        entityManagerFactoryRef = "convEntityManager",
        transactionManagerRef = "convTransactionManager"
)
@EnableTransactionManagement
public class ConvDbConfig {
    // Hikari Pool
    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.connection-timeout}")
    private int connectionTimeout;

    @Value("${spring.datasource.hikari.max-life-time}")
    private int maxLifetime;

    @Value("${spring.datasource.driver-class}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.pool-name}")
    private String poolName;

    // Connection String
    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Primary
    @Bean
    public DataSource convDataSource() {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        config.setPoolName(poolName);

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
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.show_sql", "true");
        em.setJpaPropertyMap(properties);

        em.setPersistenceUnitName("coursePU");

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
