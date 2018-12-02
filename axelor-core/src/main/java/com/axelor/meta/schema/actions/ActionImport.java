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
package com.axelor.meta.schema.actions;

import com.axelor.data.ImportException;
import com.axelor.data.ImportTask;
import com.axelor.data.Listener;
import com.axelor.data.xml.XMLImporter;
import com.axelor.db.Model;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaStore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class ActionImport extends Action {

  @XmlAttribute private String config;

  @XmlElement(name = "import")
  private List<Import> imports;

  public String getConfig() {
    return config;
  }

  public List<Import> getImports() {
    return imports;
  }

  private List<Model> doImport(XMLImporter importer, final String fileName, Object data) {

    if (!(data instanceof String)) {
      log.debug("stream type not supported: " + data.getClass());
      return null;
    }

    log.info("action-import: " + fileName);

    final StringReader reader = new StringReader((String) data);
    final HashMultimap<String, Reader> mapping = HashMultimap.create();
    final List<Model> records = Lists.newArrayList();

    mapping.put(fileName, reader);

    importer.addListener(
        new Listener() {
          @Override
          public void imported(Model bean) {
            log.info(
                "action-import (record): {}(id={})", bean.getClass().getSimpleName(), bean.getId());
            records.add(bean);
          }

          @Override
          public void imported(Integer total, Integer success) {
            // TODO Auto-generated method stub

          }

          @Override
          public void handle(Model bean, Exception e) {
            // TODO Auto-generated method stub

          }
        });

    importer.setCanClear(false);
    importer.run(
        new ImportTask() {

          @Override
          public void configure() throws IOException {
            input(fileName, reader);
          }

          @Override
          public boolean handle(ImportException e) {
            log.error("error:" + e);
            e.printStackTrace();
            return true;
          }
        });

    log.info("action-import (count): " + records.size());
    return records;
  }

  @Override
  public Object evaluate(ActionHandler handler) {
    Map<String, Object> result = Maps.newHashMap();

    Object configName = handler.evaluate(config);
    if (configName == null) {
      log.debug("No such config file: " + config);
      return result;
    }

    log.info("action-import (config): " + configName.toString());
    XMLImporter importer = new XMLImporter(configName.toString());
    importer.setContext(handler.getContext());

    int count = 0;
    for (Import stream : getImports()) {
      log.info(
          "action-import (stream, provider): " + stream.getFile() + ", " + stream.getProvider());
      Action action = MetaStore.getAction(stream.getProvider());
      if (action == null) {
        log.debug("No such action: " + stream.getProvider());
        continue;
      }

      List<Model> records = Lists.newArrayList();
      Object data = action.evaluate(handler);

      if (data instanceof Collection) {
        for (Object item : (Collection<?>) data) {
          if (item instanceof String) {
            log.info("action-import (xml stream)");
            List<Model> imported = doImport(importer, stream.getFile(), item);
            if (imported != null) {
              records.addAll(imported);
            }
          }
        }
      } else {
        log.info("action-import (object stream)");
        List<Model> imported = doImport(importer, stream.getFile(), data);
        if (imported != null) {
          records.addAll(imported);
        }
      }
      count += records.size();
      result.put(stream.name == null ? stream.getFile() : stream.name, records);
    }
    log.info("action-import (total): " + count);
    return result;
  }

  @Override
  protected Object wrapper(Object value) {
    final Map<String, Object> result = new HashMap<>();
    result.put("values", value);
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass()).add("name", getName()).toString();
  }

  @XmlType
  public static class Import {

    @XmlAttribute private String file;

    @XmlAttribute private String provider;

    @XmlAttribute private String name;

    public String getFile() {
      return file;
    }

    public String getProvider() {
      return provider;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(getClass())
          .add("file", file)
          .add("provider", provider)
          .toString();
    }
  }
}
