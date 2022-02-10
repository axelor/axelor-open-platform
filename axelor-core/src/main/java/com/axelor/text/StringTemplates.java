/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.text;

import com.axelor.common.StringUtils;
import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.repo.MetaJsonFieldRepository;
import com.axelor.meta.db.repo.MetaJsonRecordRepository;
import com.axelor.rpc.Context;
import com.axelor.rpc.JsonContext;
import com.axelor.script.ScriptBindings;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.common.xml.XmlEscapers;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.stringtemplate.v4.AttributeRenderer;
import org.stringtemplate.v4.AutoIndentWriter;
import org.stringtemplate.v4.DateRenderer;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.NumberRenderer;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.StringRenderer;
import org.stringtemplate.v4.compiler.Bytecode;
import org.stringtemplate.v4.misc.MapModelAdaptor;
import org.stringtemplate.v4.misc.ObjectModelAdaptor;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

/** The implementation of {@link Templates} for the StringTemplate (ST4) support. */
public class StringTemplates implements Templates {

  class StrRenderer extends StringRenderer {

    @Override
    public String toString(Object o, String formatString, Locale locale) {
      final String str = (String) o;
      if (StringUtils.notBlank(formatString)) {
        if (formatString.endsWith("escape")) {
          return XmlEscapers.xmlAttributeEscaper().escape(str);
        }
        if (formatString.startsWith("selection:")) {
          return getSelectionTitle(formatString.substring(10).trim(), o);
        }
      }
      return super.toString(str, formatString, locale);
    }
  }

  class LocalDateRenderer implements AttributeRenderer {

    @Override
    public String toString(Object o, String formatString, Locale locale) {
      return StringUtils.isBlank(formatString)
          ? o.toString()
          : ((LocalDate) o).format(DateTimeFormatter.ofPattern(formatString));
    }
  }

  class LocalDateTimeRenderer implements AttributeRenderer {

    @Override
    public String toString(Object o, String formatString, Locale locale) {
      return StringUtils.isBlank(formatString)
          ? o.toString()
          : ((LocalDateTime) o).format(DateTimeFormatter.ofPattern(formatString));
    }
  }

  class LocalTimeRenderer implements AttributeRenderer {

    @Override
    public String toString(Object o, String formatString, Locale locale) {
      return StringUtils.isBlank(formatString)
          ? o.toString()
          : ((LocalTime) o).format(DateTimeFormatter.ofPattern(formatString));
    }
  }

  class DataAdapter extends ObjectModelAdaptor {

    private final MapModelAdaptor mapModelAdaptor;
    private final MetaJsonRecordRepository jsonRecords;

    public DataAdapter() {
      this.mapModelAdaptor = new MapModelAdaptor();
      this.jsonRecords = Beans.get(MetaJsonRecordRepository.class);
    }

    private Object format(Property field, Object value) {
      if (field == null) return value;

      if (StringUtils.notBlank(field.getSelection())) {
        return getSelectionTitle(field.getSelection(), value);
      }

      if (field.isEnum()) {
        try {
          return MetaStore.getSelectionList(field.getEnumType()).stream()
              .filter(x -> x.getValue().equals(value.toString()))
              .findFirst()
              .map(x -> translate(x.getTitle()))
              .orElseGet(() -> (String) value);
        } catch (NullPointerException e) {
          return value;
        }
      }

      return value;
    }

    private Object format(MetaJsonField field, Object value) {
      if (field == null || StringUtils.isBlank(field.getSelection())) {
        return value;
      }
      return getSelectionTitle(field.getSelection(), value);
    }

    private MetaJsonField findCustomField(Class<?> entityClass, String name, String modelField) {
      return Beans.get(MetaJsonFieldRepository.class)
          .all()
          .filter("self.model = :model and self.name = :name and self.modelField = :modelField")
          .bind("model", entityClass.getName())
          .bind("name", name)
          .bind("modelField", modelField)
          .fetchOne();
    }

    private MetaJsonField findCustomField(String jsonModel, String name) {
      return Beans.get(MetaJsonFieldRepository.class)
          .all()
          .filter("self.jsonModel.name = :model and self.name = :name")
          .bind("model", jsonModel)
          .bind("name", name)
          .fetchOne();
    }

    private Object handle(Model entity, String name) {
      final Class<?> klass = EntityHelper.getEntityClass(entity);
      final Mapper mapper = Mapper.of(klass);
      final Property field = mapper.getProperty(name);

      // custom field?
      if (field == null) {
        final MetaJsonField jsonField = findCustomField(klass, name, "attrs");
        if (jsonField != null) {
          final Context ctx = new Context(entity.getId(), klass);
          ctx.put("attrs", mapper.get(entity, "attrs"));
          return handle(ctx, name);
        }
        return null;
      }

      if (field.isJson()) {
        final Context context = new Context(Mapper.toMap(entity), klass);
        return new JsonContext(context, field, (String) context.get(name));
      }

      return format(field, field.get(entity));
    }

    private Object handle(Context context, String key) {
      final Class<?> klass = context.getContextClass();
      if (klass == null) {
        return context.get(key);
      }

      final Object value = context.get(key);
      final Object jsonModel = context.get("jsonModel");

      // custom model?
      if (jsonModel instanceof String && MetaJsonRecord.class.isAssignableFrom(klass)) {
        final MetaJsonField field = findCustomField((String) jsonModel, key);
        return format(field, value);
      }

      final Property field = Mapper.of(klass).getProperty(key);

      // custom field?
      if (field == null) {
        return format(findCustomField(klass, key, "attrs"), value);
      }

      if (field.isJson()) {
        return new JsonContext(context, field, (String) value);
      }

      return format(field, value);
    }

    private Object handle(MetaJsonRecord record, String name) {
      Context context = jsonRecords.create(record);
      context.put("attrs", record.getAttrs());
      return handle(context, name);
    }

    private Object handle(JsonContext jsonContext, String name) {
      MetaJsonField customField =
          findCustomField(jsonContext.getContextClass(), name, jsonContext.getJsonField());
      return format(customField, jsonContext.get(name));
    }

    @Override
    public Object getProperty(
        Interpreter interp, ST self, Object o, Object property, String propertyName)
        throws STNoSuchPropertyException {
      if (o instanceof Context) return handle((Context) o, propertyName);
      if (o instanceof JsonContext) return handle((JsonContext) o, propertyName);
      if (o instanceof MetaJsonRecord) return handle((MetaJsonRecord) o, propertyName);
      if (o instanceof Model) return handle((Model) o, propertyName);
      if (o instanceof Map) {
        return mapModelAdaptor.getProperty(interp, self, o, property, propertyName);
      }
      return super.getProperty(interp, self, o, property, propertyName);
    }
  }

  class StringTemplate implements Template {

    private ST template;
    private Set<String> names;
    private Locale locale;

    private StringTemplate(ST template, Locale locale) {
      this.template = template;
      this.names = findAttributes();
      this.locale = locale == null ? Locale.getDefault() : locale;
    }

    private Set<String> findAttributes() {
      Set<String> names = Sets.newHashSet();
      int ip = 0;
      while (ip < template.impl.codeSize) {
        int opcode = template.impl.instrs[ip];
        Bytecode.Instruction I = Bytecode.instructions[opcode];
        ip++;
        for (int opnd = 0; opnd < I.nopnds; opnd++) {
          if (opcode == Bytecode.INSTR_LOAD_ATTR) {
            int nameIndex = Interpreter.getShort(template.impl.instrs, ip);
            if (nameIndex < template.impl.strings.length) {
              names.add(template.impl.strings[nameIndex]);
            }
          }
          ip += Bytecode.OPND_SIZE_IN_BYTES;
        }
      }
      return names;
    }

    @Override
    public Renderer make(Map<String, Object> context) {
      return new Renderer() {
        @Override
        public void render(Writer out) throws IOException {
          final ScriptBindings vars = new ScriptBindings(context);
          for (String name : names) {
            try {
              Object value = vars.get(name);
              if (context instanceof Context) {
                Object jsonContext = buildJsonContext((Context) context, name);
                if (jsonContext != null) {
                  value = jsonContext;
                }
              }
              template.add(name, value);
            } catch (Exception e) {
            }
          }
          try {
            template.write(new AutoIndentWriter(out), locale);
          } catch (Exception e) {
          }
        }
      };
    }

    @Override
    public <T extends Model> Renderer make(T context) {
      if (context != null) {
        return make(new Context(Mapper.toMap(context), EntityHelper.getEntityClass(context)));
      }
      return make(new HashMap<>());
    }
  }

  private Object buildJsonContext(Context context, String propertyName) {
    if (context == null || propertyName == null) {
      return null;
    }

    Mapper mapper = Mapper.of(context.getContextClass());
    Property property = mapper.getProperty(propertyName);
    if (property == null || !property.isJson()) {
      return null;
    }

    return new JsonContext(context, property, (String) context.get(propertyName));
  }

  private static final char DEFAULT_START_DELIMITER = '<';
  private static final char DEFAULT_STOP_DELIMITER = '>';

  private final STGroup group;

  private Locale locale;

  public StringTemplates() {
    this(DEFAULT_START_DELIMITER, DEFAULT_STOP_DELIMITER);
  }

  public StringTemplates(char delimiterStartChar, char delimiterStopChar) {
    this.group =
        new STGroup(delimiterStartChar, delimiterStopChar) {
          {
            adaptors.remove(Map.class);
          }
        };

    // Custom renderers
    this.group.registerRenderer(String.class, new StrRenderer());
    this.group.registerRenderer(LocalDate.class, new LocalDateRenderer());
    this.group.registerRenderer(LocalDateTime.class, new LocalDateTimeRenderer());
    this.group.registerRenderer(LocalTime.class, new LocalTimeRenderer());
    this.group.registerModelAdaptor(Object.class, new DataAdapter());

    // Other renderers provide by ST
    this.group.registerRenderer(Number.class, new NumberRenderer());
    this.group.registerRenderer(Date.class, new DateRenderer());
  }

  public StringTemplates withLocale(Locale locale) {
    this.locale = locale;
    return this;
  }

  @Override
  public Template fromText(String text) {
    ST template = new ST(group, text);
    return new StringTemplate(template, locale);
  }

  @Override
  public Template from(File file) throws IOException {
    return from(new FileReader(file));
  }

  @Override
  public Template from(Reader reader) throws IOException {
    return fromText(CharStreams.toString(reader));
  }

  private String translate(String value) {
    if (locale == null || StringUtils.isBlank(value)) {
      return value;
    }
    return I18n.getBundle(locale).getString(value);
  }

  private String valueOf(Object value) {
    if (value == null) return "";
    return String.valueOf(value);
  }

  private String getSelectionTitle(String selection, Object value) {
    final String val = valueOf(value);
    if (StringUtils.isBlank(val)) return val;
    try {
      return translate(MetaStore.getSelectionItem(selection, val).getTitle());
    } catch (Exception e) {
      return val;
    }
  }
}
