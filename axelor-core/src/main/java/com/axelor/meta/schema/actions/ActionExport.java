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
package com.axelor.meta.schema.actions;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.FileUtils;
import com.axelor.common.ResourceUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.file.store.FileStoreFactory;
import com.axelor.file.store.Store;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.schema.actions.validate.ActionValidateBuilder;
import com.axelor.meta.schema.actions.validate.validator.ValidatorType;
import com.axelor.text.GroovyTemplates;
import com.axelor.text.StringTemplates;
import com.axelor.text.Templates;
import com.google.common.base.MoreObjects;
import com.google.common.io.Files;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlType
public class ActionExport extends Action {

  private static final String DEFAULT_EXPORT_DIR = "{java.io.tmpdir}/axelor/data-export";
  private static final String DEFAULT_DIR = "${date}/${name}";

  private static final String DEFAULT_TEMPLATE_DIR = "{java.io.tmpdir}/axelor/templates";
  private static final String TEMPLATE_DIR =
      AppSettings.get().getPath(AvailableAppSettings.TEMPLATE_SEARCH_DIR, DEFAULT_TEMPLATE_DIR);

  @XmlAttribute(name = "download")
  private Boolean download;

  @XmlAttribute private Boolean attachment;

  @XmlElement(name = "export")
  private List<Export> exports;

  public Boolean getDownload() {
    return download;
  }

  public Boolean getAttachment() {
    return attachment;
  }

  public List<Export> getExports() {
    return exports;
  }

  public static File getExportPath() {
    final Path exportPath =
        Path.of(AppSettings.get().get(AvailableAppSettings.DATA_EXPORT_DIR, DEFAULT_EXPORT_DIR));
    final String tenantId = TenantResolver.currentTenantIdentifier();
    if (StringUtils.isBlank(tenantId)) {
      return exportPath.toFile();
    }
    return exportPath.resolve(tenantId).toFile();
  }

  protected String doExport(String dir, Export export, ActionHandler handler) throws IOException {
    Store store = FileStoreFactory.getStore();
    String templatePath = handler.evaluate(export.template).toString();
    File template;

    if (store.hasFile(templatePath)) {
      template = store.getFile(templatePath);
    } else {
      // if not found, search the template directory
      template = FileUtils.getFile(TEMPLATE_DIR, templatePath);
    }

    Reader reader = null;

    try {
      if (template.isFile()) {
        reader = new FileReader(template);
      }

      if (reader == null) {
        InputStream is = ResourceUtils.getResourceStream(templatePath);
        if (is == null) {
          throw new FileNotFoundException("No such template: " + templatePath);
        }
        reader = new InputStreamReader(is);
      }

      String name = export.getName();
      if (name.indexOf("$") > -1 || (name.startsWith("#{") && name.endsWith("}"))) {
        name = handler.evaluate(toExpression(name, true)).toString();
      }

      log.info("export {} as {}", templatePath, name);

      Templates engine = new StringTemplates('$', '$');
      if ("groovy".equals(export.engine)) {
        engine = new GroovyTemplates();
      }

      File output = getExportPath();
      output = FileUtils.getFile(output, dir, name);

      String contents = null;

      contents = handler.template(engine, reader);

      Files.createParentDirs(output);
      Files.asCharSink(output, StandardCharsets.UTF_8).write(contents);

      log.info("file saved: {}", output);

      return FileUtils.getFile(dir, name).toString();
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  @Override
  protected void checkPermission(ActionHandler handler) {
    super.checkPermission(handler);

    if (Boolean.TRUE.equals(getAttachment())) {
      var id = (Long) handler.getContext().get("id");

      if (id != null) {
        var klass = handler.getContext().getContextClass().asSubclass(Model.class);
        handler.checkPermission(JpaSecurity.AccessType.READ, klass, id);
      }
    }
  }

  @Override
  public Object evaluate(ActionHandler handler) {
    log.info("action-export: {}", getName());

    String dir = DEFAULT_DIR;

    dir =
        dir.replace("${name}", getName())
            .replace("${date}", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
            .replace("${time}", LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
    dir = handler.evaluate(dir).toString();

    for (Export export : exports) {
      if (!export.test(handler)) {
        continue;
      }
      final Map<String, Object> result = new HashMap<>();
      try {
        String file = doExport(dir, export, handler);
        if (Boolean.TRUE.equals(getDownload())) {
          result.put("exportFile", file);
        }
        if (Boolean.TRUE.equals(getAttachment())) {
          Long id = (Long) handler.getContext().get("id");
          if (id != null) {
            Class<? extends Model> modelClass =
                handler.getContext().getContextClass().asSubclass(Model.class);
            Model model = JPA.em().find(modelClass, id);
            MetaFile attachedMetaFile = createAttachment(model, file);
            result.put("attached", attachedMetaFile);
          }
        }
        ActionValidateBuilder validateBuilder =
            new ActionValidateBuilder(ValidatorType.NOTIFY)
                .setMessage(I18n.get("Export complete."));
        result.putAll(validateBuilder.build());
        return result;
      } catch (Exception e) {
        log.error("error while exporting: ", e);
        ActionValidateBuilder validateBuilder =
            new ActionValidateBuilder(ValidatorType.ERROR).setMessage(e.getMessage());
        result.putAll(validateBuilder.build());
        return result;
      }
    }
    return null;
  }

  private MetaFile createAttachment(Model model, String pathName) {
    var path = getExportPath().toPath().resolve(pathName);

    try (var is = java.nio.file.Files.newInputStream(path)) {
      var fileName = path.getFileName().toString();
      var dmsFile = Beans.get(MetaFiles.class).attach(is, fileName, model);
      return dmsFile.getMetaFile();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @XmlType
  public static class Export extends Element {

    @XmlAttribute private String template;

    @XmlAttribute private String engine;

    @XmlAttribute(name = "processor")
    private String processor;

    public String getTemplate() {
      return template;
    }

    public String getEngine() {
      return engine;
    }

    public String getProcessor() {
      return processor;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(getClass())
          .add("name", getName())
          .add("template", template)
          .toString();
    }
  }
}
