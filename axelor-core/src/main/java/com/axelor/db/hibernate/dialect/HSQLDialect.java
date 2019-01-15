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
package com.axelor.db.hibernate.dialect;

import com.axelor.db.hibernate.type.EncryptedTextType;
import com.axelor.db.hibernate.type.JsonSqlTypeDescriptor;
import com.axelor.db.hibernate.type.JsonType;
import java.sql.Types;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;

public class HSQLDialect extends org.hibernate.dialect.HSQLDialect {

  public HSQLDialect() {
    super();
    registerColumnType(Types.OTHER, "clob");
  }

  @Override
  public void contributeTypes(
      TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    super.contributeTypes(typeContributions, serviceRegistry);
    typeContributions.contributeType(new JsonType(JsonSqlTypeDescriptor.INSTANCE));
    typeContributions.contributeType(EncryptedTextType.INSTANCE);
  }
}
