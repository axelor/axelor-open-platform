/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.text;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.ResourceUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.script.GroovySandbox;
import com.axelor.script.GroovyScriptSupport;
import com.axelor.script.ScriptBindings;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.io.CharStreams;
import groovy.text.TemplateEngine;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The implementation of {@link Templates} for groovy string template support. */
public class GroovyTemplates implements Templates {

  private static final TemplateEngine GSTRING_ENGINE =
      GroovyScriptSupport.createStringTemplateEngine();

  private static final TemplateEngine STREAMING_ENGINE =
      GroovyScriptSupport.createStreamingTemplateEngine();

  private static final String DEFAULT_TEMPLATE_DIR = "{java.io.tmpdir}/axelor/templates";

  /** The folder of the files that templates can include ({@code template.search-dir}). */
  static final Path TEMPLATE_DIR =
      Paths.get(
              AppSettings.get()
                  .getPath(AvailableAppSettings.TEMPLATE_SEARCH_DIR, DEFAULT_TEMPLATE_DIR))
          .toAbsolutePath()
          .normalize();

  /** The classpath folder of the resources that templates can include. */
  static final String INCLUDE_RESOURCE_DIR = "templates/";

  /** The extension of the classpath resources that templates can include. */
  static final String INCLUDE_RESOURCE_EXTENSION = ".tmpl";

  private static final Pattern INCLUDE_PAT = Pattern.compile("\\{\\{\\<\\s*(.*?)\\s*\\>\\}\\}");

  private static final int DEFAULT_CACHE_SIZE = 500;
  private static final int DEFAULT_CACHE_EXPIRE_TIME = 60;

  /**
   * The compiled templates, by engine and text, with the settings of the script cache: compiling
   * takes almost all the time of a rendering (milliseconds, against microseconds to render a
   * compiled template).
   */
  private static final Cache<TemplateKey, groovy.text.Template> TEMPLATE_CACHE =
      Caffeine.newBuilder()
          .maximumSize(
              cacheSetting(AvailableAppSettings.APPLICATION_SCRIPT_CACHE_SIZE, DEFAULT_CACHE_SIZE))
          .expireAfterAccess(
              cacheSetting(
                  AvailableAppSettings.APPLICATION_SCRIPT_CACHE_EXPIRE_TIME,
                  DEFAULT_CACHE_EXPIRE_TIME),
              TimeUnit.MINUTES)
          .build();

  private record TemplateKey(TemplateEngine engine, String text) {}

  /** Reads a setting of the script cache as {@link GroovyScriptSupport} does. */
  private static int cacheSetting(String key, int defaultValue) {
    int value = 0;
    try {
      value = Integer.parseInt(System.getProperty(key));
    } catch (Exception e) {
    }
    return value > 0 ? value : defaultValue;
  }

  /**
   * Returns the template compiled from the text by the engine, compiled once and then cached.
   *
   * <p>A compiled template can be rendered many times, also concurrently: each rendering clones its
   * closures and gives them its own bindings, which also receive the variables the template
   * assigns.
   */
  static groovy.text.Template compile(TemplateEngine engine, String text)
      throws ClassNotFoundException, IOException {
    TemplateKey key = new TemplateKey(engine, text);
    groovy.text.Template template = TEMPLATE_CACHE.getIfPresent(key);
    if (template == null) {
      template = engine.createTemplate(text);
      TEMPLATE_CACHE.put(key, template);
    }
    return template;
  }

  /**
   * Checks an include name, and returns it with {@code /} as separator ({@code \}, the separator on
   * Windows, is converted), so that the same name works on every system.
   *
   * <p>Rejects a name that could reach outside of the template folder or of the {@code templates}
   * resources: it must be a relative path of names, without {@code .} or {@code ..}, or colon (URL
   * schemes such as {@code classpath:}, drive letters).
   */
  static String checkIncludeName(String name) {
    String path = name.replace('\\', '/');
    boolean valid =
        path.indexOf(':') < 0
            && path.indexOf('\0') < 0
            && Arrays.stream(path.split("/", -1))
                .noneMatch(part -> part.isEmpty() || ".".equals(part) || "..".equals(part));
    if (!valid) {
      throw new IllegalArgumentException(String.format("Invalid template include: '%s'", name));
    }
    return path;
  }

  class GroovyTemplate implements Template {

    private String text;

    public GroovyTemplate(String text) {
      this.text = text;
    }

    private boolean isWordTemplate(String text) {
      if (StringUtils.isBlank(text)) return false;
      return text.indexOf("<?mso-application") > -1;
    }

    /**
     * Reads an included template ({@code {{< name >}}}): the file of that name in the template
     * folder ({@code template.search-dir}), or else the {@code .tmpl} resource of that name in the
     * {@code templates} folder of the classpath.
     *
     * @throws IllegalArgumentException if the name is not valid (see {@link
     *     GroovyTemplates#checkIncludeName(String)}), or there is no such template
     */
    private String read(String included) throws IOException {
      String name = checkIncludeName(included);

      Path file = TEMPLATE_DIR.resolve(name).normalize();
      if (file.startsWith(TEMPLATE_DIR) && Files.isRegularFile(file)) {
        try (Reader reader = new FileReader(file.toFile())) {
          return CharStreams.toString(reader);
        }
      }

      InputStream stream =
          name.endsWith(INCLUDE_RESOURCE_EXTENSION)
              ? ResourceUtils.getResourceStream(INCLUDE_RESOURCE_DIR + name)
              : null;
      if (stream == null) {
        // the setting rather than the folder: the message can reach users
        throw new IllegalArgumentException(
            String.format(
                "Template include not found: '%s' (in template.search-dir, or as a .tmpl resource"
                    + " in templates/)",
                included));
      }
      try (Reader reader = new InputStreamReader(stream)) {
        return CharStreams.toString(reader);
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
          throw new UncheckedIOException(e);
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

      try {
        final groovy.text.Template template = compile(engine, text);
        return new Renderer() {

          @Override
          public void render(Writer out) throws IOException {
            try (GroovySandbox.Scope scope = GroovySandbox.enter()) {
              template.make(bindings).writeTo(out);
            }
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
