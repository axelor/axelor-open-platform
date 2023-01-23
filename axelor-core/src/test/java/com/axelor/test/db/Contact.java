/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.test.db;

import com.axelor.db.EntityHelper;
import com.axelor.db.JpaModel;
import com.axelor.db.annotations.EqualsInclude;
import com.axelor.db.annotations.NameColumn;
import com.axelor.db.annotations.VirtualColumn;
import com.axelor.db.annotations.Widget;
import com.google.common.collect.Lists;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "CONTACT_CONTACT")
public class Contact extends JpaModel {

  @ManyToOne(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  private Title title;

  @NotNull private String firstName;

  @NotNull private String lastName;

  @Widget(search = {"firstName", "lastName"})
  @NameColumn
  @VirtualColumn
  @Access(AccessType.PROPERTY)
  private String fullName;

  private String email;

  private String proEmail;

  private String phone;

  private String lang;

  @Widget(selection = "food.selection")
  private String food;

  private BigDecimal credit;

  private LocalDate dateOfBirth;

  @OneToMany(
      mappedBy = "contact",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private List<Address> addresses;

  @ManyToMany(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  private Set<Circle> circles;

  @Widget(title = "Photo", help = "Max size 4MB.")
  @Lob
  @Basic(fetch = FetchType.LAZY)
  private byte[] image;

  @Widget(selection = "contact.type")
  private String contactType;

  @Basic
  @Type(type = "com.axelor.db.hibernate.type.ValueEnumType")
  private EnumStatusNumber contactStatus;

  @Transient
  @Widget(multiline = true)
  private String notes;

  @EqualsInclude
  @Column(unique = true)
  private String uniqueName;

  @EqualsInclude
  @Column(unique = true)
  private String UUID;

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private Set<Contact> relatedContacts;

  @Widget(title = "Attributes")
  @Type(type = "json")
  private String attrs;

  @Widget(title = "Another Attributes")
  @Type(type = "json")
  private String anotherAttrs;

  public Contact() {}

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
    return fullName = computeFullName();
  }

  protected String computeFullName() {
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

  public String getProEmail() {
    return proEmail;
  }

  public void setProEmail(String proEmail) {
    this.proEmail = proEmail;
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

  public String getFood() {
    return food;
  }

  public void setFood(String food) {
    this.food = food;
  }

  public BigDecimal getCredit() {
    return credit;
  }

  public void setCredit(BigDecimal credit) {
    this.credit = credit;
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

  public Circle getCircle(int index) {
    if (circles == null) return null;
    return Lists.newArrayList(circles).get(index);
  }

  public Set<Circle> getCircles() {
    return circles;
  }

  public void setCircles(Set<Circle> groups) {
    this.circles = groups;
  }

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }

  public String getContactType() {
    return contactType;
  }

  public void setContactType(String contactType) {
    this.contactType = contactType;
  }

  public EnumStatusNumber getContactStatus() {
    return contactStatus;
  }

  public void setContactStatus(EnumStatusNumber contactStatus) {
    this.contactStatus = contactStatus;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getUniqueName() {
    return uniqueName;
  }

  public void setUniqueName(String uniqueName) {
    this.uniqueName = uniqueName;
  }

  public String getUUID() {
    return UUID;
  }

  public void setUUID(String UUID) {
    this.UUID = UUID;
  }

  public Set<Contact> getRelatedContacts() {
    return relatedContacts;
  }

  public void setRelatedContacts(Set<Contact> relatedContacts) {
    this.relatedContacts = relatedContacts;
  }

  public void addRelatedContact(Contact item) {
    if (getRelatedContacts() == null) {
      setRelatedContacts(new HashSet<>());
    }
    getRelatedContacts().add(item);
  }

  public void removeRelatedContact(Contact item) {
    if (getRelatedContacts() == null) {
      return;
    }
    getRelatedContacts().remove(item);
  }

  public void clearRelatedContacts() {
    if (getRelatedContacts() != null) {
      getRelatedContacts().clear();
    }
  }

  public String getAttrs() {
    return attrs;
  }

  public void setAttrs(String attrs) {
    this.attrs = attrs;
  }

  public String getAnotherAttrs() {
    return anotherAttrs;
  }

  public void setAnotherAttrs(String anotherAttrs) {
    this.anotherAttrs = anotherAttrs;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (!(obj instanceof Contact)) return false;

    final Contact other = (Contact) obj;
    if (this.getId() != null || other.getId() != null) {
      return Objects.equals(this.getId(), other.getId());
    }

    return Objects.equals(getUniqueName(), other.getUniqueName())
        && Objects.equals(getUUID(), other.getUUID())
        && (getUniqueName() != null || getUUID() != null);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  @Override
  public String toString() {
    return EntityHelper.toString(this);
  }
}
