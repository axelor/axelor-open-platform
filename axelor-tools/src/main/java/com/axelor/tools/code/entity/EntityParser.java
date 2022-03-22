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
package com.axelor.tools.code.entity;

import com.axelor.tools.code.entity.model.BaseType;
import com.axelor.tools.code.entity.model.DomainModels;
import com.axelor.tools.code.entity.model.Entity;
import com.axelor.tools.code.entity.model.EnumType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class EntityParser {

  private static final AtomicReference<JAXBContext> CONTEXT = new AtomicReference<>();

  private static JAXBContext getContext() throws JAXBException {
    if (CONTEXT.get() == null) {
      synchronized (CONTEXT) {
        CONTEXT.set(JAXBContext.newInstance(DomainModels.class));
      }
    }
    return CONTEXT.get();
  }

  public static List<BaseType<?>> parse(File file) throws JAXBException {
    JAXBContext context = getContext();
    Unmarshaller unmarshaller = context.createUnmarshaller();
    DomainModels domain = (DomainModels) unmarshaller.unmarshal(file);

    List<Entity> entities = domain.getEntities();
    List<EnumType> enums = domain.getEnums();

    List<BaseType<?>> types = new ArrayList<>();

    types.addAll(entities);
    types.addAll(enums);

    return types;
  }
}
