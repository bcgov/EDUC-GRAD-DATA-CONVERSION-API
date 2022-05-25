package ca.bc.gov.educ.api.dataconversion.service.course;

import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.util.DateConversionUtils;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiUtils;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);

    private static final String COURSE_RESTRICTION_ID = "courseRestrictionId";
    private static final String CREATE_USER = "createUser";
    private static final String CREATE_DATE = "createDate";

    private static final String CLEAF_STR = "CLEAF";
    private static final String CLEBF_STR = "CLEBF";
    private static final String CLEA_STR = "CLEA";
    private static final String CLEB_STR = "CLEB";

    private static final String FRAL_STR = "FRAL";
    private static final String FRALP_STR = "FRALP";
    private static final String QFRAL_STR = "QFRAL";
    private static final String ZFRAL_STR = "ZFRAL";
    private static final String IBFAS_STR = "IBFAS";

    private static final String ERR_MSG_FORMAT = "For {} : {}";

    private static final List<Pair<String, String>> IGNORE_LIST = new ArrayList<>();
    static {
        IGNORE_LIST.add(Pair.of(CLEA_STR, CLEB_STR));
        IGNORE_LIST.add(Pair.of(CLEA_STR, CLEBF_STR));
        IGNORE_LIST.add(Pair.of(CLEAF_STR, CLEB_STR));
        IGNORE_LIST.add(Pair.of(CLEAF_STR, CLEBF_STR));
        IGNORE_LIST.add(Pair.of(CLEB_STR, CLEA_STR));
        IGNORE_LIST.add(Pair.of(CLEB_STR, CLEAF_STR));
        IGNORE_LIST.add(Pair.of(CLEBF_STR, CLEA_STR));
        IGNORE_LIST.add(Pair.of(CLEBF_STR, CLEAF_STR));
    }

    private final RestUtils restUtils;

    @Autowired
    public CourseService(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    public CourseRestriction convertCourseRestriction(CourseRestriction courseRestriction, ConversionCourseSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        if (isInvalidData(courseRestriction.getMainCourse(), courseRestriction.getRestrictedCourse())) {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.WARNING);
            error.setItem(courseRestriction.getMainCourse() + " " + courseRestriction.getRestrictedCourse());
            error.setReason("Skip invalid data");
            summary.getErrors().add(error);
            return null;
        }
        boolean isUpdate = false;
        CourseRestriction currentCourseRestriction;
        try {
            currentCourseRestriction = restUtils.getCourseRestriction(
                    courseRestriction.getMainCourse(),
                    courseRestriction.getMainCourseLevel(),
                    courseRestriction.getRestrictedCourse(),
                    courseRestriction.getRestrictedCourseLevel(),
                    summary.getAccessToken());
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.ERROR);
            error.setItem(courseRestriction.getMainCourse() + "/" + courseRestriction.getMainCourseLevel() + ", " +
                    courseRestriction.getRestrictedCourse() + "/" + courseRestriction.getRestrictedCourseLevel());
            error.setReason("GRAD Course API is failed to retrieve!");
            summary.getErrors().add(error);
            logger.error(ERR_MSG_FORMAT, error.getItem(), error.getReason());
            return null;
        }
        if (currentCourseRestriction == null) {
            currentCourseRestriction = new CourseRestriction();
            BeanUtils.copyProperties(courseRestriction, currentCourseRestriction, COURSE_RESTRICTION_ID, CREATE_USER, CREATE_DATE);
        } else {
            isUpdate = true;  // update
        }

        convertCourseRestrictionData(currentCourseRestriction);

        CourseRestriction result;
        try {
            result = restUtils.saveCourseRestriction(currentCourseRestriction, summary.getAccessToken());
            if (isUpdate) {
                summary.setUpdatedCountForCourseRestriction(summary.getUpdatedCountForCourseRestriction() + 1L);
            } else {
                summary.setAddedCountForCourseRestriction(summary.getAddedCountForCourseRestriction() + 1L);
            }
            return result;
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.ERROR);
            error.setItem(courseRestriction.getMainCourse() + "/" + courseRestriction.getMainCourseLevel() + ", " +
                    courseRestriction.getRestrictedCourse() + "/" + courseRestriction.getRestrictedCourseLevel());
            error.setReason("GRAD Course API is failed to save Course Restriction!");
            summary.getErrors().add(error);
            logger.error(ERR_MSG_FORMAT, error.getItem(), error.getReason());
            return null;
        }
    }

    private boolean isInvalidData(String mainCourseCode, String restrictedCourseCode) {
        Pair<String, String> pair = Pair.of(mainCourseCode, restrictedCourseCode);
        return IGNORE_LIST.contains(pair);
    }

    private void convertCourseRestrictionData(CourseRestriction courseRestriction) {
        // data conversion
        if (StringUtils.isNotBlank(courseRestriction.getRestrictionStartDate())) {
            Date start = DateConversionUtils.convertStringToDate(courseRestriction.getRestrictionStartDate());
            if (start != null) {
                courseRestriction.setRestrictionStartDate(EducGradDataConversionApiUtils.formatDate(start));
            }
        }
        if (StringUtils.isNotBlank(courseRestriction.getRestrictionEndDate())) {
            Date end = DateConversionUtils.convertStringToDate(courseRestriction.getRestrictionEndDate());
            if (end != null) {
                courseRestriction.setRestrictionEndDate(EducGradDataConversionApiUtils.formatDate(end));
            }
        }
    }

    public GradCourse convertCourseRequirement(GradCourse courseRequirement, ConversionCourseSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        processEnglish(courseRequirement, summary);
        processSocials(courseRequirement, summary);
        processMath(courseRequirement, summary);
        processScience(courseRequirement, summary);
        processCareerPersonal(courseRequirement, summary);
        processPhysEd(courseRequirement, summary);
        processAppliedSkills(courseRequirement, summary);
        processPortFolio(courseRequirement, summary);
        processConsEd(courseRequirement, summary);
        processFineArts(courseRequirement, summary);
        processCareerLifeConnections(courseRequirement, summary);
        return courseRequirement;
    }

    private void processEnglish(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        // english10
        processEnglish10(gradCourse, summary);
        // english11
        processEnglish11(gradCourse, summary);
        // english12
        processEnglish12(gradCourse, summary);
    }

    private void processEnglish10(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        if (StringUtils.isNotBlank(gradCourse.getEnglish10()) && StringUtils.equals(gradCourse.getEnglish10(), "Y")) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(),
                        "101"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "701"), summary);
            }
            handleFrenchLanguageCourseForEnglish10(gradCourse, summary);
            handleBlankLanguageCourseForEnglish10(gradCourse, summary);
        }
    }

    private void handleFrenchLanguageCourseForEnglish10(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        if (hasFrenchLanguageCourse(gradCourse.getCourseCode(),
                gradCourse.getCourseLevel(), summary.getAccessToken())) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "302"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "815"), summary);
            }
        }
    }

    private void handleBlankLanguageCourseForEnglish10(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        if (hasBlankLanguageCourse(gradCourse.getCourseCode(),
                gradCourse.getCourseLevel(), summary.getAccessToken())) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "400"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "850"), summary);
            }
        }
    }

    private void processEnglish11(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        if (StringUtils.isNotBlank(gradCourse.getEnglish11()) && StringUtils.equals(gradCourse.getEnglish11(), "Y")) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "102"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "702"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "721"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "740"), summary);
            }
            handleFrenchLanguageCourseForEnglish11(gradCourse, summary);
            handleBlankLanguageCourseForEnglish11(gradCourse, summary);
        }
    }

    private void handleFrenchLanguageCourseForEnglish11(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        if (hasFrenchLanguageCourse(gradCourse.getCourseCode(), gradCourse.getCourseLevel(), summary.getAccessToken())) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "301"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "816"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "818"), summary);
            }
        }

    }

    private void handleBlankLanguageCourseForEnglish11(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        if (hasBlankLanguageCourse(gradCourse.getCourseCode(), gradCourse.getCourseLevel(), summary.getAccessToken())) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "401"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "851"), summary);
            }
        }
    }

    private void processEnglish12(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        if (StringUtils.isNotBlank(gradCourse.getEnglish12()) && StringUtils.equals(gradCourse.getEnglish12(), "Y")) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "103"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "703"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1950")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "500"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "722"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "741"), summary);
            }
            handleFrenchLanguageCourseForEnglish12(gradCourse, summary);
            handleBlankLanguageCourseForEnglish12(gradCourse, summary);
        }
    }

    private void handleFrenchLanguageCourseForEnglish12(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        if (hasFrenchLanguageCourse(gradCourse.getCourseCode(), gradCourse.getCourseLevel(), summary.getAccessToken())) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "300"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "817"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "819"), summary);
            }
        }
    }

    private void handleBlankLanguageCourseForEnglish12(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        if (hasBlankLanguageCourse(gradCourse.getCourseCode(), gradCourse.getCourseLevel(), summary.getAccessToken())) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "402"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "852"), summary);
            }
        }
    }

    private void processSocials(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        // socials10
        if (StringUtils.isNotBlank(gradCourse.getSocials10()) && StringUtils.equals(gradCourse.getSocials10(), "Y")) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "104"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "704"), summary);
            }
        }
        // socials
        if (StringUtils.isNotBlank(gradCourse.getSocials()) && StringUtils.equals(gradCourse.getSocials(), "Y")) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "105"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "705"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1950")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "502"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "723"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "742"), summary);
            }
        }
    }

    private void processMath(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        // math10
        if (StringUtils.isNotBlank(gradCourse.getMath10()) && StringUtils.equals(gradCourse.getMath10(), "Y")) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "106"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "706"), summary);
            }
        }
        // math
        if (StringUtils.isNotBlank(gradCourse.getMath()) && StringUtils.equals(gradCourse.getMath(), "Y")) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "107"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "707"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1950")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "501"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "724"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "743"), summary);
            }
        }
    }

    private void processScience(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        // science10
        if (StringUtils.isNotBlank(gradCourse.getScience10()) && StringUtils.equals(gradCourse.getScience10(), "Y")) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "108"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "708"), summary);
            }
        }
        // science
        if (StringUtils.isNotBlank(gradCourse.getScience()) && StringUtils.equals(gradCourse.getScience(), "Y")) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "109"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "709"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "725"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "744"), summary);
            }
        }
    }

    private void processCareerPersonal(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        // careerPersonal10
        if (isCareerPersonal10(gradCourse)) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "112"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "710"), summary);
            }
        }
        // careerPersonal11
        if (isCareerPersonal11(gradCourse) && StringUtils.equals(gradCourse.getGradReqtYear(), "1996")) {
            createCourseRequirement(populate(gradCourse.getCourseCode(),
                gradCourse.getCourseLevel(), "728"), summary);
        }
        // careerPersonal12
        if (isCareerPersonal12(gradCourse) && StringUtils.equals(gradCourse.getGradReqtYear(), "1996")) {
            createCourseRequirement(populate(gradCourse.getCourseCode(),
                gradCourse.getCourseLevel(), "729"), summary);
        }
    }

    private boolean isCareerPersonal10(GradCourse gradCourse) {
        return StringUtils.isNotBlank(gradCourse.getCareerPersonal10()) && StringUtils.equals(gradCourse.getCareerPersonal10(), "Y");
    }

    private boolean isCareerPersonal11(GradCourse gradCourse) {
        return StringUtils.isNotBlank(gradCourse.getCareerPersonal11()) && StringUtils.equals(gradCourse.getCareerPersonal11(), "Y");
    }

    private boolean isCareerPersonal12(GradCourse gradCourse) {
        return StringUtils.isNotBlank(gradCourse.getCareerPersonal12()) && StringUtils.equals(gradCourse.getCareerPersonal12(), "Y");
    }

    private void processPhysEd(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        // physEd10
        if (StringUtils.isNotBlank(gradCourse.getPhysEd10()) && StringUtils.equals(gradCourse.getPhysEd10(), "Y")) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "110"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "711"), summary);
            }
        }
    }

    private void processAppliedSkills(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        // appliedSkills
        if (isAppliedSkills(gradCourse)) {
            if (StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "111"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(gradCourse.getCourseCode(),
                    gradCourse.getCourseLevel(), "712"), summary);
            } else if (StringUtils.equals(gradCourse.getGradReqtYear(), "1996")) {
                if (isFineArts(gradCourse)) {
                    createCourseRequirement(populate(gradCourse.getCourseCode(),
                        gradCourse.getCourseLevel(), "732"), summary);
                } else if (!isInvalidCourseForRule727(gradCourse.getCourseCode(), gradCourse.getCourseLevel())) {
                    createCourseRequirement(populate(gradCourse.getCourseCode(),
                        gradCourse.getCourseLevel(), "727"), summary);
                }
            }
        }
    }

    private void processPortFolio(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        // portfolio
        if (StringUtils.isNotBlank(gradCourse.getPortfolio()) && StringUtils.equals(gradCourse.getPortfolio(), "Y")
            && StringUtils.equals(gradCourse.getGradReqtYear(), "2004")) {
            createCourseRequirement(populate(gradCourse.getCourseCode(),
                gradCourse.getCourseLevel(), "713"), summary);
        }
    }

    private void processConsEd(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        // consEd
        if (StringUtils.isNotBlank(gradCourse.getConsEd()) && StringUtils.equals(gradCourse.getConsEd(), "Y")
            && StringUtils.equals(gradCourse.getGradReqtYear(), "1986")) {
            createCourseRequirement(populate(gradCourse.getCourseCode(),
                gradCourse.getCourseLevel(), "745"), summary);
        }
    }

    private void processFineArts(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        // fineArts
        if (isFineArts(gradCourse)
            && StringUtils.equals(gradCourse.getGradReqtYear(), "1996") && !isAppliedSkills(gradCourse)) {
            createCourseRequirement(populate(gradCourse.getCourseCode(),
                gradCourse.getCourseLevel(), "726"), summary);
        }
    }

    private boolean isFineArts(GradCourse gradCourse) {
        return StringUtils.isNotBlank(gradCourse.getFineArts()) && StringUtils.equals(gradCourse.getFineArts(), "Y");
    }

    private boolean isAppliedSkills(GradCourse gradCourse) {
        return StringUtils.isNotBlank(gradCourse.getAppliedSkills()) && StringUtils.equals(gradCourse.getAppliedSkills(), "Y");
    }

    private void processCareerLifeConnections(GradCourse gradCourse, ConversionCourseSummaryDTO summary) {
        // careerLifeConnections
        if (isCareerLifeConnections(gradCourse) && StringUtils.equals(gradCourse.getGradReqtYear(), "2018")) {
            createCourseRequirement(populate(gradCourse.getCourseCode(),
                gradCourse.getCourseLevel(), "113"), summary);
        }
    }

    private boolean isCareerLifeConnections(GradCourse gradCourse) {
        return StringUtils.isNotBlank(gradCourse.getCareerLifeConnections()) && StringUtils.equals(gradCourse.getCareerLifeConnections(), "Y");
    }

    public void createCourseRequirementsForFrenchImmersion(ConversionCourseSummaryDTO summary) {
        // FRAL 12
        createCourseRequirement(populate(FRAL_STR, "12", "200"), summary);
        createCourseRequirement(populate(FRAL_STR, "12", "900"), summary);
        createCourseRequirement(populate(FRAL_STR, "12", "819"), summary);
        createCourseRequirement(populate(FRAL_STR, "12", "910"), summary);
        createCourseRequirement(populate(FRAL_STR, "12", "915"), summary);

        // FRALP 12
        createCourseRequirement(populate(FRALP_STR, "12", "910"), summary);
        createCourseRequirement(populate(FRALP_STR, "12", "915"), summary);

        // QFRAL 12
        createCourseRequirement(populate(QFRAL_STR, "12", "200"), summary);
        createCourseRequirement(populate(QFRAL_STR, "12", "900"), summary);
        createCourseRequirement(populate(QFRAL_STR, "12", "819"), summary);
        createCourseRequirement(populate(QFRAL_STR, "12", "910"), summary);
        createCourseRequirement(populate(QFRAL_STR, "12", "915"), summary);

        // QFRAP 12
        createCourseRequirement(populate("QFRAP", "12", "910"), summary);
        createCourseRequirement(populate("QFRAP", "12", "915"), summary);

        // ZFRAL 12
        createCourseRequirement(populate(ZFRAL_STR, "12", "819"), summary);
        createCourseRequirement(populate(ZFRAL_STR, "12", "910"), summary);
        createCourseRequirement(populate(ZFRAL_STR, "12", "915"), summary);
 
        // IBFAS 12
        createCourseRequirement(populate(IBFAS_STR, "12", "200"), summary);
        createCourseRequirement(populate(IBFAS_STR, "12", "900"), summary);

        // IBFAH 12
        createCourseRequirement(populate("IBFAH", "12", "200"), summary);
        createCourseRequirement(populate("IBFAH", "12", "900"), summary);

        // FRAL 11
        createCourseRequirement(populate(FRAL_STR, "11", "201"), summary);
        createCourseRequirement(populate(FRAL_STR, "11", "901"), summary);
        createCourseRequirement(populate(FRAL_STR, "11", "818"), summary);
        createCourseRequirement(populate(FRAL_STR, "11", "911"), summary);
        createCourseRequirement(populate(FRAL_STR, "11", "916"), summary);

        // IBFAS 11
        createCourseRequirement(populate(IBFAS_STR, "11", "201"), summary);
        createCourseRequirement(populate(IBFAS_STR, "11", "901"), summary);
        createCourseRequirement(populate(IBFAS_STR, "11", "911"), summary);
        createCourseRequirement(populate(IBFAS_STR, "11", "916"), summary);

        // FRALP 11
        createCourseRequirement(populate(FRALP_STR, "11", "201"), summary);
        createCourseRequirement(populate(FRALP_STR, "11", "901"), summary);
        createCourseRequirement(populate(FRALP_STR, "11", "911"), summary);
        createCourseRequirement(populate(FRALP_STR, "11", "916"), summary);

        // FRAL 10
        createCourseRequirement(populate(FRAL_STR, "10", "202"), summary);
        createCourseRequirement(populate(FRAL_STR, "10", "902"), summary);

        // FRALP 10
        createCourseRequirement(populate(FRALP_STR, "10", "202"), summary);
        createCourseRequirement(populate(FRALP_STR, "10", "902"), summary);

        // IBFRS 12A
        createCourseRequirement(populate("IBFRS", "12A", "200"), summary);

        // IBFRH 12A
        createCourseRequirement(populate("IBFRH", "12A", "200"), summary);

        // IBFNS 11
        createCourseRequirement(populate("IBFNS", "11", "201"), summary);
        createCourseRequirement(populate("IBFNS", "11", "901"), summary);

        // IBFRS 11
        createCourseRequirement(populate("IBFRS", "11", "201"), summary);

        // IBFRH 11
        createCourseRequirement(populate("IBFRH", "11", "201"), summary);

        // SPLGF 11
        createCourseRequirement(populate("SPLGF", "11", "201"), summary);

        // LCFF 11
        createCourseRequirement(populate("LCFF", "11", "201"), summary);

        // NMDF 11
        createCourseRequirement(populate("NMDF", "11", "201"), summary);

        // LTSTF 11
        createCourseRequirement(populate("LTSTF", "11", "201"), summary);

        // Adult Work Experience
        createCourseRequirement(populate("CPWE", "12", "506"), summary);
        createCourseRequirement(populate("SSA", "12A", "506"), summary);
        createCourseRequirement(populate("SSA", "12B", "506"), summary);
        createCourseRequirement(populate("WEX", "12A", "506"), summary);
        createCourseRequirement(populate("WEX", "12B", "506"), summary);

        createCourseRequirement(populate("SSA", "12A", "600"), summary);
        createCourseRequirement(populate("SSA", "12B", "600"), summary);
        createCourseRequirement(populate("SSA", "11A", "600"), summary);
        createCourseRequirement(populate("SSA", "11B", "600"), summary);

        createCourseRequirement(populate("WEX", "12A", "600"), summary);
        createCourseRequirement(populate("WEX", "12B", "600"), summary);

        createCourseRequirement(populate("WRK", "12A", "600"), summary);
        createCourseRequirement(populate("WRK", "12B", "600"), summary);
        createCourseRequirement(populate("WRK", "11A", "600"), summary);
        createCourseRequirement(populate("WRK", "11B", "600"), summary);
    }

    private CourseRequirement populate(String courseCode, String courseLevel, String courseRequirementCode) {
        CourseRequirement courseRequirement = new CourseRequirement();
        courseRequirement.setCourseCode(courseCode.trim());
        courseRequirement.setCourseLevel(StringUtils.isBlank(courseLevel)? " " :  courseLevel.trim());

        CourseRequirementCodeDTO ruleCode = new CourseRequirementCodeDTO();
        ruleCode.setCourseRequirementCode(courseRequirementCode);

        courseRequirement.setRuleCode(ruleCode);
        return courseRequirement;
    }

    private CourseRequirement createCourseRequirement(CourseRequirement courseRequirement, ConversionCourseSummaryDTO summary) {
        logger.info(" Create CourseRequirement: course [{} / {}], rule [{}]",
                courseRequirement.getCourseCode(), courseRequirement.getCourseLevel(),
                courseRequirement.getRuleCode() != null? courseRequirement.getRuleCode().getCourseRequirementCode() : "");

        boolean isUpdate;
        try {
            if (StringUtils.isBlank(courseRequirement.getCourseLevel())) {
                logger.info("course level [{}] is found for {}", courseRequirement.getCourseLevel(), courseRequirement.getCourseCode());
                courseRequirement.setCourseLevel(" ");
            }
            isUpdate = restUtils.checkCourseRequirementExists(
                    courseRequirement.getCourseCode(),
                    courseRequirement.getCourseLevel(),
                    courseRequirement.getRuleCode().getCourseRequirementCode(),
                    summary.getAccessToken());
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.ERROR);
            error.setItem(courseRequirement.getCourseCode() + "/" + courseRequirement.getCourseLevel() + ", rule[" + courseRequirement.getRuleCode().getCourseRequirementCode() + "]");
            error.setReason("GRAD Course API is failed to check Course Requirement exits!");
            summary.getErrors().add(error);
            logger.error(ERR_MSG_FORMAT, error.getItem(), error.getReason());
            return null;
        }

        try {
            CourseRequirement result = restUtils.saveCourseRequirement(courseRequirement, summary.getAccessToken());
            if (isUpdate) {
                summary.setUpdatedCountForCourseRequirement(summary.getUpdatedCountForCourseRequirement() + 1L);
            } else {
                summary.setAddedCountForCourseRequirement(summary.getAddedCountForCourseRequirement() + 1L);
            }
            return result;
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.ERROR);
            error.setItem(courseRequirement.getCourseCode() + "/" + courseRequirement.getCourseLevel() + ", rule[" + courseRequirement.getRuleCode().getCourseRequirementCode() + "]");
            error.setReason("GRAD Course API is failed to save Course Requirement!");
            summary.getErrors().add(error);
            logger.error(ERR_MSG_FORMAT, error.getItem(), error.getReason());
            return null;
        }
    }

    private boolean isInvalidCourseForRule727(String courseCode, String courseLevel) {
         return (StringUtils.equals(courseCode.trim(), "AC") && StringUtils.equals(courseLevel, "11 "))
            || (StringUtils.equals(courseCode.trim(), "ACC") && StringUtils.equals(courseLevel, "12 "))
            || (StringUtils.equals(courseCode.trim(), "COP") && StringUtils.equals(courseLevel, "11 "))
            || (StringUtils.equals(courseCode.trim(), "COP") && StringUtils.equals(courseLevel, "12 "))
            || (StringUtils.equals(courseCode.trim(), "FA") && StringUtils.equals(courseLevel, "12 "));
    }

    public boolean hasFrenchLanguageCourse(String courseCode, String courseLevel, String accessToken) {
        return this.restUtils.checkFrenchLanguageCourse(courseCode.trim(),
                StringUtils.isBlank(courseLevel)? " " : courseLevel.trim(), accessToken);
    }

    public boolean hasBlankLanguageCourse(String courseCode, String courseLevel, String accessToken) {
        return this.restUtils.checkBlankLanguageCourse(courseCode.trim(),
                StringUtils.isBlank(courseLevel)? " " : courseLevel.trim(), accessToken);
    }

    public boolean isFrenchImmersionCourse(String pen, String courseLevel, String accessToken) {
        String courseLevelParam = StringUtils.isBlank(courseLevel)? " " : courseLevel.trim();
        return this.restUtils.checkFrenchImmersionCourse(pen, courseLevelParam, accessToken);
    }

    public boolean isFrenchImmersionCourseForEN(String pen, String courseLevel, String accessToken) {
        String courseLevelParam = StringUtils.isBlank(courseLevel)? " " : courseLevel.trim();
        return this.restUtils.checkFrenchImmersionCourseForEN(pen, courseLevelParam, accessToken);
    }

    public List<StudentCourse> getStudentCourses(String pen, String accessToken) {
        return this.restUtils.getStudentCoursesByPen(pen, accessToken);
    }

}
