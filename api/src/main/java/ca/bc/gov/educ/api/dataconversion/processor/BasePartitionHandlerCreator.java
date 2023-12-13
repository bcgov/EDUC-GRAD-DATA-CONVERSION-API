package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BasePartitionHandlerCreator implements Tasklet {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePartitionHandlerCreator.class);

    @Autowired
    RestUtils restUtils;

    protected abstract void aggregate(StepContribution contribution, String tableName, String summaryContextName);

    protected String fetchAccessToken() {
        return restUtils.getTokenResponseObject().getAccess_token();
    }


}