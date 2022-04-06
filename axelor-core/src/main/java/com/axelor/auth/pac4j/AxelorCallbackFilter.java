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
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.common.StringUtils;
import io.buji.pac4j.filter.CallbackFilter;
import javax.inject.Inject;
import org.pac4j.core.config.Config;

public class AxelorCallbackFilter extends CallbackFilter {

  @Inject
  public AxelorCallbackFilter(Config config, AxelorCallbackLogic callbackLogic) {
    setConfig(config);

    final AppSettings settings = AppSettings.get();
    final String defaultUrl = settings.getBaseURL();

    if (StringUtils.notBlank(defaultUrl)) {
      setDefaultUrl(defaultUrl);
    }

    setDefaultClient(config.getClients().getClients().get(0).getName());
    setCallbackLogic(callbackLogic);
  }
}
