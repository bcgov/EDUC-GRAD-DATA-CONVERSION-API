package ca.bc.gov.educ.api.dataconversion.entity.trax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "STUDENT_MASTER")
public class TraxStudentEntity {
    @Id
    @Column(name = "STUD_NO", unique = true, updatable = false)
    private String studNo;

//    @Column(name = "ARCHIVE_FLAG")
//    private String archiveFlag;
//
//    @Column(name = "STUD_SURNAME")
//    private String studSurname;
//
//    @Column(name = "STUD_GIVEN")
//    private String studGiven;
//
//    @Column(name = "STUD_MIDDLE")
//    private String studMiddle;
//
//    @Column(name = "ADDRESS1")
//    private String address1;
//
//    @Column(name = "ADDRESS2")
//    private String address2;
//
//    @Column(name = "CITY")
//    private String city;
//
//    @Column(name = "PROV_CODE")
//    private String provCode;
//
//    @Column(name = "CNTRY_CODE")
//    private String cntryCode;
//
//    @Column(name = "POSTAL")
//    private String postal;
//
//    @Column(name = "STUD_BIRTH")
//    private String studBirth;
//
//    @Column(name = "STUD_SEX")
//    private String studSex;
//
//    @Column(name = "STUD_CITIZ")
//    private String studCitiz;
//
//    @Column(name = "STUD_GRADE")
//    private String studGrade;
//
//    @Column(name = "MINCODE")
//    private String mincode;
//
//    @Column(name = "STUD_LOCAL_ID")
//    private String studLocalId;
//
//    @Column(name = "STUD_TRUE_NO")
//    private String studTrueNo;
//
//    @Column(name = "STUD_SIN")
//    private String studSin;
//
//    @Column(name = "PRGM_CODE")
//    private String prgmCode;
//
//    @Column(name = "PRGM_CODE2")
//    private String prgmCode2;
//
//    @Column(name = "PRGM_CODE3")
//    private String prgmCode3;
//
//    @Column(name = "STUD_PSI_PERMIT")
//    private String studPsiPermit;
//
//    @Column(name = "STUD_RSRCH_PERMIT")
//    private String studRsrchPermit;
//
//    @Column(name = "STUD_STATUS")
//    private String studStatus;
//
//    @Column(name = "STUD_CONSED_FLAG")
//    private String studConsedFlag;
//
//    @Column(name = "YR_ENTER_11")
//    private String yrEnter11;
//
//    @Column(name = "GRAD_DATE")
//    private Long gradDate;
//
//    @Column(name = "DOGWOOD_FLAG")
//    private String dogwoodFlag;
//
//    @Column(name = "HONOUR_FLAG")
//    private String honourFlag;
//
//    @Column(name = "MINCODE_GRAD")
//    private String mincodeGrad;
//
//    @Column(name = "FRENCH_DOGWOOD")
//    private String frenchDogwood;
//
//    @Column(name = "PRGM_CODE4")
//    private String prgmCode4;
//
//    @Column(name = "PRGM_CODE5")
//    private String prgmCode5;
//
//    @Column(name = "SCC_DATE")
//    private Long sccDate;
//
//    @Column(name = "GRAD_REQT_YEAR")
//    private String gradReqtYear;
//
//    @Column(name = "SLP_DATE")
//    private Long slpDate;
//
//    @Column(name = "MERGED_FROM_PEN")
//    private String mergedFromPen;
//
//    @Column(name = "GRAD_REQT_YEAR_AT_GRAD")
//    private String gradReqtYearAtGrad;
//
//    @Column(name = "STUD_GRADE_AT_GRAD")
//    private String studGradeAtGrad;
//
//    @Column(name = "XCRIPT_ACTV_DATE")
//    private Long xcriptActvDate;
//
//    @Column(name = "ALLOWED_ADULT")
//    private String allowedAdult;
//
//    @Column(name = "SSA_NOMINATION_DATE")
//    private String ssaNominationDate;
//
//    @Column(name = "ADJ_TEST_YEAR")
//    private String adjTestYear;
//
//    @Column(name = "GRADUATED_ADULT")
//    private String graduatedAdult;
//
//    @Column(name = "SUPPLIER_NO")
//    private String supplierNo;
//
//    @Column(name = "SITE_NO")
//    private String siteNo;
//
//    @Column(name = "EMAIL_ADDRESS")
//    private String emailAddress;
//
//    @Column(name = "ENGLISH_CERT")
//    private String englishCert;
//
//    @Column(name = "FRENCH_CERT")
//    private String frenchCert;
//
//    @Column(name = "ENGLISH_CERT_DATE")
//    private Long englishCertDate;
//
//    @Column(name = "FRENCH_CERT_DATE")
//    private Long frenchCertDate;
}
