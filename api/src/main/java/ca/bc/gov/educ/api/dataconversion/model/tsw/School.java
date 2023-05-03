package ca.bc.gov.educ.api.dataconversion.model.tsw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Data
@Component
@JsonIgnoreProperties(ignoreUnknown = true)
public class School implements Comparable<School> {

	private String minCode;
	private String schoolName;
	private String districtName;
	private String transcriptEligibility;
	private String certificateEligibility;
	private String independentDesignation;
	private String mailerType;
	private String address1;
	private String address2;
	private String city;
	private String provCode;
	private String provinceName;
	private String countryCode;
	private String countryName;
	private String postal;
	private String independentAffiliation;
	private String openFlag;
	private String signatureDistrict;
	private String newMinCode;
	private String schoolOrg;
	private String appendTrans;
	private String ministryContact;
	private String principalName;
	private String schoolPhone;
	private String schoolFax;
	private String schoolEmail;

	private String schoolCategory;

	public String getSchoolName() {
		return  schoolName != null ? schoolName.trim(): "";
	}

	public String getDistrictName() {
		return districtName != null ? districtName.trim(): "";
	}

	public String getAddress1() {
		return address1 != null ? address1.trim(): "";
	}

	public String getAddress2() {
		return address2 != null ? address2.trim(): "";
	}

	public String getCity() {
		return city != null ? city.trim(): "";
	}

	public String getProvinceName() {
		return provinceName != null ? provinceName.trim(): "";
	}

	public String getCountryName() {
		return countryName != null ? countryName.trim(): "";
	}

	public String getPostal() {
		return postal != null ? postal.trim(): "";
	}

	public String getIndependentDesignation() {
		return independentDesignation != null ? independentDesignation.trim(): "";
	}

	public String getIndependentAffiliation() {
		return independentAffiliation != null ? independentAffiliation.trim(): "";
	}

	public String getOpenFlag() {
		return openFlag != null ? openFlag.trim(): "";
	}

	public String getReportingFlag() {
		return getOpenFlag();
	}

	public String getSignatureDistrict() {
		return signatureDistrict != null ? signatureDistrict.trim(): "";
	}

	public String getSchoolEmail() {
		return  schoolEmail != null ? schoolEmail.trim(): "";
	}

	public String getPrincipalName() {
		return  principalName != null ? principalName.trim(): "";
	}

	public String getAppendTrans() {
		return  appendTrans != null ? appendTrans.trim(): "";
	}

	public String getMinistryContact() {
		return  ministryContact != null ? ministryContact.trim(): "";
	}

	public String getMinCode() {
		return minCode != null ? minCode.trim(): "";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		School school = (School) o;
		return getMinCode().equals(school.getMinCode())
				&& getSchoolName().equals(school.getSchoolName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getMinCode(), getSchoolName());
	}

	@Override
	public String toString() {
		return "School [minCode=" + minCode + ", schoolName=" + schoolName + ", districtName=" + districtName
				+ ", transcriptEligibility=" + transcriptEligibility + ", certificateEligibility="
				+ certificateEligibility + ", independentDesignation=" + independentDesignation + ", mailerType="
				+ mailerType + ", address1=" + address1 + ", address2=" + address2 + ", city=" + city + ", provCode="
				+ provCode + ", provinceName=" + provinceName + ", countryCode=" + countryCode + ", countryName="
				+ countryName + ", postal=" + postal + ", independentAffiliation=" + independentAffiliation
				+ ", openFlag=" + openFlag + ", signatureDistrict=" + signatureDistrict + ", newMinCode=" + newMinCode
				+ ", schoolOrg=" + schoolOrg + ", appendTrans=" + appendTrans + ", ministryContact=" + ministryContact
				+ ", principalName=" + principalName + ", schoolPhone=" + schoolPhone + ", schoolFax=" + schoolFax
				+ ", schoolEmail=" + schoolEmail + "]";
	}

	@Override
	public int compareTo(School o) {
		int result = 0;
		{
			if (result == 0) {
				result = getMinCode().compareToIgnoreCase(o.getMinCode());
			}
			if (result == 0) {
				result = getSchoolName().compareToIgnoreCase(o.getSchoolName());
			}
		}
		return result;
	}
}
