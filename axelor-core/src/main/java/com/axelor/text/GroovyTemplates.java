/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.text;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.FileUtils;
import com.axelor.common.ResourceUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.script.GroovyScriptSupport;
import com.axelor.script.GroovyScriptSupport.PolicyChecker;
import com.axelor.script.ScriptBindings;
import com.google.common.io.CharStreams;
import groovy.text.TemplateEngine;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The implementation of {@link Templates} for groovy string template support. */
public class GroovyTemplates implements Templates {

  private static final TemplateEngine GSTRING_ENGINE =
      GroovyScriptSupport.createStringTemplateEngine();

  private static final TemplateEngine STREAMING_ENGINE =
      GroovyScriptSupport.createStreamingTemplateEngine();

  private static final String DEFAULT_TEMPLATE_DIR = "{java.io.tmpdir}/axelor/templates";
  private static final String TEMPLATE_DIR =
      AppSettings.get().getPath(AvailableAppSettings.TEMPLATE_SEARCH_DIR, DEFAULT_TEMPLATE_DIR);
  private static final Pattern INCLUDE_PAT = Pattern.compile("\\{\\{\\<\\s*(.*?)\\s*\\>\\}\\}");

  class GroovyTemplate implements Template {

    private String text;

    public GroovyTemplate(String text) {
      this.text = text;
    }

    private boolean isWordTemplate(String text) {
      if (StringUtils.isBlank(text)) return false;
      return text.indexOf("<?mso-application") > -1;
    }

    private String read(String included) throws IOException {

      Reader reader = null;
      File file = FileUtils.getFile(TEMPLATE_DIR, included);
      if (file.isFile()) {
        reader = new FileReader(file);
      } else {
        InputStream stream = ResourceUtils.getResourceStream(included);
        if (stream != null) {
          reader = new InputStreamReader(stream);
        }
      }

      if (reader == null) {
        return "";
      }

      try {
        return CharStreams.toString(reader);
      } finally {
        reader.close();
      }
    }

    private String process(String text) {
      if (StringUtils.isBlank(text)) {
        return "";
      }
      text =
          text.replaceAll(
              "\\$\\{\\s*(\\w+)(\\?)?\\.([^}]*?)\\s*\\|\\s*text\\s*\\}",
              "\\${__fmt__.text($1, '$3')}");
      text =
          text.replaceAll(
              "\\$\\{\\s*([^}]*?)\\s*\\|\\s*text\\s*\\}", "\\${__fmt__.text(it, '$1')}");
      text = text.replaceAll("\\$\\{\\s*([^}]*?)\\s*\\|\\s*e\\s*\\}", "\\${($1) ?: ''}");
      if (text.trim().startsWith("<?xml ")) {
        text = text.replaceAll("\\$\\{(.*?)\\}", "\\${__fmt__.escape($1)}");
      }

      StringBuilder builder = new StringBuilder();
      Matcher matcher = INCLUDE_PAT.matcher(text);
      int position = 0;
      while (matcher.find()) {
        builder.append(text.substring(position, matcher.start()));
        position = matcher.end();
        try {
          String include = read(matcher.group(1));
          builder.append(process(include));
        } catch (IOException e) {
        }
      }
      builder.append(text.substring(position));
      return builder.toString();
    }

    @Override
    public Renderer make(final Map<String, Object> context) {
      final ScriptBindings bindings = new ScriptBindings(context);
      final String text = process(this.text);
      final TemplateEngine engine = isWordTemplate(text) ? STREAMING_ENGINE : GSTRING_ENGINE;

      bindings.put("__fmt__", new FormatHelper());
      bindings.put(PolicyChecker.NAME, new PolicyChecker());

      try {
        final groovy.text.Template template = engine.createTemplate(text);
        return new Renderer() {

          @Override
          public void render(Writer out) throws IOException {
            template.make(bindings).writeTo(out);
          }
        };
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    @SuppressWarnings("serial")
    public <T extends Model> Renderer make(final T context) {
      final Mapper mapper =
          context == null ? null : Mapper.of(EntityHelper.getEntityClass(context));
      final Map<String, Object> ctx =
          new HashMap<>() {

            @Override
            public boolean containsKey(Object key) {
              return mapper != null && mapper.getProperty((String) key) != null;
            }

            @Override
            public Object get(Object key) {
              return mapper == null ? null : mapper.get(context, (String) key);
            }
          };
      return make(ctx);
    }
  }

  @Override
  public Template fromText(String text) {
    return new GroovyTemplate(text);
  }

  @Override
  public Template from(File file) throws IOException {
    return from(new FileReader(file));
  }

  @Override
  public Template from(Reader reader) throws IOException {
    return fromText(CharStreams.toString(reader));
  }
}
