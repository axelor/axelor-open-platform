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
package com.axelor.meta.loader;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;

final class Module {

  private String name;

  private String title;

  private String description;

  private List<Module> depends = new ArrayList<>();

  private String version;

  private boolean application = false;

  private boolean installed = false;

  private boolean pending = false;

  public Module(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Module> getDepends() {
    return depends;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public boolean isApplication() {
    return application;
  }

  public void setApplication(boolean application) {
    this.application = application;
  }

  public boolean isInstalled() {
    return installed;
  }

  public void setInstalled(boolean installed) {
    this.installed = installed;
  }

  public boolean isPending() {
    return pending;
  }

  public void setPending(boolean pending) {
    this.pending = pending;
  }

  public void dependsOn(Module module) {
    if (!depends.contains(module)) {
      depends.add(module);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(Module.class.getName(), name);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null) return false;
    if (!(obj instanceof Module)) return false;
    return name.equals(((Module) obj).name);
  }

  public String pprint(int depth) {
    StringBuilder builder = new StringBuilder();
    builder.append(name).append("\n");
    for (Module dep : depends) {
      builder.append(Strings.repeat("  ", depth)).append("-> ").append(dep.pprint(depth + 1));
    }
    return builder.toString();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).add("version", version).toString();
  }
}
