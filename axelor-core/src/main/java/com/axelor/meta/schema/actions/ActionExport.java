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
package com.axelor.meta.schema.actions;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.FileUtils;
import com.axelor.common.ResourceUtils;
import com.axelor.i18n.I18n;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.schema.actions.validate.ActionValidateBuilder;
import com.axelor.meta.schema.actions.validate.validator.ValidatorType;
import com.axelor.text.GroovyTemplates;
import com.axelor.text.StringTemplates;
import com.axelor.text.Templates;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class ActionExport extends Action {

  private static final String DEFAULT_EXPORT_DIR = "{java.io.tmpdir}/axelor/data-export";
  private static final String DEFAULT_DIR = "${date}/${name}";

  @XmlAttribute(name = "output")
  private String output;

  @XmlAttribute(name = "download")
  private Boolean download;

  @XmlElement(name = "export")
  private List<Export> exports;

  public String getOutput() {
    return output;
  }

  public Boolean getDownload() {
    return download;
  }

  public List<Export> getExports() {
    return exports;
  }

  public static File getExportPath() {
    final String path =
        AppSettings.get().getPath(AvailableAppSettings.DATA_EXPORT_DIR, DEFAULT_EXPORT_DIR);
    return new File(path);
  }

  protected String doExport(String dir, Export export, ActionHandler handler) throws IOException {
    String templatePath = handler.evaluate(export.template).toString();

    Reader reader = null;

    try {
      File template = new File(templatePath);
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
      Files.asCharSink(output, Charsets.UTF_8).write(contents);

      log.info("file saved: {}", output);

      return FileUtils.getFile(dir, name).toString();
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  @Override
  public Object evaluate(ActionHandler handler) {
    log.info("action-export: {}", getName());

    String dir = output == null ? DEFAULT_DIR : output;

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
