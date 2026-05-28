/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.db;

import com.axelor.db.JpaModel;
import com.axelor.db.annotations.NameColumn;
import com.axelor.db.annotations.VirtualColumn;
import com.axelor.db.annotations.Widget;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Entity
public class Contact extends JpaModel {

  @ManyToOne(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private Title title;

  @NotNull private String firstName;

  @NotNull private String lastName;

  @Widget(
      title = "Full Name",
      search = {"firstName", "lastName"})
  @NameColumn
  @VirtualColumn
  @Access(AccessType.PROPERTY)
  private String fullName;

  @NotNull private String email;

  private String phone;

  @Widget(title = "Address List")
  @OneToMany(
      fetch = FetchType.LAZY,
      mappedBy = "contact",
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<Address> addresses;

  public Contact() {}

  public Contact(String firstName, String lastName, String email, String phone) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.phone = phone;
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

  public List<Address> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<Address> addresses) {
    this.addresses = addresses;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getFullName() {
    fullName = computeFullName();
    return fullName;
  }

  protected String computeFullName() {
    if (title == null) return firstName + " " + lastName;
    return title.getName() + " " + firstName + " " + lastName;
  }
}
