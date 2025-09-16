/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import com.axelor.events.StartupEvent;

public class ViewWatcherObserver {

  public void onAppStart(@Observes StartupEvent event) {
    ViewWatcher.getInstance().start();
  }

  public void onAppShutdown(@Observes ShutdownEvent event) {
    ViewWatcher.getInstance().stop();
  }
}
