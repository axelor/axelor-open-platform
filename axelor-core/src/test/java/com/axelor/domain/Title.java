package com.axelor.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;

@Entity
@Table(name = "CONTACT_TITLE")
public class Title extends Model {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CONTACT_TITLE_SEQ")
	@SequenceGenerator(name = "CONTACT_TITLE_SEQ", sequenceName = "CONTACT_TITLE_SEQ", allocationSize = 1)
	private Long id;

	@NotNull
	@Column(unique = true)
	private String code;

	@NotNull
	@Column(unique = true)
	private String name;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static Query<Title> all() {
		return JPA.all(Title.class);
	}

}
