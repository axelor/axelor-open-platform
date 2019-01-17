/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.test.db;

import com.axelor.db.JpaModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.Digits;

@Entity
@Table(name = "CONTACT_TYPE_CHECK")
public class TypeCheck extends JpaModel {

  private Boolean boolValue;

  private Long longValue;

  private Integer intValue;

  private Double doubleValue;

  private boolean boolValue2;

  private long longValue2;

  private int intValue2;

  private double doubleValue2;

  @Digits(integer = 20, fraction = 10)
  private BigDecimal decimalValue1;

  private BigDecimal decimalValue2;

  private ZonedDateTime dateTime1;

  private ZonedDateTime dateTime2;

  private LocalDate localDate1;

  private LocalDate localDate2;

  public Boolean getBoolValue() {
    return boolValue == null ? Boolean.FALSE : boolValue;
  }

  public void setBoolValue(Boolean boolValue) {
    this.boolValue = boolValue;
  }

  public Long getLongValue() {
    return longValue;
  }

  public void setLongValue(Long longValue) {
    this.longValue = longValue;
  }

  public Integer getIntValue() {
    return intValue == null ? 0 : intValue;
  }

  public void setIntValue(Integer intValue) {
    this.intValue = intValue;
  }

  public Double getDoubleValue() {
    return doubleValue;
  }

  public void setDoubleValue(Double doubleValue) {
    this.doubleValue = doubleValue;
  }

  public boolean isBoolValue2() {
    return boolValue2;
  }

  public void setBoolValue2(boolean boolValue2) {
    this.boolValue2 = boolValue2;
  }

  public long getLongValue2() {
    return longValue2;
  }

  public void setLongValue2(long longValue2) {
    this.longValue2 = longValue2;
  }

  public int getIntValue2() {
    return intValue2;
  }

  public void setIntValue2(int intValue2) {
    this.intValue2 = intValue2;
  }

  public double getDoubleValue2() {
    return doubleValue2;
  }

  public void setDoubleValue2(double doubleValue2) {
    this.doubleValue2 = doubleValue2;
  }

  public BigDecimal getDecimalValue1() {
    return decimalValue1;
  }

  public void setDecimalValue1(BigDecimal decimalValue1) {
    this.decimalValue1 = decimalValue1;
  }

  public BigDecimal getDecimalValue2() {
    return decimalValue2;
  }

  public void setDecimalValue2(BigDecimal decimalValue2) {
    this.decimalValue2 = decimalValue2;
  }

  public ZonedDateTime getDateTime1() {
    return dateTime1;
  }

  public void setDateTime1(ZonedDateTime dateTime1) {
    this.dateTime1 = dateTime1;
  }

  public ZonedDateTime getDateTime2() {
    return dateTime2;
  }

  public void setDateTime2(ZonedDateTime dateTime2) {
    this.dateTime2 = dateTime2;
  }

  public LocalDate getLocalDate1() {
    return localDate1;
  }

  public void setLocalDate1(LocalDate localDate1) {
    this.localDate1 = localDate1;
  }

  public LocalDate getLocalDate2() {
    return localDate2;
  }

  public void setLocalDate2(LocalDate localDate2) {
    this.localDate2 = localDate2;
  }
}
