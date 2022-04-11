package ca.bc.gov.educ.api.dataconversion.service.course;

import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseKey;
import ca.bc.gov.educ.api.dataconversion.model.*;
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

    private static final String COURSE_API_NAME = "GRAD Course API";

    private static final List<Pair<String, String>> IGNORE_LIST = new ArrayList<>() {{
        add(Pair.of("CLEA", "CLEB"));
        add(Pair.of("CLEA", "CLEBF"));
        add(Pair.of("CLEAF", "CLEB"));
        add(Pair.of("CLEAF", "CLEBF"));
        add(Pair.of("CLEB", "CLEA"));
        add(Pair.of("CLEB", "CLEAF"));
        add(Pair.of("CLEBF", "CLEA"));
        add(Pair.of("CLEBF", "CLEAF"));
    }};

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
            return null;
        }
        if (currentCourseRestriction == null) {
            currentCourseRestriction = new CourseRestriction();
            BeanUtils.copyProperties(courseRestriction, currentCourseRestriction);
        } else {
            isUpdate = true;  // update
        }

        CourseRestriction result;
        try {
            result = restUtils.saveCourseRestriction(courseRestriction, summary.getAccessToken());
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
            return null;
        }
    }

    private boolean isInvalidData(String mainCourseCode, String restrictedCourseCode) {
        Pair<String, String> pair = Pair.of(mainCourseCode, restrictedCourseCode);
        return IGNORE_LIST.contains(pair);
    }

//    private void convertCourseRestrictionData(CourseRestriction courseRestriction, CourseRestrictionEntity courseRestrictionEntity) {
//        if (courseRestrictionEntity.getCourseRestrictionId() == null) {
//            courseRestrictionEntity.setCourseRestrictionId(UUID.randomUUID());
//        }
//        courseRestrictionEntity.setMainCourse(courseRestriction.getMainCourse());
//        courseRestrictionEntity.setMainCourseLevel(courseRestriction.getMainCourseLevel() == null? " " :  courseRestriction.getMainCourseLevel());
//        courseRestrictionEntity.setRestrictedCourse(courseRestriction.getRestrictedCourse());
//        courseRestrictionEntity.setRestrictedCourseLevel(courseRestriction.getRestrictedCourseLevel() == null? " " : courseRestriction.getRestrictedCourseLevel());
//        // data conversion
//        if (StringUtils.isNotBlank(courseRestriction.getRestrictionStartDate())) {
//            Date start = DateConversionUtils.convertStringToDate(courseRestriction.getRestrictionStartDate());
//            if (start != null) {
//                courseRestrictionEntity.setRestrictionStartDate(start);
//            }
//        }
//        if (StringUtils.isNotBlank(courseRestriction.getRestrictionEndDate())) {
//            Date end = DateConversionUtils.convertStringToDate(courseRestriction.getRestrictionEndDate());
//            if (end != null) {
//                courseRestrictionEntity.setRestrictionEndDate(end);
//            }
//        }
//    }

    public GraduationCourseEntity convertCourseRequirement(GraduationCourseEntity courseRequirement, ConversionCourseSummaryDTO summary) {
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

    private void processEnglish(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // english10
        if (StringUtils.isNotBlank(graduationCourseEntity.getEnglish10()) && StringUtils.equals(graduationCourseEntity.getEnglish10(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "101"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "701"), summary);
            }
            if (hasFrenchLanguageCourse(graduationCourseEntity.getGraduationCourseKey(), summary.getAccessToken())) {
                if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "302"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "815"), summary);
                }
            }
            if (hasBlankLanguageCourse(graduationCourseEntity.getGraduationCourseKey(), summary.getAccessToken())) {
                if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "400"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "850"), summary);
                }
            }
        }
        // english11
        if (StringUtils.isNotBlank(graduationCourseEntity.getEnglish11()) && StringUtils.equals(graduationCourseEntity.getEnglish11(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "102"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "702"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "721"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "740"), summary);
            }
            if (hasFrenchLanguageCourse(graduationCourseEntity.getGraduationCourseKey(), summary.getAccessToken())) {
                if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "301"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "816"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "818"), summary);
                }
            }
            if (hasBlankLanguageCourse(graduationCourseEntity.getGraduationCourseKey(), summary.getAccessToken())) {
                if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "401"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "851"), summary);
                }
            }
        }
        // english12
        if (StringUtils.isNotBlank(graduationCourseEntity.getEnglish12()) && StringUtils.equals(graduationCourseEntity.getEnglish12(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "103"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "703"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1950")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "500"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "722"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "741"), summary);
            }
            if (hasFrenchLanguageCourse(graduationCourseEntity.getGraduationCourseKey(), summary.getAccessToken())) {
                if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "300"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "817"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "819"), summary);
                }
            }
            if (hasBlankLanguageCourse(graduationCourseEntity.getGraduationCourseKey(), summary.getAccessToken())) {
                if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "402"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "852"), summary);
                }
            }
        }
    }

    private void processSocials(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // socials10
        if (StringUtils.isNotBlank(graduationCourseEntity.getSocials10()) && StringUtils.equals(graduationCourseEntity.getSocials10(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "104"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "704"), summary);
            }
        }
        // socials
        if (StringUtils.isNotBlank(graduationCourseEntity.getSocials()) && StringUtils.equals(graduationCourseEntity.getSocials(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "105"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "705"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1950")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "502"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "723"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "742"), summary);
            }
        }
    }

    private void processMath(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // math10
        if (StringUtils.isNotBlank(graduationCourseEntity.getMath10()) && StringUtils.equals(graduationCourseEntity.getMath10(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "106"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "706"), summary);
            }
        }
        // math
        if (StringUtils.isNotBlank(graduationCourseEntity.getMath()) && StringUtils.equals(graduationCourseEntity.getMath(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "107"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "707"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1950")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "501"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "724"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "743"), summary);
            }
        }
    }

    private void processScience(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // science10
        if (StringUtils.isNotBlank(graduationCourseEntity.getScience10()) && StringUtils.equals(graduationCourseEntity.getScience10(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "108"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "708"), summary);
            }
        }
        // science
        if (StringUtils.isNotBlank(graduationCourseEntity.getScience()) && StringUtils.equals(graduationCourseEntity.getScience(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "109"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "709"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "725"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "744"), summary);
            }
        }
    }

    private void processCareerPersonal(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // careerPersonal10
        if (StringUtils.isNotBlank(graduationCourseEntity.getCareerPersonal10()) && StringUtils.equals(graduationCourseEntity.getCareerPersonal10(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "112"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "710"), summary);
            }
        }
        // careerPersonal11
        if (StringUtils.isNotBlank(graduationCourseEntity.getCareerPersonal11()) && StringUtils.equals(graduationCourseEntity.getCareerPersonal11(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "728"), summary);
            }
        }
        // careerPersonal12
        if (StringUtils.isNotBlank(graduationCourseEntity.getCareerPersonal12()) && StringUtils.equals(graduationCourseEntity.getCareerPersonal12(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "729"), summary);
            }
        }
    }

    private void processPhysEd(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // physEd10
        if (StringUtils.isNotBlank(graduationCourseEntity.getPhysEd10()) && StringUtils.equals(graduationCourseEntity.getPhysEd10(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "110"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "711"), summary);
            }
        }
    }

    private void processAppliedSkills(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // appliedSkills
        if (StringUtils.isNotBlank(graduationCourseEntity.getAppliedSkills()) && StringUtils.equals(graduationCourseEntity.getAppliedSkills(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "111"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "712"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                if (StringUtils.isNotBlank(graduationCourseEntity.getFineArts()) && StringUtils.equals(graduationCourseEntity.getFineArts(), "Y")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "732"), summary);
                } else if (isValidCourseForRule727(graduationCourseEntity.getGraduationCourseKey())) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "727"), summary);
                }
            }
        }
    }

    private void processPortFolio(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // portfolio
        if (StringUtils.isNotBlank(graduationCourseEntity.getPortfolio()) && StringUtils.equals(graduationCourseEntity.getPortfolio(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "713"), summary);
            }
        }
    }

    private void processConsEd(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // consEd
        if (StringUtils.isNotBlank(graduationCourseEntity.getConsEd()) && StringUtils.equals(graduationCourseEntity.getConsEd(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "745"), summary);
            }
        }
    }

    private void processFineArts(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // fineArts
        if (StringUtils.isNotBlank(graduationCourseEntity.getFineArts()) && StringUtils.equals(graduationCourseEntity.getFineArts(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                if (StringUtils.isBlank(graduationCourseEntity.getAppliedSkills()) || !StringUtils.equals(graduationCourseEntity.getAppliedSkills(), "Y")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "726"), summary);
                }
            }
        }
    }

    private void processCareerLifeConnections(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // careerLifeConnections
        if (StringUtils.isNotBlank(graduationCourseEntity.getCareerLifeConnections()) && StringUtils.equals(graduationCourseEntity.getCareerLifeConnections(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "113"), summary);
            }
        }
    }

    public void createCourseRequirementsForFrenchImmersion(ConversionCourseSummaryDTO summary) {
        // FRAL 12
        createCourseRequirement(populate("FRAL", "12", "200"), summary);
        createCourseRequirement(populate("FRAL", "12", "900"), summary);
        createCourseRequirement(populate("FRAL", "12", "819"), summary);
        createCourseRequirement(populate("FRAL", "12", "910"), summary);
        createCourseRequirement(populate("FRAL", "12", "915"), summary);

        // FRALP 12
        createCourseRequirement(populate("FRALP", "12", "910"), summary);
        createCourseRequirement(populate("FRALP", "12", "915"), summary);

        // QFRAL 12
        createCourseRequirement(populate("QFRAL", "12", "200"), summary);
        createCourseRequirement(populate("QFRAL", "12", "900"), summary);
        createCourseRequirement(populate("QFRAL", "12", "819"), summary);
        createCourseRequirement(populate("QFRAL", "12", "910"), summary);
        createCourseRequirement(populate("QFRAL", "12", "915"), summary);

        // QFRALP 12
        createCourseRequirement(populate("QFRALP", "12", "910"), summary);
        createCourseRequirement(populate("QFRALP", "12", "915"), summary);

        // ZFRAL 12
        createCourseRequirement(populate("ZFRAL", "12", "819"), summary);
        createCourseRequirement(populate("ZFRAL", "12", "910"), summary);
        createCourseRequirement(populate("ZFRAL", "12", "915"), summary);

        // IBFAS 12
        createCourseRequirement(populate("IBFAS", "12", "200"), summary);
        createCourseRequirement(populate("IBFAS", "12", "900"), summary);

        // IBFAH 12
        createCourseRequirement(populate("IBFAH", "12", "200"), summary);
        createCourseRequirement(populate("IBFAH", "12", "900"), summary);

        // FRAL 11
        createCourseRequirement(populate("FRAL", "11", "201"), summary);
        createCourseRequirement(populate("FRAL", "11", "901"), summary);
        createCourseRequirement(populate("FRAL", "11", "818"), summary);
        createCourseRequirement(populate("FRAL", "11", "911"), summary);
        createCourseRequirement(populate("FRAL", "11", "916"), summary);

        // IBFAS 11
        createCourseRequirement(populate("IBFAS", "11", "201"), summary);
        createCourseRequirement(populate("IBFAS", "11", "901"), summary);
        createCourseRequirement(populate("IBFAS", "11", "911"), summary);
        createCourseRequirement(populate("IBFAS", "11", "916"), summary);

        // FRALP 11
        createCourseRequirement(populate("FRALP", "11", "201"), summary);
        createCourseRequirement(populate("FRALP", "11", "901"), summary);
        createCourseRequirement(populate("FRALP", "11", "911"), summary);
        createCourseRequirement(populate("FRALP", "11", "916"), summary);

        // FRAL 10
        createCourseRequirement(populate("FRAL", "10", "202"), summary);
        createCourseRequirement(populate("FRAL", "10", "902"), summary);

        // FRALP 10
        createCourseRequirement(populate("FRALP", "10", "202"), summary);
        createCourseRequirement(populate("FRALP", "10", "902"), summary);

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

    private CourseRequirement populate(GraduationCourseKey key, String courseRequirementCode) {
        return populate(key.getCourseCode(), key.getCourseLevel(), courseRequirementCode);
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
        boolean isUpdate = false;
        CourseRequirements crs = restUtils.getCourseRequirements(courseRequirement.getCourseCode(), courseRequirement.getCourseLevel(), summary.getAccessToken());
        if (crs != null && crs.getCourseRequirementList() != null && !crs.getCourseRequirementList().isEmpty()) {
            isUpdate = true;
        }
        logger.info(" Create CourseRequirement: course [{} / {}], rule [{}]",
                courseRequirement.getCourseCode(), courseRequirement.getCourseLevel(),
                courseRequirement.getRuleCode() != null? courseRequirement.getRuleCode().getCourseRequirementCode() : "");
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
            return null;
        }
    }

    private boolean isValidCourseForRule727(GraduationCourseKey courseKey) {
        if ( (StringUtils.equals(courseKey.getCourseCode().trim(), "AC") && StringUtils.equals(courseKey.getCourseLevel(), "11 "))
            || (StringUtils.equals(courseKey.getCourseCode().trim(), "ACC") && StringUtils.equals(courseKey.getCourseLevel(), "12 "))
            || (StringUtils.equals(courseKey.getCourseCode().trim(), "COP") && StringUtils.equals(courseKey.getCourseLevel(), "11 "))
            || (StringUtils.equals(courseKey.getCourseCode().trim(), "COP") && StringUtils.equals(courseKey.getCourseLevel(), "12 "))
            || (StringUtils.equals(courseKey.getCourseCode().trim(), "FA") && StringUtils.equals(courseKey.getCourseLevel(), "12 ")) ) {
            return false;
        }
        return true;
    }

    public boolean hasFrenchLanguageCourse(GraduationCourseKey key, String accessToken) {
        return this.restUtils.checkFrenchLanguageCourse(key.getCourseCode(), key.getCourseLevel(), accessToken);
    }

    public boolean hasBlankLanguageCourse(GraduationCourseKey key, String accessToken) {
        return this.restUtils.checkBlankLanguageCourse(key.getCourseCode(), key.getCourseLevel(), accessToken);
    }

    public boolean isFrenchImmersionCourse(String pen, String courseLevel, String accessToken) {
        return this.restUtils.checkFrenchImmersionCourse(pen, courseLevel, accessToken);
    }

    public boolean isFrenchImmersionCourseForEN(String pen, String courseLevel, String accessToken) {
        return this.restUtils.checkFrenchImmersionCourseForEN(pen, courseLevel, accessToken);
    }

    public List<StudentCourse> getStudentCourses(String pen, String accessToken) {
        return this.restUtils.getStudentCoursesByPen(pen, accessToken);
    }

}
