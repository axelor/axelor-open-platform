/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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

  public Boolean getAttachment() {
    return attachment;
  }

  public List<Export> getExports() {
    return exports;
  }

  protected PendingExport doExport(Export export, ActionHandler handler) throws IOException {
    String templatePath = handler.evaluate(export.template).toString();

    try (Reader reader = createTemplateReader(templatePath)) {
      String name = resolveExportName(export, handler);
      log.info("export {} as {}", templatePath, name);

      String contents = handler.template(createTemplateEngine(export), reader);

      InputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
      return new PendingExport(stream, name);
    }
  }

  /**
   * Create a template engine based on the export engine.
   *
   * @param export the export definition
   * @return the template engine
   */
  private Templates createTemplateEngine(Export export) {
    if ("groovy".equals(export.engine)) {
      return new GroovyTemplates();
    }

    return new StringTemplates('$', '$');
  }

  /**
   * Resolve the export name.
   *
   * @param export the export definition
   * @param handler the action handler
   * @return the resolved export name
   */
  private String resolveExportName(Export export, ActionHandler handler) {
    String name = export.getName();

    if (name.indexOf("$") > -1 || (name.startsWith("#{") && name.endsWith("}"))) {
      name = handler.evaluate(toExpression(name, true)).toString();
    }

    return FileUtils.safeFileName(name);
  }

  /**
   * Find the template file.
   *
   * @param templatePath the template path
   * @return the template file
   */
  private File findTemplateFile(String templatePath) {
    Store store = FileStoreFactory.getStore();

    if (store.hasFile(templatePath)) {
      return store.getFile(templatePath);
    }

    // if not found, search the template directory
    return FileUtils.getFile(TEMPLATE_DIR, templatePath);
  }

  /**
   * Create the template reader.
   *
   * @param templatePath the template path
   * @return the template reader
   * @throws IOException if an I/O error occurs
   */
  private Reader createTemplateReader(String templatePath) throws IOException {
    File templateFile = findTemplateFile(templatePath);

    if (templateFile.isFile()) {
      return new FileReader(templateFile);
    }

    InputStream resourceStream = ResourceUtils.getResourceStream(templatePath);
    if (resourceStream == null) {
      throw new FileNotFoundException("No such template: " + templatePath);
    }

    return new InputStreamReader(resourceStream);
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
      InputStream exportStream = null;
      try {
        PendingExport pendingExport = doExport(export, handler);
        exportStream = pendingExport.stream();
        String exportName = pendingExport.name();
        result.put("exportFile", exportName);

        if (Boolean.TRUE.equals(getAttachment())) {
          Long id = (Long) handler.getContext().get("id");
          if (id != null) {
            Class<? extends Model> modelClass =
                handler.getContext().getContextClass().asSubclass(Model.class);
            Model model = JPA.em().find(modelClass, id);
            MetaFile attachedMetaFile = createAttachment(model, exportStream, exportName);
            result.put("attached", attachedMetaFile);
          }
        } else {
          String token = Beans.get(PendingExportService.class).add(exportStream);
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
        close(exportStream);
      }
    }
    return null;
  }

  private MetaFile createAttachment(Model model, InputStream exportStream, String fileName) {
    try {
      var dmsFile = Beans.get(MetaFiles.class).attach(exportStream, fileName, model);
      return dmsFile.getMetaFile();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void close(Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
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
