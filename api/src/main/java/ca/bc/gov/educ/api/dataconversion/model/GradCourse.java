package ca.bc.gov.educ.api.dataconversion.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class GradCourse {
    private String courseCode;
    private String courseLevel;
    private String gradReqtYear;

    private String startSession;
    private String endSession;

    // english
    private String english10;
    private String english11;
    private String english12;

    // career personal
    private String careerPersonal10;
    private String careerPersonal11;
    private String careerPersonal12;

    // socials
    private String socials;
    private String socials10;

    // science
    private String science;
    private String science10;

    // math
    private String math;
    private String math10;

    // physics education
    private String physEd;
    private String physEd10;

    // applied skills
    private String appliedSkills;

    // portfolio
    private String portfolio;

    // cons ed
    private String consEd;

    // fine arts
    private String fineArts;

    // career life connections
    private String careerLifeConnections;

}
