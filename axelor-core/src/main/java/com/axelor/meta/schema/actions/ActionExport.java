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
import com.axelor.db.JPA;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.file.store.FileStoreFactory;
import com.axelor.file.store.Store;
import com.axelor.file.temp.TempFiles;
import com.axelor.inject.Beans;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.schema.actions.validate.ActionValidateBuilder;
import com.axelor.meta.schema.actions.validate.validator.ValidatorType;
import com.axelor.rpc.PendingExportService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlType
public class ActionExport extends Action {

  private static final String DEFAULT_TEMPLATE_DIR = "{java.io.tmpdir}/axelor/templates";
  private static final String TEMPLATE_DIR =
      AppSettings.get().getPath(AvailableAppSettings.TEMPLATE_SEARCH_DIR, DEFAULT_TEMPLATE_DIR);

  @XmlAttribute private Boolean attachment;

  @XmlElement(name = "export")
  private List<Export> exports;

  private static record PendingExport(Path path, String name) {}

  public Boolean getAttachment() {
    return attachment;
  }

  public List<Export> getExports() {
    return exports;
  }

  protected PendingExport doExport(Export export, ActionHandler handler) throws IOException {
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

      name = FileUtils.safeFileName(name);
      Path output = TempFiles.createTempFile("export-", null);

      String contents = null;

      contents = handler.template(engine, reader);

      Files.asCharSink(output.toFile(), StandardCharsets.UTF_8).write(contents);

      log.info("file saved: {}", output);

      return new PendingExport(output, name);
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

    for (Export export : exports) {
      if (!export.test(handler)) {
        continue;
      }
      final Map<String, Object> result = new HashMap<>();
      Path exportPath = null;
      try {
        PendingExport pendingExport = doExport(export, handler);
        exportPath = pendingExport.path();
        String exportName = pendingExport.name();
        result.put("exportFile", exportName);

        if (Boolean.TRUE.equals(getAttachment())) {
          Long id = (Long) handler.getContext().get("id");
          if (id != null) {
            Class<? extends Model> modelClass =
                handler.getContext().getContextClass().asSubclass(Model.class);
            Model model = JPA.em().find(modelClass, id);
            MetaFile attachedMetaFile = createAttachment(model, exportPath, exportName);
            result.put("attached", attachedMetaFile);
          }
        } else {
          String token = Beans.get(PendingExportService.class).add(exportPath);
          result.put("exportToken", token);
        }

        return result;
      } catch (Exception e) {
        log.error("error while exporting: ", e);
        ActionValidateBuilder validateBuilder =
            new ActionValidateBuilder(ValidatorType.ERROR).setMessage(e.getMessage());
        result.putAll(validateBuilder.build());
        return result;
      } finally {
        if (exportPath != null) {
          deleteIfExists(exportPath);
        }
      }
    }
    return null;
  }

  private MetaFile createAttachment(Model model, Path exportPath, String fileName) {
    try (var is = java.nio.file.Files.newInputStream(exportPath)) {
      var dmsFile = Beans.get(MetaFiles.class).attach(is, fileName, model);
      return dmsFile.getMetaFile();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private boolean deleteIfExists(Path path) {
    try {
      java.nio.file.Files.deleteIfExists(path);
      return true;
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
