package ca.bc.gov.educ.api.dataconversion.config;

import ca.bc.gov.educ.api.dataconversion.model.ResponseObjCache;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestUtilsConfig {

    @Autowired
    EducGradDataConversionApiConstants constants;

    @Bean
    public ResponseObjCache createResponseObjCache() {
        return new ResponseObjCache(constants.getTokenExpiryOffset());
    }

}
