/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import com.axelor.auth.db.Group;
import com.axelor.auth.db.repo.GroupRepository;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.Query;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.views.AbstractView;
import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/** Create computed view from base view and their extensions. */
public class ComputedViewProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(ComputedViewProcessor.class);

  private static final String STRING_DELIMITER = ",";

  private final MetaView baseView;
  private final List<MetaView> extendsViews;

  private final ComputedViewXmlProcessor processor;

  private final GroupRepository groupRepo;
  private final MetaViewRepository metaViewRepo;

  public ComputedViewProcessor(MetaView baseView, List<MetaView> extendsViews) {
    this.baseView = baseView;
    this.extendsViews = extendsViews;
    this.groupRepo = Beans.get(GroupRepository.class);
    this.metaViewRepo = Beans.get(MetaViewRepository.class);
    this.processor = new ComputedViewXmlProcessor(baseView, extendsViews);
  }

  public MetaView compute()
      throws JAXBException, ParserConfigurationException, IOException, SAXException,
          XPathExpressionException {
    MetaView computedView = getComputedView();
    if (computedView == null) {
      return null;
    }

    LOG.debug("Compute view {} with extensions...", baseView.getName());

    Document document = processor.compute();

    final ObjectViews objectViews = XMLViews.unmarshal(document);
    final AbstractView finalView = objectViews.getViews().get(0);
    String finalXml = XMLViews.toXml(finalView, true);

    computedView.setXml(finalXml);
    computedView.setModule(getLastModule());

    addGroups(computedView, finalView.getGroups());

    return computedView;
  }

  private void addGroups(MetaView view, String codes) {
    if (StringUtils.notBlank(codes)) {
      Arrays.stream(codes.split("\\s*,\\s*"))
          .forEach(
              code -> {
                Group group = groupRepo.findByCode(code);
                if (group == null) {
                  LOG.info("Creating a new user group: {}", code);
                  group = groupRepo.save(new Group(code, code));
                }
                view.addGroup(group);
              });
    }
  }

  @Nullable
  private String getLastModule() {
    for (final ListIterator<MetaView> it = extendsViews.listIterator(extendsViews.size());
        it.hasPrevious(); ) {
      final String module = it.previous().getModule();

      if (StringUtils.notBlank(module)) {
        return module;
      }
    }

    return null;
  }

  private MetaView getComputedView() {
    final Query<MetaView> computedViewQuery =
        metaViewRepo
            .all()
            .filter("self.name = :name AND self.computed = TRUE")
            .bind("name", baseView.getName());

    if (ObjectUtils.isEmpty(extendsViews)) {
      computedViewQuery.remove();
      LOG.debug(
          "No extends views found for {}: remove existing computed views.", baseView.getName());
      return null;
    }

    final MetaView computedView;
    final Iterator<MetaView> computedViewIt =
        computedViewQuery.order("xmlId").fetchStream().iterator();
    if (computedViewIt.hasNext()) {
      computedView = computedViewIt.next();
      while (computedViewIt.hasNext()) {
        metaViewRepo.remove(computedViewIt.next());
      }
    } else {
      final MetaView copy = metaViewRepo.copy(baseView, false);
      final String xmlId =
          MoreObjects.firstNonNull(baseView.getXmlId(), baseView.getName()) + "__computed__";
      copy.setXmlId(xmlId);
      copy.setComputed(true);
      computedView = metaViewRepo.save(copy);
    }

    computedView.setPriority(baseView.getPriority() + 1);

    return computedView;
  }

  public String getDependentModules() {
    return iterableToString(processor.getDependentModules());
  }

  public String getDependentFeatures() {
    return iterableToString(processor.getDependentFeatures());
  }

  private String iterableToString(Iterable<? extends CharSequence> elements) {
    return String.join(STRING_DELIMITER, elements);
  }
}
