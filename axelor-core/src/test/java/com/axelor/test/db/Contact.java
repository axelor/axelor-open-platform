package com.axelor.test.db;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import com.axelor.db.Binary;
import com.axelor.db.JPA;
import com.axelor.db.JpaModel;
import com.axelor.db.NameColumn;
import com.axelor.db.Query;
import com.axelor.db.VirtualColumn;
import com.axelor.db.Widget;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.Lists;

@Entity
@Table(name = "CONTACT_CONTACT")
public class Contact extends JpaModel {

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	private Title title;

	@NotNull
	private String firstName;

	@NotNull
	private String lastName;
	
	@Widget(search = { "firstName", "lastName" })
	@NameColumn
	@VirtualColumn
	@Access(AccessType.PROPERTY)
	private String fullName;

	@NotNull
	private String email;

	private String phone;

	private String lang;

	private LocalDate dateOfBirth;

	@OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true)
	private List<Address> addresses;

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch=FetchType.LAZY)
	private Set<Group> groups;

	@Widget(title = "Photo", help = "Max size 4MB.")
	@Lob @Basic(fetch = FetchType.LAZY)
	@Type(type="com.axelor.db.types.BinaryType")
	private Binary image;

	@Widget(multiline = true)
	private String notes;

	public Contact() {
	}

	public Contact(String firstName) {
		this.firstName = firstName;
	}

	public Contact(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public Title getTitle() {
		return title;
	}

	public void setTitle(Title title) {
		this.title = title;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getFullName() {
		return fullName = calculateFullName();
	}

	protected String calculateFullName() {
		fullName = firstName + " " + lastName;
		if (this.title != null) {
			return this.title.getName() + " " + fullName;
		}
		return fullName;
	}
	
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public List<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}

	public Group getGroup(int index) {
		if (groups == null) return null;
		return Lists.newArrayList(groups).get(index);
	}

	public Set<Group> getGroups() {
		return groups;
	}

	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}
	
	public Binary getImage() {
		return image;
	}
	
	public void setImage(Binary image) {
		this.image = image;
	}
	
	public String getNotes() {
		return notes;
	}
	
	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	@Override
	public String toString() {
		ToStringHelper tsh = Objects.toStringHelper(getClass());
		
		tsh.add("id", getId());
		tsh.add("fullName", getFirstName());
		tsh.add("email", getEmail());
		
		return tsh.omitNullValues().toString();
	}
	
	public Contact find(Long id) {
		return JPA.find(Contact.class, id);
	}

	public static Contact edit(Map<String, Object> values) {
		return JPA.edit(Contact.class, values);
	}

	public static Query<Contact> all() {
		return JPA.all(Contact.class);
	}

}
