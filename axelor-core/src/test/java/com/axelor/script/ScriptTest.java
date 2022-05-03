/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.script;

import com.axelor.JpaTest;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.repo.MetaJsonFieldRepository;
import com.axelor.meta.db.repo.MetaJsonModelRepository;
import com.axelor.meta.db.repo.MetaSelectRepository;
import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Title;
import com.axelor.test.db.repo.ContactRepository;
import com.axelor.test.db.repo.TitleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

public abstract class ScriptTest extends JpaTest {

  @Inject private ContactRepository contacts;

  @Inject private TitleRepository titles;

  @Inject private MetaSelectRepository selects;

  @Inject private MetaJsonFieldRepository jsonFields;

  @Inject private MetaJsonModelRepository jsonModels;

  @Inject private ObjectMapper jsonMapper;

  protected Contact contact;

  private Title title;

  @BeforeEach
  @Transactional
  public void prepare() {
    prepareCustomModels();
    prepareCustomFields();
    prepareAnotherCustomFields();
    prepareDemoData();
    prepareMoreData();
  }

  protected void prepareMoreData() {}

  private void prepareDemoData() {
    if (titles.all().count() == 0) {
      Title t = new Title();
      t.setCode("mr");
      t.setName("Mr.");
      titles.save(t);
    }

    if (contacts.all().count() == 0) {
      Contact c = new Contact();
      c.setFirstName("John");
      c.setLastName("Smith");
      c.setEmail("jsmith@gmail.com");
      c.setTitle(titles.findByCode("mr"));
      contacts.save(c);
    }

    if (contact == null) {
      contact = contacts.findByEmail("jsmith@gmail.com");
    }

    if (title == null) {
      title = titles.findByCode("mrs");
    }

    // prepare selection lists
    if (selects.findByName("colors") == null) {
      MetaSelect colors = new MetaSelect();
      MetaSelect types = new MetaSelect();

      colors.setName("colors");
      colors.addItem(
          new MetaSelectItem() {
            {
              setValue("black");
              setTitle("Black");
            }
          });
      colors.addItem(
          new MetaSelectItem() {
            {
              setValue("white");
              setTitle("White");
            }
          });
      colors.addItem(
          new MetaSelectItem() {
            {
              setValue("red");
              setTitle("Red");
            }
          });

      types.setName("contact.type");
      types.addItem(
          new MetaSelectItem() {
            {
              setValue("customer");
              setTitle("Customer");
            }
          });
      types.addItem(
          new MetaSelectItem() {
            {
              setValue("supplier");
              setTitle("Supplier");
            }
          });

      selects.save(colors);
      selects.save(types);
    }
  }

  private void prepareCustomFields() {
    if (jsonFields
            .all()
            .filter("self.model = :model AND self.modelField = :field")
            .bind("model", Contact.class.getName())
            .bind("field", "attrs")
            .count()
        == 0) {

      final Consumer<MetaJsonField> fields =
          f -> {
            f.setModel(Contact.class.getName());
            f.setModelField("attrs");
            jsonFields.save(f);
          };

      MetaJsonField field;

      field = new MetaJsonField();
      field.setName("nickName");
      field.setType("string");
      fields.accept(field);

      field = new MetaJsonField();
      field.setName("numerology");
      field.setType("integer");
      fields.accept(field);

      field = new MetaJsonField();
      field.setName("birthDate");
      field.setType("date");
      fields.accept(field);

      field = new MetaJsonField();
      field.setName("guardian");
      field.setType("many-to-one");
      field.setTargetModel(Contact.class.getName());
      fields.accept(field);

      field = new MetaJsonField();
      field.setName("favColor");
      field.setType("string");
      field.setSelection("colors");
      fields.accept(field);

      field = new MetaJsonField();
      field.setName("isCustomer");
      field.setType("boolean");
      fields.accept(field);

      field = new MetaJsonField();
      field.setName("orderAmount");
      field.setType("decimal");
      field.setScale(2);
      fields.accept(field);

      field = new MetaJsonField();
      field.setName("lastActivity");
      field.setType("datetime");
      fields.accept(field);
    }
  }

  private void prepareAnotherCustomFields() {
    if (jsonFields
            .all()
            .filter("self.model = :model AND self.modelField = :field")
            .bind("model", Contact.class.getName())
            .bind("field", "anotherAttrs")
            .count()
        == 0) {

      final Consumer<MetaJsonField> fields =
          f -> {
            f.setModel(Contact.class.getName());
            f.setModelField("anotherAttrs");
            jsonFields.save(f);
          };

      MetaJsonField field;

      field = new MetaJsonField();
      field.setName("nickName");
      field.setType("string");
      fields.accept(field);

      field = new MetaJsonField();
      field.setName("guardian");
      field.setType("many-to-one");
      field.setTargetModel(Contact.class.getName());
      fields.accept(field);

      field = new MetaJsonField();
      field.setName("favColor");
      field.setType("string");
      field.setSelection("colors");
      fields.accept(field);
    }
  }

  private void prepareCustomModels() {
    if (jsonModels.findByName("hello") == null) {
      final MetaJsonModel hello = new MetaJsonModel();
      hello.setName("hello");
      hello.setTitle("Hello");
      hello.addField(
          new MetaJsonField() {
            {
              setName("name");
              setNameField(true);
              setType("string");
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });
      hello.addField(
          new MetaJsonField() {
            {
              setName("date");
              setType("datetime");
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });
      hello.addField(
          new MetaJsonField() {
            {
              setName("color");
              setType("string");
              setSelection("colors");
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });

      jsonModels.save(hello);

      final MetaJsonModel world = new MetaJsonModel();
      world.setName("world");
      world.setTitle("World");
      world.addField(
          new MetaJsonField() {
            {
              setName("name");
              setNameField(true);
              setType("string");
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });
      world.addField(
          new MetaJsonField() {
            {
              setName("price");
              setType("decimal");
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });

      hello.addField(
          new MetaJsonField() {
            {
              setName("world");
              setType("json-many-to-one");
              setTargetJsonModel(world);
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });

      hello.addField(
          new MetaJsonField() {
            {
              setName("contact");
              setType("many-to-one");
              setTargetModel(Contact.class.getName());
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });

      jsonModels.save(world);
    }
  }

  protected Context context() {
    return new Context(contextMap(), Contact.class);
  }

  protected Map<String, Object> contextMap() {
    final Map<String, Object> values = new HashMap<>();
    values.put("lastName", "NAME");
    values.put("id", contact.getId());
    values.put("_model", Contact.class.getName());

    final Map<String, Object> t = new HashMap<>();
    t.put("id", title.getId());
    values.put("title", t);

    values.put("contactType", "customer");
    values.put("contactStatus", 1);

    values.put("attrs", getCustomerAttrsJson());
    values.put("anotherAttrs", getCustomerAnotherAttrsJson());

    final List<Map<String, Object>> addresses = new ArrayList<>();
    final Map<String, Object> a1 = new HashMap<>();
    a1.put("street", "My");
    a1.put("area", "Home");
    a1.put("city", "Paris");
    a1.put("zip", "1212");
    final Map<String, Object> a2 = new HashMap<>();
    a2.put("street", "My");
    a2.put("area", "Office");
    a2.put("city", "London");
    a2.put("zip", "1111");
    a2.put("selected", true);

    addresses.add(a1);
    addresses.add(a2);

    values.put("addresses", addresses);

    final Map<String, Object> parent = new HashMap<>();
    parent.put("_model", Contact.class.getName());
    parent.put("id", contact.getId());
    parent.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

    values.put("_parent", parent);
    values.put("_ref", parent);

    return values;
  }

  protected String getCustomerAttrsJson() {
    final Map<String, Object> map = new HashMap<>();

    map.put("nickName", "Some Name");
    map.put("numerology", 2);
    map.put("birthDate", LocalDate.of(2020, 5, 22).toString());
    map.put("favColor", "red");
    map.put("isCustomer", true);
    map.put("orderAmount", new BigDecimal("1000.20"));
    map.put("lastActivity", LocalDateTime.of(2021, 4, 29, 7, 57, 0));

    final Map<String, Object> guardian = new HashMap<>();
    guardian.put("id", 1L);

    map.put("guardian", guardian);

    try {
      return jsonMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  protected String getCustomerAnotherAttrsJson() {
    final Map<String, Object> map = new HashMap<>();

    map.put("nickName", "Some Custom Name");
    map.put("numerology", 5);
    map.put("favColor", "black");

    final Map<String, Object> guardian = new HashMap<>();
    guardian.put("id", all(Contact.class).fetchOne().getId());

    map.put("guardian", guardian);

    try {
      return jsonMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      return null;
    }
  }
}
