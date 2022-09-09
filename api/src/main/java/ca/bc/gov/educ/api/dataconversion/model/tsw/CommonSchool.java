package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.Data;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Component
public class CommonSchool {

    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    @Size(max = 3)
    @NotNull(message = "distNo can not be null.")
    private String distNo;

    @Size(max = 5)
    @NotNull(message = "schlNo can not be null.")
    private String schlNo;

    @Size(max = 40)
    private String scAddressLine1;

    @Size(max = 40)
    private String scAddressLine2;

    @Size(max = 30)
    private String scCity;

    @Size(max = 2)
    private String scProvinceCode;

    @Size(max = 3)
    private String scCountryCode;

    @Size(max = 6)
    private String scPostalCode;

    @Size(max = 10)
    private String scFaxNumber;

    @Size(max = 10)
    private String scPhoneNumber;

    @Size(max = 100)
    private String scEMailId;

    @Size(max = 2)
    private String facilityTypeCode;

    @Size(max = 40)
    private String schoolName;

    @Size(max = 2)
    private String schoolTypeCode;

    @Size(max = 3)
    private String schoolOrganizationCode;

    @Size(max = 2)
    private String schoolCategoryCode;

    @Size(max = 25)
    private String prGivenName;

    @Size(max = 25)
    private String prSurname;

    @Size(max = 25)
    private String prMiddleName;

    @Size(max = 2)
    private String prTitleCode;

    private Long numberOfDivisions;

    private Long numberOfSecFteTeachers;

    private Long numberOfElmFteTeachers;

    private Long ttblElemInstrMinutes;

    @Size(max = 1)
    private String schoolStatusCode;

    private Long enrolHeadcount1523;

    private Long enrolHeadcount1701;

    @Size(max = 1)
    private String grade01Ind;

    @Size(max = 1)
    private String grade29Ind;

    @Size(max = 1)
    private String grade04Ind;

    @Size(max = 1)
    private String grade05Ind;

    @Size(max = 1)
    private String grade06Ind;

    @Size(max = 1)
    private String grade07Ind;

    @Size(max = 1)
    private String grade08Ind;

    @Size(max = 1)
    private String grade09Ind;

    @Size(max = 1)
    private String grade10Ind;

    @Size(max = 1)
    private String grade11Ind;

    @Size(max = 1)
    private String grade12Ind;

    @Size(max = 1)
    private String grade79Ind;

    @Size(max = 1)
    private String grade89Ind;

    @Size(max = 8)
    private String openedDate;

    @Size(max = 8)
    private String closedDate;

    @Size(max = 3)
    private String authNumber;

    private Long createDate;

    private Long createTime;

    @Size(max = 12)
    private String createUsername;

    private Long editDate;

    private Long editTime;

    @Size(max = 12)
    private String editUsername;

    private Long elemTeachersHc;

    private Long secTeachersHc;

//  @Size(max = 45)
//  private String comment;

    @Size(max = 1)
    private String gradeKhInd;

    @Size(max = 1)
    private String gradeKfInd;

    @Size(max = 1)
    private String grade02Ind;

    @Size(max = 1)
    private String grade03Ind;

    @Size(max = 1)
    private String gradeEuInd;

    @Size(max = 1)
    private String gradeSuInd;

    @Size(max = 1)
    private String gradeHsInd;

    @Size(max = 4)
    private String contedFundFlag;

    private Long elemFteClassroom;

    private Long elemFteSupport;

    private Long elemFteAdmin;

    private Long secFteClassroom;

    private Long secFteSupport;

    private Long secFteAdmin;

    @Size(max = 40)
    private String physAddressLine1;

    @Size(max = 40)
    private String physAddressLine2;

    @Size(max = 30)
    private String physCity;

    @Size(max = 2)
    private String physProvinceCode;

    @Size(max = 3)
    private String physCountryCode;

    @Size(max = 6)
    private String physPostalCode;

    private Long educMethodClassCnt;

    private Long educMethodDelCnt;

    private Long educMethodBothCnt;

    @Size(max = 3)
    private String newDistno;

    @Size(max = 5)
    private String newSchlno;

    private String dateOpened;

    private String dateClosed;

    private Long assetNumber;

    @Size(max = 12)
    private String assetAssignedBy;

    private String assetAssignedDate;

    @Size(max = 12)
    private String assetChangedBy;

    private String assetChangedDate;

    @Size(max = 1)
    private String restrictFunding;

    @Size(max = 1)
    private String gradeGaInd;

    @Size(max = 1)
    private String nlcEarlyLearningFlag;

    @Size(max = 1)
    private String nlcAfterSchoolProgramFlag;

    @Size(max = 1)
    private String nlcContinuingEdFlag;

    @Size(max = 1)
    private String nlcSeniorsFlag;

    @Size(max = 1)
    private String nlcSportAndRecFlag;

    @Size(max = 1)
    private String nlcCommunityUseFlag;

    @Size(max = 1)
    private String nlcIntegratedServicesFlag;
}