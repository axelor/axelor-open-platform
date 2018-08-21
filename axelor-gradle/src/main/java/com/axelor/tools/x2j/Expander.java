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
package com.axelor.tools.x2j;

import com.axelor.tools.x2j.pojo.Entity;
import com.axelor.tools.x2j.pojo.EnumType;
import com.axelor.tools.x2j.pojo.Repository;
import com.google.common.collect.Maps;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import org.codehaus.groovy.control.CompilationFailedException;

final class Expander {

  private static final GStringTemplateEngine engine = new GStringTemplateEngine();

  private Template pojoTemplate;

  private Template bodyTemplate;

  private Template headTemplate;

  private Template enumTemplate;

  private Template enumBodyTemplate;

  private Template enumHeadTemplate;

  private Template repoTemplate;

  private Template repoBodyTemplate;

  private Template repoHeadTemplate;

  private static Expander instance;

  private Expander() {
    pojoTemplate = template("templates/pojo.template");
    headTemplate = template("templates/head.template");
    bodyTemplate = template("templates/body.template");
    enumTemplate = template("templates/enum.template");
    enumHeadTemplate = template("templates/enum-head.template");
    enumBodyTemplate = template("templates/enum-body.template");
    repoTemplate = template("templates/repo.template");
    repoHeadTemplate = template("templates/repo-head.template");
    repoBodyTemplate = template("templates/repo-body.template");
  }

  public static Expander getInstance() {
    if (instance == null) {
      instance = new Expander();
    }
    return instance;
  }

  public static String expand(Entity entity, boolean repo) {
    if (repo) {
      return Expander.getInstance().toRepositoryString(entity);
    }
    return Expander.getInstance().toEntityString(entity);
  }

  public static String expand(EnumType entity) {
    return Expander.getInstance().toEnumString(entity);
  }

  private Reader read(String resource) {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    return new BufferedReader(new InputStreamReader(stream));
  }

  private Template template(String resource) {
    try {
      return engine.createTemplate(read(resource));
    } catch (CompilationFailedException | ClassNotFoundException | IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private String toEnumString(EnumType entity) {

    final Map<String, Object> binding = Maps.newHashMap();

    binding.put("pojo", entity);

    final String body = enumBodyTemplate.make(binding).toString();
    final String imports = enumHeadTemplate.make(binding).toString();

    binding.put("namespace", entity.getNamespace());
    binding.put("body", body);
    binding.put("imports", imports);

    return enumTemplate.make(binding).toString();
  }

  private String toEntityString(Entity entity) {

    final Map<String, Object> binding = Maps.newHashMap();

    binding.put("pojo", entity);

    final String body = bodyTemplate.make(binding).toString();
    final String imports = headTemplate.make(binding).toString();

    binding.put("namespace", entity.getNamespace());
    binding.put("body", body);
    binding.put("imports", imports);

    return pojoTemplate.make(binding).toString();
  }

  private String toRepositoryString(Entity entity) {

    final Map<String, Object> binding = Maps.newHashMap();
    final Repository repo = entity.getRepository();

    binding.put("repo", repo);

    final String body = repoBodyTemplate.make(binding).toString();
    final String imports = repoHeadTemplate.make(binding).toString();

    binding.put("body", body);
    binding.put("imports", imports);

    return repoTemplate.make(binding).toString();
  }
}
