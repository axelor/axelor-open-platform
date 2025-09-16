/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.rpc.ActionRequest;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class TestActionWS extends MetaTest {

  @Inject private ActionExecutor executor;

  private ActionHandler createHandler(String actions) {

    ActionRequest request = new ActionRequest();

    Map<String, Object> data = new HashMap<>();
    request.setData(data);
    request.setModel("com.axelor.test.db.Contact");

    data.put("action", actions);
    data.put("context", prepareContext());

    return executor.newActionHandler(request);
  }

  private Map<String, Object> prepareContext() {
    Map<String, Object> context = new HashMap<>();
    context.put("ISOCode", "FR");
    return context;
  }

  @Test
  public void test_ws_call() throws Exception {
    ObjectViews views = this.unmarshal("com/axelor/meta/WSTest.xml", ObjectViews.class);
    List<Action> actions = views.getActions();
    assertNotNull(actions);
    MetaStore.register(views);

    Action action = MetaStore.getAction("ws.capital.city");
    assertNotNull(action);

    ActionHandler handler = createHandler("ws.capital.city");
    Object response = action.execute(handler);
    assertNotNull(response);
    assertTrue(response.toString().contains("<m:CapitalCityResult>Paris</m:CapitalCityResult>"));
  }
}
