package ca.bc.gov.educ.api.dataconversion.config;

import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


import javax.sql.DataSource;

@Configuration
@Profile("!test")
@Component
public class BatchDataSourceConfig extends DefaultBatchConfigurer {

    @Autowired
    public BatchDataSourceConfig(@Qualifier("batchDataSource") DataSource batchDataSource) {
        super(batchDataSource);
    }
}
