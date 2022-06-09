package ca.bc.gov.educ.api.dataconversion.entity;

import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.ThreadLocalStateUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Date;

@Data
@MappedSuperclass
public class BaseEntity {
	@Column(name = "CREATE_USER", nullable = true)
	private String createUser;

	@Column(name = "CREATE_DATE", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	@DateTimeFormat(pattern = "yyyy-mm-dd hh:mm:ss")
	private Date createDate;

	@Column(name = "UPDATE_USER", nullable = true)
	private String updateUser;

	@Column(name = "UPDATE_DATE", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	@DateTimeFormat(pattern = "yyyy-mm-dd hh:mm:ss")
	private Date updateDate;

	@PrePersist
	protected void onCreate() {
		if (StringUtils.isBlank(createUser)) {
			this.createUser = ThreadLocalStateUtil.getCurrentUser();
			if (StringUtils.isBlank(createUser)) {
				this.createUser = EducGradDataConversionApiConstants.DEFAULT_CREATED_BY;
			}
		}
		if (StringUtils.isBlank(updateUser)) {
			this.updateUser = ThreadLocalStateUtil.getCurrentUser();
			if (StringUtils.isBlank(updateUser)) {
				this.updateUser = EducGradDataConversionApiConstants.DEFAULT_UPDATED_BY;
			}
		}
		this.createDate = new Date(System.currentTimeMillis());
		this.updateDate = new Date(System.currentTimeMillis());
	}

	@PreUpdate
	protected void onPersist() {
		this.updateDate = new Date(System.currentTimeMillis());
		if (StringUtils.isBlank(updateUser)) {
			this.updateUser = ThreadLocalStateUtil.getCurrentUser();
			if (StringUtils.isBlank(updateUser)) {
				this.updateUser = EducGradDataConversionApiConstants.DEFAULT_UPDATED_BY;
			}
		}
		if (StringUtils.isBlank(createUser)) {
			this.createUser = ThreadLocalStateUtil.getCurrentUser();
			if (StringUtils.isBlank(createUser)) {
				this.createUser = EducGradDataConversionApiConstants.DEFAULT_CREATED_BY;
			}
		}
		if (this.createDate == null) {
			this.createDate = new Date(System.currentTimeMillis());
		}
	}
}
