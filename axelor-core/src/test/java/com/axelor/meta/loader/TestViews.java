/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.common.ResourceUtils;
import com.axelor.db.Query.Selector;
import com.axelor.meta.MetaTest;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.ChartView;
import com.axelor.meta.schema.views.FormView;
import com.axelor.meta.schema.views.PanelInclude;
import com.axelor.meta.schema.views.Search;
import com.axelor.script.ScriptHelper;
import com.axelor.test.db.Title;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestViews extends MetaTest {

  @Inject private ViewLoader loader;

  @Test
  public void test1() throws Exception {
    ObjectViews views = this.unmarshal("com/axelor/meta/Contact.xml", ObjectViews.class);

    assertNotNull(views);
    assertNotNull(views.getViews());
    assertEquals(2, views.getViews().size());

    String json = toJson(views);

    assertNotNull(json);
  }

  @Test
  public void test2() throws Exception {
    ObjectViews views = this.unmarshal("com/axelor/meta/Welcome.xml", ObjectViews.class);

    assertNotNull(views);
    assertNotNull(views.getViews());
    assertEquals(1, views.getViews().size());

    String json = toJson(views);

    assertNotNull(json);
  }

  @Test
  @Transactional
  public void test3() throws Exception {
    ObjectViews views = this.unmarshal("com/axelor/meta/Search.xml", ObjectViews.class);
    assertNotNull(views);
    assertNotNull(views.getViews());
    assertEquals(1, views.getViews().size());

    String json = toJson(views);

    assertNotNull(json);

    Search search = (Search) views.getViews().getFirst();

    Title title = all(Title.class).filter("self.code = ?", "mr").fetchOne();
    assertNotNull(title);

    Map<String, Object> binding = new HashMap<>();
    binding.put("customer", "Some");
    binding.put("date", "2011-11-11");
    binding.put("xxx", 111);
    binding.put("title", title);
    binding.put("country", "IN");
    binding.put("value", "100.10");

    Map<String, Object> partner = new HashMap<>();
    partner.put("firstName", "Name");

    binding.put("partner", partner);

    ScriptHelper helper = search.scriptHandler(binding);

    for (Search.SearchSelect s : search.getSelects()) {
      Selector q = s.toQuery(helper);
      if (q == null) continue;

      assertNotNull(q.fetch(search.getLimit(), 0));
    }
  }

  @Test
  @Transactional
  public void testInclude() throws Exception {

    final URL url = ResourceUtils.getResource("com/axelor/meta/Include.xml");
    loader.process(url, new Module("test"), false);

    final AbstractView form1 = XMLViews.findView("contact-form1", null, null, "test");
    final AbstractView form2 = XMLViews.findView("contact-form2", null, null, "test");

    assertTrue(form1 instanceof FormView);
    assertTrue(form2 instanceof FormView);

    final PanelInclude include = (PanelInclude) ((FormView) form2).getItems().getFirst();
    final AbstractView included = include.getView();

    assertEquals(form1.getName(), included.getName());
  }

  @Test
  public void testChart() throws Exception {
    ObjectViews views = this.unmarshal("com/axelor/meta/Charts.xml", ObjectViews.class);

    ChartView chartView = (ChartView) views.getViews().getFirst();

    assertEquals(1, chartView.getActions().size());
    assertEquals("testChartAction", chartView.getActions().getFirst().getName());
    assertEquals(
        "com.axelor.meta.web.Hello:chartAction", chartView.getActions().getFirst().getAction());

    StringWriter writer = new StringWriter();
    XMLViews.marshal(views, writer);

    assertTrue(writer.toString().contains("com.axelor.meta.web.Hello:chartAction"));
  }
}
