package com.axelor.web.db;

import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.axelor.db.JPA;
import com.axelor.db.JpaModel;
import com.axelor.db.Query;

@Entity
public class Title extends JpaModel {

	@NotNull
	@Size( min = 2 )
	private String code;
	
	@NotNull
	@Size( min = 2 )
	private String name;
	
	public Title() {
	}

	public Title(String code, String name) {
		this.code = code;
		this.name = name;
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
	
	public Title save() {
		return JPA.save(this);
	}
}
