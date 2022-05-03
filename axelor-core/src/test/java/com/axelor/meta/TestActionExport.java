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
package com.axelor.meta;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.rpc.ActionRequest;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;

public class TestActionExport extends MetaTest {

  @Inject private ActionExecutor executor;

  private ActionHandler createHandler(String actions) {

    ActionRequest request = new ActionRequest();

    Map<String, Object> data = Maps.newHashMap();
    request.setData(data);
    request.setModel("com.axelor.test.db.Contact");

    data.put("action", actions);
    data.put("context", prepareContext());

    return executor.newActionHandler(request);
  }

  private Map<String, Object> prepareContext() {
    Map<String, Object> context = Maps.newHashMap();

    context.put("name", "SO001");
    context.put("orderDate", LocalDate.now());
    context.put("customer", ImmutableMap.of("name", "John Smith"));

    List<Object> items = Lists.newArrayList();
    context.put("items", items);

    items.add(
        ImmutableMap.of("product", ImmutableMap.of("name", "PC1"), "price", 250, "quantity", 1));
    items.add(
        ImmutableMap.of("product", ImmutableMap.of("name", "PC2"), "price", 550, "quantity", 1));
    items.add(
        ImmutableMap.of("product", ImmutableMap.of("name", "Laptop"), "price", 690, "quantity", 1));

    return context;
  }

  @Test
  public void test_export() throws Exception {
    ObjectViews views = this.unmarshal("com/axelor/meta/WSTest.xml", ObjectViews.class);

    MetaStore.resister(views);

    Action action = MetaStore.getAction("export.sale.order");
    assertNotNull(action);

    ActionHandler handler = createHandler("export.sale.order");
    Object response = action.execute(handler);
    assertNotNull(response);

    Object exportFile = ((Map) response).get("exportFile");
    assertNotNull(exportFile);
  }
}
