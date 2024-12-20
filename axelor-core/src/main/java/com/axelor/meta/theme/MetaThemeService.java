/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.meta.theme;

import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaTheme;
import java.util.List;
import javax.annotation.Nullable;

public interface MetaThemeService {

  /**
   * Retrieve the theme content depending on the given name. The current user can be used to
   * retrieve theme depending on user context.
   *
   * @param name the theme name
   * @param user the current user, or null if not available
   * @return {@link MetaTheme}
   */
  MetaTheme getTheme(String name, @Nullable User user);

  /**
   * Retrieve the available themes for the user.
   *
   * @param user the given user
   * @return list of available {@link MetaTheme}
   */
  List<MetaTheme> getAvailableThemes(User user);
}
