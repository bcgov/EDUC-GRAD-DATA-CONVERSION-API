package ca.bc.gov.educ.api.dataconversion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.dataconversion.model.AlgorithmResponse;
import ca.bc.gov.educ.api.dataconversion.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ConversionError;
import ca.bc.gov.educ.api.dataconversion.model.GraduationStatus;
import ca.bc.gov.educ.api.dataconversion.rest.RestUtils;

@Service
public class GradAlgorithmService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GradAlgorithmService.class);
	
	private final RestUtils restUtils;

	public GradAlgorithmService(RestUtils restUtils) {
		this.restUtils = restUtils;
	}

	public GraduationStatus processStudent(GraduationStatus item, AlgorithmSummaryDTO summary) {
		LOGGER.info(" Processing  **** PEN: ****" + item.getPen().substring(5));
		summary.setProcessedCount(summary.getProcessedCount() + 1L);
		try {
			String accessToken = summary.getAccessToken();
			AlgorithmResponse algorithmResponse = restUtils.runGradAlgorithm(item.getStudentID(), accessToken);
			return algorithmResponse.getGraduationStatus();
		}catch(Exception e) {
			ConversionError error = new ConversionError();
			error.setPen(item.getPen());
			error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
			summary.getErrors().add(error);
			summary.setProcessedCount(summary.getProcessedCount() - 1L);
			return null;
		}
		
	}

	
}
