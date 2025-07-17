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
package com.axelor.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.test.fixture.Fixture;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class FixtureTest {

  @Test
  public void test() throws IOException {

    var types =
        Map.of(
            Person.class.getSimpleName(), Person.class,
            Address.class.getSimpleName(), Address.class,
            Country.class.getSimpleName(), Country.class);

    var fixture = new Fixture();
    var records = fixture.load("fixture-test.yaml", types::get, x -> x);

    assertNotNull(records);
    assertEquals(5, records.size());

    var country =
        records.stream()
            .filter(x -> x instanceof Country)
            .map(x -> (Country) x)
            .filter(x -> "FR".equals(x.getCode()))
            .findFirst()
            .orElse(null);

    assertNotNull(country);
    assertEquals("France", country.getName());

    var address =
        records.stream()
            .filter(x -> x instanceof Address)
            .map(x -> (Address) x)
            .filter(x -> "Paris".equals(x.getCity()))
            .findFirst()
            .orElse(null);

    assertNotNull(address);
    assertEquals("Main St", address.getStreet());
    assertEquals(123, address.getNumber());
    assertTrue(address.isValid);
    assertNotNull(address.getCountry());
    assertEquals(country, address.getCountry());
    assertNotNull(address.getPerson());
    assertEquals(AddressType.HOME, address.getType());

    var person =
        records.stream()
            .filter(x -> x instanceof Person)
            .map(x -> (Person) x)
            .findFirst()
            .orElse(null);

    assertNotNull(person);
    assertEquals("John", person.getFirstName());
    assertEquals("Doe", person.getLastName());
    assertEquals("john.doe@example.com", person.getEmail());
    assertEquals(LocalDate.of(1979, 11, 1), person.getBirtDate());
    assertEquals(1234567890, person.getInternalCode());
    assertEquals(new BigDecimal("159.99"), person.getTotalSale());
    assertNotNull(person.getAddresses());
    assertEquals(2, person.getAddresses().size());

    assertEquals(address, person.getAddresses().getFirst());

    // Verify that the address is linked to the person
    assertEquals(address.getPerson(), person);
  }

  static class Person {

    private String firstName;

    private String lastName;

    private String email;

    private LocalDate birtDate;

    private Long internalCode;

    private BigDecimal totalSale;

    private List<Address> addresses;

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

    public List<Address> getAddresses() {
      return addresses;
    }

    public void setAddresses(List<Address> addresses) {
      this.addresses = addresses;
    }

    public LocalDate getBirtDate() {
      return birtDate;
    }

    public void setBirtDate(LocalDate birtDate) {
      this.birtDate = birtDate;
    }

    public void setInternalCode(Long internalCode) {
      this.internalCode = internalCode;
    }

    public Long getInternalCode() {
      return internalCode;
    }

    public BigDecimal getTotalSale() {
      return totalSale;
    }

    public void setTotalSale(BigDecimal totalSale) {
      this.totalSale = totalSale;
    }
  }

  static enum AddressType {
    HOME,
    WORK
  }

  static class Address {

    private Person person;

    private Integer number;

    private String street;

    private String city;

    private String zipCode;

    private Country country;

    private AddressType type;

    private Boolean isValid;

    public Person getPerson() {
      return person;
    }

    public void setPerson(Person person) {
      this.person = person;
    }

    public String getStreet() {
      return street;
    }

    public void setStreet(String street) {
      this.street = street;
    }

    public String getCity() {
      return city;
    }

    public void setCity(String city) {
      this.city = city;
    }

    public String getZipCode() {
      return zipCode;
    }

    public void setZipCode(String zipCode) {
      this.zipCode = zipCode;
    }

    public Country getCountry() {
      return country;
    }

    public void setCountry(Country country) {
      this.country = country;
    }

    public AddressType getType() {
      return type;
    }

    public void setType(AddressType type) {
      this.type = type;
    }

    public Integer getNumber() {
      return number;
    }

    public void setNumber(Integer number) {
      this.number = number;
    }

    public void setIsValid(Boolean valid) {
      isValid = valid;
    }

    public Boolean getIsValid() {
      return isValid;
    }
  }

  static class Country {

    private String name;

    private String code;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }
  }
}
