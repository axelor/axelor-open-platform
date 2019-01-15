/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import com.axelor.db.JPA;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaEnum;
import com.axelor.meta.db.MetaSequence;
import com.axelor.meta.db.repo.MetaEnumRepository;
import com.axelor.meta.db.repo.MetaSequenceRepository;
import com.axelor.meta.service.MetaModelService;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ModelLoader extends AbstractLoader {

  private static Logger log = LoggerFactory.getLogger(ModelLoader.class);

  @Inject private MetaModelService service;

  @Inject private MetaSequenceRepository sequences;

  @Inject private MetaEnumRepository enums;

  @Override
  protected void doLoad(Module module, boolean update) {

    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final DocumentBuilder db;
    try {
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      return;
    }

    for (URL file : MetaScanner.findAll(module.getName(), "(domains|objects)", "(.*?)\\.xml$")) {
      log.debug("importing: {}", file.getFile());
      try (InputStream is = file.openStream()) {
        final Document doc = db.parse(is);
        final NodeList elements = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < elements.getLength(); i++) {
          final Node node = elements.item(i);
          if (node instanceof Element) {
            final Element element = (Element) elements.item(i);
            final String name = element.getTagName();
            if ("enum".equals(name)) importEnums(element, update);
            if ("entity".equals(name)) importModels(element, update);
            if ("sequence".equals(name)) importSequences(element, update);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  static Set<String> findEntities(Module module) {

    final Set<String> names = new HashSet<>();

    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final DocumentBuilder db;
    try {
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      return names;
    }

    for (URL file : MetaScanner.findAll(module.getName(), "(domains|objects)", "(.*?)\\.xml$")) {
      try (InputStream is = file.openStream()) {
        final Document doc = db.parse(is);
        final NodeList elements = doc.getElementsByTagName("entity");
        for (int i = 0; i < elements.getLength(); i++) {
          final Element element = (Element) elements.item(i);
          names.add(element.getAttribute("name"));
        }

      } catch (Exception e) {
      }
    }
    return names;
  }

  private void importModels(Element element, boolean update) {
    final String name = element.getAttribute("name");
    if ("Model".equals(name)) {
      return;
    }
    log.debug("Loading model: {}", name);
    service.process(JPA.model(name));
  }

  private void importEnums(Element element, boolean update) {
    final Element module =
        (Element) element.getOwnerDocument().getElementsByTagName("module").item(0);
    final String packageName = module.getAttribute("package");
    final String name = element.getAttribute("name");
    final String fullName = packageName + "." + name;

    log.debug("Loading enum: {}", fullName);

    MetaEnum found = enums.findByName(fullName);
    if (found == null) {
      found = new MetaEnum();
      found.setName(fullName);
    }

    enums.save(found);
  }

  private void importSequences(Element element, boolean update) {
    String name = element.getAttribute("name");

    if (isVisited(MetaSequence.class, name)) {
      return;
    }
    if (sequences.findByName(name) != null) {
      return;
    }

    log.debug("Loading sequence: {}", name);

    MetaSequence entity = new MetaSequence(name);

    entity.setPrefix(element.getAttribute("prefix"));
    entity.setSuffix(element.getAttribute("suffix"));

    Integer padding = Ints.tryParse(element.getAttribute("padding"));
    Integer increment = Ints.tryParse(element.getAttribute("increment"));
    Long initial = Longs.tryParse(element.getAttribute("initial"));

    if (padding != null) entity.setPadding(padding);
    if (increment != null) entity.setIncrement(increment);
    if (initial != null) entity.setInitial(initial);

    sequences.save(entity);
  }
}
