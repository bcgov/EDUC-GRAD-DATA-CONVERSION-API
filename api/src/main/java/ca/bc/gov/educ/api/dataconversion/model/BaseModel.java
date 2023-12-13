package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BaseModel implements Serializable {
	private String createUser;
	private Date createDate;
	private String updateUser;
	private Date updateDate;
}
