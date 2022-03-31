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
                "ca.bc.gov.educ.api.dataconversion.repository.batch"
        },
        entityManagerFactoryRef = "batchEntityManager",
        transactionManagerRef = "batchTransactionManager"
)
@EnableTransactionManagement
public class BatchDbConfig {
    // Hikari Pool
    @Value("${batch.partitions.number}")
    private int numberOfPartitions;

    @Value("${spring.db-connection.hikari.connection-timeout}")
    private int connectionTimeout;

    @Value("${spring.db-connection.hikari.max-lifetime}")
    private int maxLifetime;

    @Value("${spring.db-connection.hikari.keepalive-time}")
    private int keepAliveTime;

    @Value("${spring.db-connection.driver-class}")
    private String driverClassName;

    @Value("${spring.db-connection.batch.pool-name}")
    private String batchPoolName;

    // Connection String
    @Value("${spring.db-connection.url}")
    private String jdbcUrl;

    @Value("${spring.db-connection.batch.username}")
    private String batchUsername;

    @Value("${spring.db-connection.batch.password}")
    private String batchPassword;

    @Bean
    public DataSource batchDataSource() {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(batchUsername);
        config.setPassword(batchPassword);
        config.setPoolName(batchPoolName);

        config.setMinimumIdle(2);
        config.setMaximumPoolSize(numberOfPartitions);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTimeout(connectionTimeout);
        config.setKeepaliveTime(keepAliveTime);
        config.addDataSourceProperty("socketTimeout", maxLifetime);

        System.out.println("==> BATCH DB : POOL SIZE = " + numberOfPartitions);

        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean batchEntityManager() {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(batchDataSource());
        em.setPackagesToScan(new String[] {"ca.bc.gov.educ.api.dataconversion.entity.batch"});

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.Oracle12cDialect");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.show_sql", "false");
        em.setJpaPropertyMap(properties);

        em.setPersistenceUnitName("batchPU");

        return em;
    }

    @Bean
    public PlatformTransactionManager batchTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(batchEntityManager().getObject());
        return transactionManager;
    }
}
