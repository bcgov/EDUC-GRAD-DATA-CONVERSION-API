package ca.bc.gov.educ.api.dataconversion.service.course;

import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseKey;
import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRequirementCodeEntity;
import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRequirementEntity;
import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRestrictionEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionAlert;
import ca.bc.gov.educ.api.dataconversion.model.ConversionBaseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.repository.course.CourseRequirementCodeRepository;
import ca.bc.gov.educ.api.dataconversion.repository.course.CourseRequirementRepository;
import ca.bc.gov.educ.api.dataconversion.repository.course.CourseRestrictionRepository;
import ca.bc.gov.educ.api.dataconversion.util.DateConversionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);

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

    private final CourseRestrictionRepository courseRestrictionRepository;
    private final CourseRequirementRepository courseRequirementRepository;
    private final CourseRequirementCodeRepository courseRequirementCodeRepository;

    @Autowired
    public CourseService(CourseRestrictionRepository courseRestrictionRepository,
                         CourseRequirementRepository courseRequirementRepository,
                         CourseRequirementCodeRepository courseRequirementCodeRepository) {
        this.courseRestrictionRepository = courseRestrictionRepository;
        this.courseRequirementRepository = courseRequirementRepository;
        this.courseRequirementCodeRepository = courseRequirementCodeRepository;
    }

    @Transactional(transactionManager = "courseTransactionManager")
    public GradCourseRestriction convertCourseRestriction(GradCourseRestriction courseRestriction, ConversionBaseSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        if (isInvalidData(courseRestriction.getMainCourse(), courseRestriction.getRestrictedCourse())) {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.WARNING);
            error.setItem(courseRestriction.getMainCourse() + " " + courseRestriction.getRestrictedCourse());
            error.setReason("Skip invalid data");
            summary.getErrors().add(error);
            return null;
        }
        Optional<CourseRestrictionEntity> optional =  courseRestrictionRepository.findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel(
                courseRestriction.getMainCourse(), courseRestriction.getMainCourseLevel(), courseRestriction.getRestrictedCourse(), courseRestriction.getRestrictedCourseLevel());

        CourseRestrictionEntity entity = optional.orElseGet(CourseRestrictionEntity::new);
        convertCourseRestrictionData(courseRestriction, entity);
        courseRestrictionRepository.save(entity);
        if (optional.isPresent()) {
            summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
        } else {
            summary.setAddedCount(summary.getAddedCount() + 1L);
        }
        return courseRestriction;
    }

    private boolean isInvalidData(String mainCourseCode, String restrictedCourseCode) {
        Pair<String, String> pair = Pair.of(mainCourseCode, restrictedCourseCode);
        return IGNORE_LIST.contains(pair);
    }

    private void convertCourseRestrictionData(GradCourseRestriction courseRestriction, CourseRestrictionEntity courseRestrictionEntity) {
        if (courseRestrictionEntity.getCourseRestrictionId() == null) {
            courseRestrictionEntity.setCourseRestrictionId(UUID.randomUUID());
        }
        courseRestrictionEntity.setMainCourse(courseRestriction.getMainCourse());
        courseRestrictionEntity.setMainCourseLevel(courseRestriction.getMainCourseLevel() == null? " " :  courseRestriction.getMainCourseLevel());
        courseRestrictionEntity.setRestrictedCourse(courseRestriction.getRestrictedCourse());
        courseRestrictionEntity.setRestrictedCourseLevel(courseRestriction.getRestrictedCourseLevel() == null? " " : courseRestriction.getRestrictedCourseLevel());
        // data conversion
        if (StringUtils.isNotBlank(courseRestriction.getRestrictionStartDate())) {
            Date start = DateConversionUtils.convertStringToDate(courseRestriction.getRestrictionStartDate());
            if (start != null) {
                courseRestrictionEntity.setRestrictionStartDate(start);
            }
        }
        if (StringUtils.isNotBlank(courseRestriction.getRestrictionEndDate())) {
            Date end = DateConversionUtils.convertStringToDate(courseRestriction.getRestrictionEndDate());
            if (end != null) {
                courseRestrictionEntity.setRestrictionEndDate(end);
            }
        }
//        else {
//            courseRestrictionEntity.setRestrictionEndDate(DateConversionUtils.convertStringToDate(EducGradDataConversionApiConstants.DEFAULT_END_DATE_FORMAT));
//        }
    }

    @Transactional(transactionManager = "courseTransactionManager")
    public GraduationCourseEntity convertCourseRequirement(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        processEnglish(graduationCourseEntity, summary);
        processSocials(graduationCourseEntity, summary);
        processMath(graduationCourseEntity, summary);
        processScience(graduationCourseEntity, summary);
        processCareerPersonal(graduationCourseEntity, summary);
        processPhysEd(graduationCourseEntity, summary);
        processAppliedSkills(graduationCourseEntity, summary);
        processPortFolio(graduationCourseEntity, summary);
        processConsEd(graduationCourseEntity, summary);
        processFineArts(graduationCourseEntity, summary);
        processCareerLiefConnections(graduationCourseEntity, summary);
        return graduationCourseEntity;
    }

    private void processEnglish(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // english10
        if (StringUtils.isNotBlank(graduationCourseEntity.getEnglish10()) && StringUtils.equals(graduationCourseEntity.getEnglish10(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "101"), summary);
            } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "701"), summary);
            }
            if (hasFrenchLanguageCourse(graduationCourseEntity.getGraduationCourseKey())) {
                if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "302"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "815"), summary);
                }
            }
            if (hasBlankLanguageCourse(graduationCourseEntity.getGraduationCourseKey())) {
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
            if (hasFrenchLanguageCourse(graduationCourseEntity.getGraduationCourseKey())) {
                if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "301"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "816"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "818"), summary);
                }
            }
            if (hasBlankLanguageCourse(graduationCourseEntity.getGraduationCourseKey())) {
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
            if (hasFrenchLanguageCourse(graduationCourseEntity.getGraduationCourseKey())) {
                if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "300"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2004")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "817"), summary);
                } else if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "819"), summary);
                }
            }
            if (hasBlankLanguageCourse(graduationCourseEntity.getGraduationCourseKey())) {
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
                } else {
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
        // portfolio
        if (StringUtils.isNotBlank(graduationCourseEntity.getConsEd()) && StringUtils.equals(graduationCourseEntity.getConsEd(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1986")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "745"), summary);
            }
        }
    }

    private void processFineArts(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // portfolio
        if (StringUtils.isNotBlank(graduationCourseEntity.getFineArts()) && StringUtils.equals(graduationCourseEntity.getFineArts(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "1996")) {
                if (StringUtils.isBlank(graduationCourseEntity.getAppliedSkills()) || !StringUtils.equals(graduationCourseEntity.getAppliedSkills(), "Y")) {
                    createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "726"), summary);
                }
            }
        }
    }

    private void processCareerLiefConnections(GraduationCourseEntity graduationCourseEntity, ConversionCourseSummaryDTO summary) {
        // careerLifeConnections
        if (StringUtils.isNotBlank(graduationCourseEntity.getCareerLifeConnections()) && StringUtils.equals(graduationCourseEntity.getCareerLifeConnections(), "Y")) {
            if (StringUtils.equals(graduationCourseEntity.getGraduationCourseKey().getGradReqtYear(), "2018")) {
                createCourseRequirement(populate(graduationCourseEntity.getGraduationCourseKey(), "113"), summary);
            }
        }
    }

    @Transactional(transactionManager = "courseTransactionManager")
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

    private CourseRequirementEntity populate(GraduationCourseKey key, String courseRequirementCode) {
        return populate(key.getCourseCode(), key.getCourseLevel(), courseRequirementCode);
    }

    private CourseRequirementEntity populate(String courseCode, String courseLevel, String courseRequirementCode) {
        CourseRequirementEntity courseRequirement = new CourseRequirementEntity();
        courseRequirement.setCourseCode(courseCode.trim());
        courseRequirement.setCourseLevel(StringUtils.isBlank(courseLevel)? " " :  courseLevel.trim());

        Optional<CourseRequirementCodeEntity> courseRequirementCodeOptional = courseRequirementCodeRepository.findById(courseRequirementCode);
        if (courseRequirementCodeOptional.isPresent()) {
            courseRequirement.setRuleCode(courseRequirementCodeOptional.get());
        }
        courseRequirement.setCourseRequirementId(UUID.randomUUID());
        return courseRequirement;
    }

    private void createCourseRequirement(CourseRequirementEntity courseRequirementEntity, ConversionCourseSummaryDTO summary) {
        CourseRequirementEntity currentEntity = courseRequirementRepository.findByCourseCodeAndCourseLevelAndRuleCode(
                courseRequirementEntity.getCourseCode(), courseRequirementEntity.getCourseLevel(), courseRequirementEntity.getRuleCode());
        logger.info(" Create CourseRequirement: course [{} / {}], rule [{}]",
                courseRequirementEntity.getCourseCode(), courseRequirementEntity.getCourseLevel(),
                courseRequirementEntity.getRuleCode() != null? courseRequirementEntity.getRuleCode().getCourseRequirementCode() : "");
        if (currentEntity != null) {
            // Update
            currentEntity.setUpdateDate(null);
            currentEntity.setUpdateUser(null);
            courseRequirementRepository.save(currentEntity);
            summary.setUpdatedCountForCourseRequirement(summary.getUpdatedCountForCourseRequirement() + 1L);
        } else {
            // Add
            courseRequirementRepository.save(courseRequirementEntity);
            summary.setAddedCountForCourseRequirement(summary.getAddedCountForCourseRequirement() + 1L);
        }
    }

    @Transactional(readOnly = true, transactionManager = "courseTransactionManager")
    public boolean hasFrenchLanguageCourse(GraduationCourseKey key) {
        if (this.courseRequirementRepository.countTabCourses(key.getCourseCode(), key.getCourseLevel(), "F") > 0L) {
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true, transactionManager = "courseTransactionManager")
    public boolean hasBlankLanguageCourse(GraduationCourseKey key) {
        if (this.courseRequirementRepository.countTabCourses(key.getCourseCode(), key.getCourseLevel(), " ") > 0L) {
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true, transactionManager = "courseTransactionManager")
    public boolean isFrenchImmersionCourse(String pen, String courseLevel) {
        if (this.courseRequirementRepository.countFrenchImmersionCourses(pen, courseLevel) > 0L) {
            return true;
        }
        return false;
    }
    @Transactional(readOnly = true, transactionManager = "courseTransactionManager")
    public boolean isFrenchImmersionCourseForEN(String pen, String courseLevel) {
        if (this.courseRequirementRepository.countFrenchImmersionCourse(pen, courseLevel) > 0L) {
            return true;
        }
        return false;
    }

}
