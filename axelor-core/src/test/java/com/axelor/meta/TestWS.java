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
package com.axelor.meta;

import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.repo.MetaSelectRepository;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.rpc.ActionRequest;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import com.axelor.text.GroovyTemplates;
import com.axelor.text.Templates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestWS extends MetaTest {

  @Inject private ActionExecutor executor;

  private ActionHandler createHandler(String actions, Map<String, Object> context) {

    ActionRequest request = new ActionRequest();

    Map<String, Object> data = Maps.newHashMap();
    request.setData(data);
    request.setModel("com.axelor.test.db.Contact");

    data.put("action", actions);
    data.put("context", context);

    return executor.newActionHandler(request);
  }

  @Test
  public void test1() throws Exception {
    ObjectViews views = this.unmarshal("com/axelor/meta/WSTest.xml", ObjectViews.class);
    List<Action> actions = views.getActions();

    Assert.assertNotNull(actions);
    Assert.assertEquals(4, actions.size());

    MetaStore.resister(views);

    Action action = MetaStore.getAction("data.import.1");
    Map<String, Object> context = Maps.newHashMap();

    ZonedDateTime dt = ZonedDateTime.now();
    dt = dt.plus(Period.ofDays(20));

    context.put("dt", dt);

    ActionHandler handler = createHandler("data.import.1", context);
    action.execute(handler);
  }

  @Test
  public void test2() throws Exception {
    ObjectViews views = this.unmarshal("com/axelor/meta/WSTest.xml", ObjectViews.class);

    MetaStore.resister(views);

    Action action = MetaStore.getAction("export.sale.order");
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

    ActionHandler handler = createHandler("export.sale.order", context);
    action.execute(handler);
  }

  @Transactional
  public void prepareTest3() {
    final MetaSelect select = new MetaSelect();
    final MetaSelectItem item = new MetaSelectItem();

    final MetaSelectRepository selects = Beans.get(MetaSelectRepository.class);
    final ContactRepository contacts = Beans.get(ContactRepository.class);

    item.setValue("pizza");
    item.setTitle("Pizza");
    select.addItem(item);
    select.setName("food.selection");

    selects.save(select);

    Contact c = new Contact();
    c.setFirstName("John");
    c.setLastName("Smith");
    c.setEmail("john.smith@gmail.com");
    c.setFood("pizza");

    contacts.save(c);
  }

  @Test
  public void test3() throws Exception {
    prepareTest3();
    Contact c = all(Contact.class).filter("self.email = ?", "john.smith@gmail.com").fetchOne();
    Map<String, Object> context = ImmutableMap.<String, Object>of("person", c);

    ActionHandler actionHandler = createHandler("dummy", context);
    Templates engine = new GroovyTemplates();
    Reader template =
        new StringReader("<%__fmt__.debug('Person food : {}', person.food)%>${person.food}");

    String text = actionHandler.template(engine, template);
    Assert.assertEquals("pizza", text);

    template = new StringReader("${ person.food | text}");
    text = actionHandler.template(engine, template);
    Assert.assertEquals("Pizza", text);
  }
}
