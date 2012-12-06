package com.axelor.db;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * A generic implementation of {@link Model}.
 * 
 * This class uses {@link GenerationType#AUTO} strategy for id generation which
 * is best suited for models where sequence of record ids is important.
 * 
 * This is the most optimal strategy so this class should be used whenever
 * possible.
 * 
 */
@MappedSuperclass
public abstract class JpaModel extends Model {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
