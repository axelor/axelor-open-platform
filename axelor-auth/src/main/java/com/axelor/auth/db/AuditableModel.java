package com.axelor.auth.db;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.joda.time.LocalDateTime;

import com.axelor.db.Model;
import com.axelor.db.Widget;

/**
 * The base abstract class with update logging feature.
 * 
 * The model instance logs the creation date, last modified date,
 * the authorized user who created the record and the user who
 * updated the record last time.
 * 
 */
@MappedSuperclass
public abstract class AuditableModel extends Model {

	@Widget(readonly = true)
	private LocalDateTime createdOn;

	@Widget(readonly = true)
	private LocalDateTime updatedOn;

	@Widget(readonly = true)
	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private User createdBy;

	@Widget(readonly = true)
	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private User updatedBy;
	
	public LocalDateTime getCreatedOn() {
		return createdOn;
	}

	@Access(AccessType.FIELD)
	private void setCreatedOn(LocalDateTime createdOn) {
		this.createdOn = createdOn;
	}

	public LocalDateTime getUpdatedOn() {
		return updatedOn;
	}

	@Access(AccessType.FIELD)
	private void setUpdatedOn(LocalDateTime updatedOn) {
		this.updatedOn = updatedOn;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	@Access(AccessType.FIELD)
	private void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public User getUpdatedBy() {
		return updatedBy;
	}

	@Access(AccessType.FIELD)
	private void setUpdatedBy(User updatedBy) {
		this.updatedBy = updatedBy;
	}
}
