/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta.loader;

import com.axelor.db.ParallelTransactionExecutor;
import java.net.URL;
import java.util.List;
import java.util.ListIterator;

abstract class AbstractParallelLoader extends AbstractLoader {

  protected abstract List<List<URL>> findFileLists(Module module);

  protected abstract void doLoad(URL file, Module module, boolean update);

  @Override
  protected void doLoad(Module module, boolean update) {
    findFileLists(module)
        .stream()
        .flatMap(List::stream)
        .forEach(file -> doLoad(file, module, update));
  }

  protected void feedTransactionExecutor(
      ParallelTransactionExecutor transactionExecutor, Module module, boolean update) {
    for (final ListIterator<List<URL>> it = findFileLists(module).listIterator(); it.hasNext(); ) {
      final int priority = it.nextIndex();
      final List<URL> files = it.next();
      files
          .parallelStream()
          .forEach(file -> transactionExecutor.add(() -> doLoad(file, module, update), priority));
    }
  }
}
