package ca.bc.gov.educ.api.dataconversion.writer;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

public class StudentPartitionWriter implements ItemWriter<ConvGradStudent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentPartitionWriter.class);

    @Autowired
    private RestUtils restUtils;

    @Value("#{stepExecutionContext['summary']}")
    private ConversionStudentSummaryDTO summaryDTO;

    @Override
    public void write(Chunk<? extends ConvGradStudent> chunk) throws Exception {
        if (!chunk.isEmpty()) {
            ConvGradStudent student = chunk.getItems().get(0);
            fetchAccessToken();
            saveConversionStatus(student, summaryDTO.getErrors());
            LOGGER.info("Processed student[{}] - PEN: {} in total {}", summaryDTO.getProcessedCount(), student.getPen(), summaryDTO.getReadCount());
        }
    }

    private void saveConversionStatus(ConvGradStudent student, List<ConversionAlert> errors) {
        TraxStudentNo traxStudentNo = new TraxStudentNo();
        traxStudentNo.setStudNo(student.getPen());

        List<ConversionAlert> list = errors.stream().filter(a -> StringUtils.equals(a.getItem(), traxStudentNo.getStudNo())).collect(Collectors.toList());
        ConversionResultType status = student.getResult();
        if (status != ConversionResultType.SUCCESS || !list.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            for (ConversionAlert a : list) {
                if (a.getLevel() == ConversionAlert.AlertLevelEnum.WARNING) {
                    status = ConversionResultType.WARNING;
                    msg.append(a.getLevel() + "-" + a.getReason() + " ");
                } else if (a.getLevel() == ConversionAlert.AlertLevelEnum.ERROR) {
                    status = ConversionResultType.FAILURE;
                    msg.append(a.getLevel() + "-" + a.getReason() + " ");
                }
            }
            if (msg.length() > 0) {
                traxStudentNo.setReason(msg.length() > 500? msg.substring(0,500) : msg.toString());
            }
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1L);
        }
        traxStudentNo.setStatus(status.toString());
        restUtils.saveTraxStudentNo(traxStudentNo, summaryDTO.getAccessToken());
        summaryDTO.getErrors().clear();
    }

    private void fetchAccessToken() {
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            summaryDTO.setAccessToken(res.getAccess_token());
            LOGGER.debug("Setting the new access token in summaryDTO.");
        }
    }
}
