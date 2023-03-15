package ca.bc.gov.educ.api.dataconversion.model;

import ca.bc.gov.educ.api.dataconversion.constant.BatchStatusEnum;
import lombok.Data;

import java.util.Date;

@Data
public class BatchJobResponse {
    private Long batchId;
    private Date startTime = new Date(System.currentTimeMillis());
    private String status = BatchStatusEnum.STARTED.name();
    private String jobType;

    private String exception;
}
