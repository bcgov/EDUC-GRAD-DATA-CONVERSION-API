package ca.bc.gov.educ.api.dataconversion.model.tsw;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class School {

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
    
	public String getSchoolName() {
		return  schoolName != null ? schoolName.trim(): null;
	}
	
	public String getDistrictName() {
		return districtName != null ? districtName.trim(): null;
	}
	
	public String getAddress1() {
		return address1 != null ? address1.trim(): null;
	}

	public String getAddress2() {
		return address2 != null ? address2.trim(): null;
	}

	public String getCity() {
		return city != null ? city.trim(): null;
	}

	public String getProvinceName() {
		return provinceName != null ? provinceName.trim(): null;
	}
	
	public String getCountryName() {
		return countryName != null ? countryName.trim(): null;
	}
	
	public String getPostal() {
		return postal != null ? postal.trim(): null;
	}
	
	public String getIndependentDesignation() {
		return independentDesignation != null ? independentDesignation.trim(): null;
	}
	
	public String getIndependentAffiliation() {
		return independentAffiliation != null ? independentAffiliation.trim(): null;
	}
	
	public String getOpenFlag() {
		return openFlag != null ? openFlag.trim(): null;
	}
	
	public String getSignatureDistrict() {
		return signatureDistrict != null ? signatureDistrict.trim(): null;
	}
	
	public String getSchoolEmail() {
		return  schoolEmail != null ? schoolEmail.trim(): null;
	}
	
	public String getPrincipalName() {
		return  principalName != null ? principalName.trim(): null;
	}
	
	public String getAppendTrans() {
		return  appendTrans != null ? appendTrans.trim(): null;
	}
	
	public String getMinistryContact() {
		return  ministryContact != null ? ministryContact.trim(): null;
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
}
