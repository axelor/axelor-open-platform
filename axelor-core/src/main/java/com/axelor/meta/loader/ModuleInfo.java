/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.repo.MetaModuleRepository;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.List;

/**
 * The ModuleInfo class acts as a wrapper and provides a simplified interface for managing and
 * retrieving information about a specific module.
 */
public class ModuleInfo {

  private final String name;
  private final Module module;

  /**
   * Constructs a new ModuleInfo instance for the specified module name. Throws an
   * IllegalArgumentException if no module with the provided name is found.
   *
   * @param name the name of the module to retrieve information for
   * @throws IllegalArgumentException if the module with the specified name cannot be found
   */
  public ModuleInfo(String name) {
    this.name = name;
    this.module = ModuleManager.getModule(name);
    if (this.module == null) {
      throw new IllegalArgumentException("Module not found: " + name);
    }
  }

  /**
   * Retrieves the name of the module.
   *
   * @return the name of the module
   */
  public String getName() {
    return name;
  }

  /**
   * Retrieves the version of the module.
   *
   * @return the version of the module
   */
  public String getVersion() {
    return module.getVersion();
  }

  /**
   * Retrieves the Maven group identifier associated with the module.
   *
   * @return the Maven group identifier of the module
   */
  public String getMavenGroup() {
    return module.getMavenGroup();
  }

  /**
   * Retrieves the description of the module.
   *
   * @return the description of the module
   */
  public String getDescription() {
    return module.getDescription();
  }

  /**
   * Retrieves the title of the module.
   *
   * @return the title of the module
   */
  public String getTitle() {
    return module.getTitle();
  }

  /**
   * Whatever the module is installed.
   *
   * @return true if the module is installed, false otherwise
   */
  public boolean isInstalled() {
    return module.isInstalled();
  }

  /**
   * Retrieves a list of modules that the current module depends on, represented as {@link
   * ModuleInfo} objects.
   *
   * @return a list of {@link ModuleInfo}
   */
  public List<ModuleInfo> getDepends() {
    List<ModuleInfo> depends = new ArrayList<>();
    for (Module depend : module.getDepends()) {
      depends.add(new ModuleInfo(depend.getName()));
    }
    return depends;
  }

  /**
   * Retrieves the corresponding {@link MetaModule} associated to the current module name.
   *
   * @return the {@link MetaModule} associated with the current module name
   */
  public MetaModule getMetaModule() {
    return Beans.get(MetaModuleRepository.class).findByName(name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(ModuleInfo.class.getName(), name);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null) return false;
    if (!(obj instanceof ModuleInfo)) return false;
    return name.equals(((ModuleInfo) obj).getName());
  }

  @Override
  public String toString() {
    return name;
  }
}
