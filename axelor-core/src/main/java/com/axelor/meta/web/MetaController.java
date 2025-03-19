/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.meta.web;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.common.csv.CSVFile;
import com.axelor.db.JpaSecurity;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.i18n.I18n;
import com.axelor.i18n.I18nBundle;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaAttrs;
import com.axelor.meta.db.MetaField;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.MetaTheme;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaAttrsRepository;
import com.axelor.meta.db.repo.MetaThemeRepository;
import com.axelor.meta.db.repo.MetaTranslationRepository;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.actions.ActionExport;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.validate.ActionValidateBuilder;
import com.axelor.meta.schema.actions.validate.validator.ValidatorType;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.axelor.script.ScriptHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.bind.JAXBException;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaController {

  @Inject private ModuleManager moduleManager;

  @Inject private MetaTranslationRepository translations;

  private static final Logger log = LoggerFactory.getLogger(MetaController.class);

  private ObjectViews validateXML(String xml) {
    try {
      return XMLViews.fromXML(xml);
    } catch (JAXBException e) {
      String message = I18n.get("Invalid XML.");
      Throwable ex = e.getLinkedException();
      if (ex != null) {
        message = ex.getMessage().replaceFirst("[^:]+\\:(.*)", "$1");
      }
      throw new IllegalArgumentException(message);
    }
  }

  public void validateAction(ActionRequest request, ActionResponse response) {

    MetaAction meta = request.getContext().asType(MetaAction.class);

    Action action = XMLViews.findAction(meta.getName());
    Map<String, Map<String, String>> data = Maps.newHashMap();

    response.setData(ImmutableList.of(data));

    ObjectViews views;
    try {
      views = validateXML(meta.getXml());
    } catch (Exception e) {
      ActionValidateBuilder validateBuilder =
          new ActionValidateBuilder(ValidatorType.ERROR).setMessage(e.getMessage());
      data.putAll(validateBuilder.build());
      return;
    }

    Action current = views.getActions().get(0);
    if (action != null && !action.getName().equals(current.getName())) {
      ActionValidateBuilder validateBuilder =
          new ActionValidateBuilder(ValidatorType.ERROR)
              .setMessage(I18n.get("Action name can't be changed."));
      data.putAll(validateBuilder.build());
      return;
    }
  }

  public void validateView(ActionRequest request, ActionResponse response) {
    MetaView meta = request.getContext().asType(MetaView.class);
    Map<String, Object> data = Maps.newHashMap();

    try {
      validateXML(meta.getXml());
    } catch (Exception e) {
      ActionValidateBuilder validateBuilder =
          new ActionValidateBuilder(ValidatorType.ERROR).setMessage(e.getMessage());
      data.putAll(validateBuilder.build());
    }

    response.setData(ImmutableList.of(data));
  }

  private List<MetaAttrs> findAttrs(String model, String view) {
    String filter =
        StringUtils.isBlank(view)
            ? "self.model = :model and self.view is null"
            : "self.model = :model and self.view = :view";
    MetaAttrsRepository repo = Beans.get(MetaAttrsRepository.class);
    return repo.all().filter(filter).bind("model", model).bind("view", view).order("order").fetch();
  }

  public void moreAttrs(ActionRequest request, ActionResponse response) {
    Context ctx = request.getContext();
    String model = (String) ctx.get("_model");
    String view = (String) ctx.get("_viewName");

    User user = AuthUtils.getUser();
    ScriptHelper sh = request.getScriptHelper();
    List<MetaAttrs> attrs = new ArrayList<>(findAttrs(model, null));

    if (StringUtils.notBlank(view)) {
      attrs.addAll(findAttrs(model, view));
    }

    attrs.stream()
        // check roles
        .filter(
            attr ->
                attr.getRoles() == null
                    || attr.getRoles().isEmpty()
                    || attr.getRoles().stream()
                        .anyMatch(role -> AuthUtils.hasRole(user, role.getName())))
        // check conditions
        .filter(attr -> sh.test(attr.getCondition()))
        // set attrs
        .forEach(
            attr -> {
              final Object value =
                  attr.getName().matches("readonly|required|hidden|collapse|refresh|focus|active")
                      ? sh.test(attr.getValue())
                      : sh.eval(attr.getValue());
              response.setAttr(attr.getField(), attr.getName(), value);
            });
  }

  /** This action is called from custom fields form when context field is changed. */
  public void contextFieldChange(ActionRequest request, ActionResponse response) {
    final MetaJsonField jsonField = request.getContext().asType(MetaJsonField.class);
    final String modelName = jsonField.getModel();
    final String fieldName = jsonField.getContextField();

    final Class<?> modelClass;
    try {
      modelClass = Class.forName(modelName);
    } catch (ClassNotFoundException e) {
      // this should not happen
      response.setException(e);
      return;
    }

    final Mapper mapper = Mapper.of(modelClass);
    final Property property = mapper.getProperty(fieldName);
    final String target = property == null ? null : property.getTarget().getName();
    final String targetName = property == null ? null : property.getTargetName();

    response.setValue("contextFieldTarget", target);
    response.setValue("contextFieldTargetName", targetName);
    response.setValue("contextFieldValue", null);
    response.setValue("contextFieldTitle", null);
  }

  public void clearCache(ActionRequest request, ActionResponse response) {
    if (request.getBeanClass() != null && MetaView.class.isAssignableFrom(request.getBeanClass())) {
      final MetaView view = request.getContext().asType(MetaView.class);
      if (!Objects.equals(view.getType(), "grid")) {
        int deleted = Beans.get(MetaService.class).removeCustomViews(view);
        if (deleted > 0) {
          response.setNotify(
              I18n.get(
                  "{0} customized view is deleted.", "{0} customized views are deleted.", deleted));
        }
      }
    }
    MetaStore.clear();
  }

  public void removeUserCustomViews(ActionRequest request, ActionResponse response) {
    if (StringUtils.isBlank(request.getModel())) {
      request.setModel(MetaView.class.getName());
    }

    final MetaView view = request.getContext().asType(MetaView.class);
    Beans.get(MetaService.class).removeCustomViews(view, AuthUtils.getUser());
  }

  /**
   * Open ModelEntity of the relationship.
   *
   * @param request
   * @param response
   */
  public void openModel(ActionRequest request, ActionResponse response) {

    MetaField metaField = request.getContext().asType(MetaField.class);

    String domain =
        String.format(
            "self.packageName = '%s' AND self.name = '%s'",
            metaField.getPackageName(), metaField.getTypeName());
    response.setView(
        ActionView.define(metaField.getTypeName())
            .model(MetaModel.class.getName())
            .domain(domain)
            .map());
    response.setCanClose(true);
  }

  public void restoreAll(ActionRequest request, ActionResponse response) {
    try {
      final Instant startInstant = Instant.now();
      moduleManager.restoreMeta();
      MetaStore.clear();
      I18nBundle.invalidate();
      final Duration duration = Duration.between(startInstant, Instant.now());
      final String durationTime =
          LocalTime.MIN.plusSeconds(duration.getSeconds()).format(DateTimeFormatter.ISO_LOCAL_TIME);
      response.setNotify(
          String.format(I18n.get("All views have been restored (%s)."), durationTime)
              + "<br>"
              + I18n.get("Please refresh your browser to see updated views."));
      log.info("Restore meta time: {}", durationTime);
    } catch (Exception e) {
      response.setException(e);
    }
  }

  private void exportI18n(String module, URL file) throws IOException {

    String name = Paths.get(file.getFile()).getFileName().toString();
    if (!name.startsWith("messages_")) {
      return;
    }

    Path path = ActionExport.getExportPath().toPath().resolve("i18n");
    String lang = StringUtils.normalizeLanguageTag(name.substring(9, name.length() - 4));
    Path target = path.resolve(Paths.get(module, "src/main/resources/i18n", name));

    final List<String[]> items = new ArrayList<>();
    final CSVFile csv = CSVFile.DEFAULT.withFirstRecordAsHeader();

    try (CSVParser csvParser = csv.parse(file.openStream(), StandardCharsets.UTF_8)) {
      for (CSVRecord record : csvParser) {

        if (CSVFile.isEmpty(record)) {
          continue;
        }

        final Map<String, String> map = record.toMap();

        String key = map.get("key");
        String value = map.get("value");

        if (StringUtils.isBlank(key)) {
          continue;
        }

        MetaTranslation tr = translations.findByKey(key, lang);
        if (tr != null) {
          value = tr.getMessage();
        }
        String[] row = {key, value, map.get("comment"), map.get("context")};
        items.add(row);
      }
    }

    Files.createParentDirs(target.toFile());

    try (CSVPrinter printer = CSVFile.DEFAULT.withQuoteAll().write(target.toFile())) {
      printer.printRecord("key", "message", "comment", "context");
      printer.printRecords(items);
    }
  }

  public void exportI18n(ActionRequest request, ActionResponse response) {
    for (String module : ModuleManager.getResolution()) {
      for (URL file : MetaScanner.findAll(module, "i18n", "(.*?)\\.csv")) {
        try {
          exportI18n(module, file);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    response.setInfo(I18n.get("Export complete."));
  }

  public void setThemeSelectable(ActionRequest request, ActionResponse response) {
    MetaTheme theme = request.getContext().asType(MetaTheme.class);

    if (theme == null || theme.getId() == null) {
      return;
    }

    Beans.get(JpaSecurity.class).check(JpaSecurity.CAN_WRITE, MetaTheme.class, theme.getId());

    try {
      Beans.get(MetaService.class)
          .updateSelectableTheme(Beans.get(MetaThemeRepository.class).find(theme.getId()), true);
      response.setReload(true);
    } catch (Exception e) {
      response.setError(e.getMessage());
    }
  }

  public void setThemeUnSelectable(ActionRequest request, ActionResponse response) {
    MetaTheme theme = request.getContext().asType(MetaTheme.class);

    if (theme == null || theme.getId() == null) {
      return;
    }

    Beans.get(JpaSecurity.class).check(JpaSecurity.CAN_WRITE, MetaTheme.class, theme.getId());

    try {
      Beans.get(MetaService.class)
          .updateSelectableTheme(Beans.get(MetaThemeRepository.class).find(theme.getId()), false);
      response.setReload(true);
    } catch (Exception e) {
      response.setError(e.getMessage());
    }
  }
}
