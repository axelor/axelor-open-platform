/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.theme;

import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaTheme;
import jakarta.annotation.Nullable;
import java.util.List;

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
   * @return list of {@link AvailableTheme}
   */
  List<AvailableTheme> getAvailableThemes(User user);
}
