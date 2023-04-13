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
package com.axelor.meta;

import com.axelor.JpaTest;
import com.axelor.common.ResourceUtils;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.repo.MetaJsonFieldRepository;
import com.axelor.test.db.Contact;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.persist.Transactional;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public abstract class MetaTest extends JpaTest {

  @Inject private ObjectMapper mapper;

  @Inject private MetaJsonFieldRepository jsonFields;

  protected InputStream read(String resource) {
    return ResourceUtils.getResourceStream(resource);
  }

  protected ObjectMapper getObjectMapper() {
    return mapper;
  }

  @SuppressWarnings("unchecked")
  protected <T> T unmarshal(String resource, Class<T> type) throws JAXBException {
    JAXBContext context = JAXBContext.newInstance(type);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    return (T) unmarshaller.unmarshal(read(resource));
  }

  protected String toJson(Object object)
      throws JsonGenerationException, JsonMappingException, IOException {
    return getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object);
  }

  @BeforeEach
  @Transactional
  public void prepare() {
    prepareCustomFields();
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

}
