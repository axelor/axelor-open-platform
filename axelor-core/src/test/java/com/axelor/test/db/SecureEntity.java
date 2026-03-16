/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2026 Axelor (<http://axelor.com>).
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
import com.axelor.db.annotations.Widget;
import com.axelor.db.converters.EncryptedBytesConverter;
import com.axelor.db.converters.EncryptedStringConverter;
import com.google.common.base.MoreObjects;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "SECURE_ENTITY")
public class SecureEntity extends AuditableModel {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SECURE_ENTITY_SEQ")
  @SequenceGenerator(
      name = "SECURE_ENTITY_SEQ",
      sequenceName = "SECURE_ENTITY_SEQ",
      allocationSize = 1)
  private Long id;

  @Convert(converter = EncryptedStringConverter.class)
  private String mySecureString;

  private String myString;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Convert(converter = EncryptedBytesConverter.class)
  private byte[] mySecureBinary;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  private byte[] myBinary;

  @Widget(title = "Attributes")
  @Basic(fetch = FetchType.LAZY)
  @Type(type = "json")
  private String attrs;

  public SecureEntity() {}

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public void setId(Long id) {
    this.id = id;
  }

  public String getMySecureString() {
    return mySecureString;
  }

  public void setMySecureString(String mySecureString) {
    this.mySecureString = mySecureString;
  }

  public String getMyString() {
    return myString;
  }

  public void setMyString(String myString) {
    this.myString = myString;
  }

  public byte[] getMySecureBinary() {
    return mySecureBinary;
  }

  public void setMySecureBinary(byte[] mySecureBinary) {
    this.mySecureBinary = mySecureBinary;
  }

  public byte[] getMyBinary() {
    return myBinary;
  }

  public void setMyBinary(byte[] myBinary) {
    this.myBinary = myBinary;
  }

  public String getAttrs() {
    return attrs;
  }

  public void setAttrs(String attrs) {
    this.attrs = attrs;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (!(obj instanceof SecureEntity)) return false;

    final SecureEntity other = (SecureEntity) obj;
    if (this.getId() != null || other.getId() != null) {
      return Objects.equals(this.getId(), other.getId());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 31;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", getId())
        .add("mySecureString", getMySecureString())
        .add("myString", getMyString())
        .omitNullValues()
        .toString();
  }
}
