/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import com.axelor.i18n.I18n;
import com.axelor.rpc.filter.Filter;
import java.util.Set;

public interface JpaSecurity {

  public static enum AccessType {
    READ(/*$$(*/ "You are not authorized to read this resource." /*)*/),
    WRITE(/*$$(*/ "You are not authorized to update this resource." /*)*/),
    CREATE(/*$$(*/ "You are not authorized to create this resource." /*)*/),
    REMOVE(/*$$(*/ "You are not authorized to remove this resource." /*)*/),
    IMPORT(/*$$(*/ "You are not authorized to import the data." /*)*/),
    EXPORT(/*$$(*/ "You are not authorized to export the data." /*)*/);

    private String message;

    private AccessType(String message) {
      this.message = message;
    }

    public String getMessage() {
      return I18n.get(message);
    }
  }

  public static final AccessType CAN_READ = AccessType.READ;
  public static final AccessType CAN_WRITE = AccessType.WRITE;
  public static final AccessType CAN_CREATE = AccessType.CREATE;
  public static final AccessType CAN_REMOVE = AccessType.REMOVE;
  public static final AccessType CAN_IMPORT = AccessType.IMPORT;
  public static final AccessType CAN_EXPORT = AccessType.EXPORT;

  Set<AccessType> getAccessTypes(Class<? extends Model> model);

  Set<AccessType> getAccessTypes(Class<? extends Model> model, Long id);

  boolean hasRole(String name);

  Filter getFilter(AccessType type, Class<? extends Model> model, Long... ids);

  boolean isPermitted(AccessType type, Class<? extends Model> model, Long... ids);

  void check(AccessType type, Class<? extends Model> model, Long... ids);
}
