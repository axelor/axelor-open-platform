/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.web;

import com.axelor.meta.db.MetaFilter;
import com.axelor.meta.service.MetaFilterService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import jakarta.inject.Inject;

public class MetaFilterController {

  @Inject private MetaFilterService service;

  public void saveFilter(ActionRequest request, ActionResponse response) {
    MetaFilter ctx = request.getContext().asType(MetaFilter.class);
    if (ctx != null) {
      ctx = service.saveFilter(ctx);
      response.setData(ctx);
    }
  }

  public void removeFilter(ActionRequest request, ActionResponse response) {
    MetaFilter ctx = request.getContext().asType(MetaFilter.class);
    if (ctx != null) {
      ctx = service.removeFilter(ctx);
      response.setData(ctx);
    }
  }

  public void findFilters(ActionRequest request, ActionResponse response) {
    MetaFilter ctx = request.getContext().asType(MetaFilter.class);
    if (ctx != null && ctx.getFilterView() != null) {
      response.setData(service.getFilters(ctx.getFilterView()));
    }
  }
}
