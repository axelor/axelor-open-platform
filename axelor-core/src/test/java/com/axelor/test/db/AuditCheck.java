/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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

import com.axelor.auth.db.AuditableModel;
import com.axelor.db.annotations.Sequence;
import com.axelor.db.annotations.Track;
import com.axelor.db.annotations.TrackEvent;
import com.axelor.db.annotations.TrackField;
import com.axelor.db.annotations.TrackMessage;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "AUDIT_CHECK")
@Track(
    fields = {
      @TrackField(name = "name"),
      @TrackField(name = "email", on = TrackEvent.ALWAYS),
      @TrackField(name = "empSeq", on = TrackEvent.CREATE)
    },
    messages = {
      @TrackMessage(
          message = "Invalid email?",
          on = TrackEvent.ALWAYS,
          condition = "email?.contains('example.com')")
    })
public class AuditCheck extends AuditableModel {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CONTACT_TITLE_SEQ")
  @SequenceGenerator(
      name = "CONTACT_TITLE_SEQ",
      sequenceName = "CONTACT_TITLE_SEQ",
      allocationSize = 1)
  private Long id;

  private String name;

  private String email;

  @Sequence("seq.emp.id")
  private String empSeq;

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getEmpSeq() {
    return empSeq;
  }

  public void setEmpSeq(String empSeq) {
    this.empSeq = empSeq;
  }
}
