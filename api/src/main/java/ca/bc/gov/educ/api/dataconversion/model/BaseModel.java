package ca.bc.gov.educ.api.dataconversion.model;

import java.util.Date;

import lombok.Data;

@Data
public class BaseModel {
	private String createUser;
	private Date createDate;
	private String updateUser;
	private Date updateDate;
}
