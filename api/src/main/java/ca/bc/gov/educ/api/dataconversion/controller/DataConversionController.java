package ca.bc.gov.educ.api.dataconversion.controller;

import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.service.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.PermissionsContants;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_ROOT_MAPPING + EducGradBatchGraduationApiConstants.GRAD_CONVERSION_API_MAPPING)
@CrossOrigin
@EnableResourceServer
public class DataConversionController {
  private static final Logger logger = LoggerFactory.getLogger(DataConversionController.class);

  private final DataConversionService dataConversionService;

  private static final List<Pair<String, String>> REMOVAL_LIST = new ArrayList<>() {{
    add(Pair.of("CLEA", "CLEB"));
    add(Pair.of("CLEA", "CLEBF"));
    add(Pair.of("CLEAF", "CLEB"));
    add(Pair.of("CLEAF", "CLEBF"));
    add(Pair.of("CLEB", "CLEA"));
    add(Pair.of("CLEB", "CLEAF"));
    add(Pair.of("CLEBF", "CLEA"));
    add(Pair.of("CLEBF", "CLEAF"));
  }};

  public DataConversionController(DataConversionService dataConversionService) {
    this.dataConversionService = dataConversionService;
  }

  @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_COURSE_RESTRICTIONS_CONVERSION_JOB)
  @PreAuthorize(PermissionsContants.LOAD_STUDENT_IDS)
  public ResponseEntity<ConversionSummaryDTO> runCourseRestrictionsDataConversionJob(@RequestParam(defaultValue = "false") boolean purge) {
    logger.info("Inside runDataConversionJob");

    ConversionSummaryDTO summary = new ConversionSummaryDTO();
    summary.setTableName("GRAD_COURSE_RESTRICTIONS");

    List<GradCourseRestriction> courseRestrictions;
    try {
      courseRestrictions = dataConversionService.loadInitialRawGradCourseRestrictionsData(purge);
      summary.setReadCount(courseRestrictions.size());
      logger.info("01. Course Restrictions - Initial Raw Data Load is done successfully");
    } catch (Exception e) {
      logger.info("01. Initial Raw Data Loading is failed: " + e.getLocalizedMessage());
      e.printStackTrace();
      summary.setException(e.getLocalizedMessage());
      return ResponseEntity.status(500).body(summary);
    }

    logger.info("02. Convert Course Restrictions started");
    int i = 1;
    try {
      for (GradCourseRestriction c : courseRestrictions) {
        logger.info(" Found courseRestriction[{}] in total {}", i++, summary.getReadCount());
        dataConversionService.convertCourseRestriction(c, summary);
      }

      logger.info("02. Convert Course Restrictions done successfully");
    } catch (Exception e) {
      logger.info("02. Convert Course Restrictions failed: " + e.getLocalizedMessage());
      e.printStackTrace();
      summary.setException(e.getLocalizedMessage());
      return ResponseEntity.status(500).body(summary);
    }

    try {
      REMOVAL_LIST.forEach(c -> dataConversionService.removeGradCourseRestriction(c.getLeft(), c.getRight(), summary));
      logger.info("03. Clean up Data for date conversion is done successfully");
    } catch (Exception e) {
      logger.info("03. Clean up Data for date conversion is failed: " + e.getLocalizedMessage());
      e.printStackTrace();
      summary.setException(e.getLocalizedMessage());
      return ResponseEntity.status(500).body(summary);
    }

    return ResponseEntity.ok(summary);
  }
}
