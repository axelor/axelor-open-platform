/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    final List<Model> records = new ArrayList<>();

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
    Map<String, Object> result = new HashMap<>();

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

      List<Model> records = new ArrayList<>();
      Object data = action.evaluate(handler);

      if (data instanceof Collection<?> collection) {
        for (Object item : collection) {
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
