/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.tools.code.entity;

import com.axelor.tools.code.JavaFile;
import com.axelor.tools.code.JavaType;
import com.axelor.tools.code.entity.model.BaseType;
import com.axelor.tools.code.entity.model.Entity;
import com.axelor.tools.code.entity.model.EnumType;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityGenerator {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private File domainPath;

  private File outputPath;

  private final Set<String> definedEntities = new HashSet<>();
  private final Set<String> definedEnums = new HashSet<>();

  private final List<EntityGenerator> lookup = new ArrayList<>();
  private final Function<String, String> formatter;

  private final Multimap<String, Entity> entities = LinkedHashMultimap.create();
  private final Multimap<String, EnumType> enums = LinkedHashMultimap.create();

  private static final Map<String, Entity> mergedEntities = new HashMap<>();
  private static final Set<String> MODEL_FIELD_NAMES = ImmutableSet.of("archived");
  private static final Set<String> AUDITABLE_MODEL_FIELD_NAMES =
      ImmutableSet.of("createdOn", "updatedOn", "createdBy", "updatedBy");

  public EntityGenerator(File domainPath, File outputPath) {
    this(domainPath, outputPath, String -> String);
  }

  public EntityGenerator(File domainPath, File outputPath, Function<String, String> formatter) {
    this.domainPath = domainPath;
    this.outputPath = outputPath;
    this.formatter = Objects.requireNonNull(formatter);
  }

  private List<File> renderEnum(Collection<EnumType> items, boolean doLookup) throws IOException {

    if (items == null || items.isEmpty()) {
      return null;
    }

    final List<EnumType> all = new ArrayList<>(items);
    final EnumType first = all.get(0);

    final String ns = first.getPackageName();
    final String name = first.getName();

    // prepend all lookup entities
    if (doLookup) {
      for (EntityGenerator gen : lookup) {
        if (gen.definedEnums.contains(name)) {
          if (gen.enums.isEmpty()) {
            gen.processAll(false);
          }
          all.addAll(0, gen.enums.get(name));
        }
      }
    }

    // check that all entities have same namespace
    for (EnumType it : all) {
      if (!ns.equals(it.getPackageName())) {
        throw new IllegalArgumentException(
            String.format(
                "Invalid namespace: %s.%s != %s.%s", ns, name, it.getPackageName(), name));
      }
    }

    final EnumType entity = all.remove(0);

    for (EnumType it : all) {
      entity.merge(it);
    }

    final JavaFile javaFile = new JavaFile(entity.getPackageName(), entity.toJavaClass());

    return List.of(save(javaFile));
  }

  private File save(JavaFile javaFile) throws IOException {
    final File outFile = javaFile.getPath(outputPath.toPath()).toFile();

    Files.createDirectories(outFile.getParentFile().toPath());

    if (outFile.exists()) {
      outFile.delete();
    }

    log.info("Generating: " + outFile);

    writeTo(outFile, javaFile);

    return outFile;
  }

  private List<File> render(Collection<Entity> items, boolean doLookup) throws IOException {

    if (items == null || items.isEmpty()) {
      return null;
    }

    final List<Entity> all = new ArrayList<>(items);
    final Entity first = all.get(0);

    final String ns = first.getPackageName();
    final String name = first.getName();

    // prepend all lookup entities
    if (doLookup) {
      for (EntityGenerator gen : lookup) {
        if (gen.definedEntities.contains(name)) {
          if (gen.entities.isEmpty()) {
            gen.processAll(false);
          }
          all.addAll(0, gen.entities.get(name));
        }
      }
    }

    // check that all entities have same namespace
    for (Entity it : all) {
      if (!ns.equals(it.getPackageName())) {
        throw new IllegalArgumentException(
            String.format(
                "Invalid namespace: %s.%s != %s.%s", ns, name, it.getPackageName(), name));
      }
    }

    final Entity entity = all.remove(0);
    for (Entity it : all) {
      entity.merge(it);
    }
    mergedEntities.put(entity.getName(), entity);

    Optional.ofNullable(entity.getTrack())
        .ifPresent(
            track ->
                track.getFields().stream()
                    .map(field -> field.getName())
                    .filter(fieldName -> !fieldExists(entity.getName(), fieldName))
                    .forEach(
                        fieldName ->
                            log.error("{}: track unknown field: {}", entity.getName(), fieldName)));

    final JavaType javaType = entity.toJavaClass();
    final JavaType repoType = entity.toRepoClass();

    final List<File> rendered = new ArrayList<>();

    if (javaType != null) {
      rendered.add(save(new JavaFile(entity.getPackageName(), javaType)));
    }

    if (repoType != null) {
      rendered.add(save(new JavaFile(entity.getRepoPackage(), repoType)));
    }

    return rendered;
  }

  private boolean fieldExists(String entityName, String fieldName) {
    Entity itEntity;
    String itEntityName = entityName;
    do {
      itEntity = mergedEntities.get(itEntityName);
      if (itEntity == null) {
        if ("AuditableModel".equals(itEntityName)
            && AUDITABLE_MODEL_FIELD_NAMES.contains(fieldName)) {
          return true;
        }
        return MODEL_FIELD_NAMES.contains(fieldName);
      }
      if (itEntity.findField(fieldName) != null) {
        return true;
      }
    } while ((itEntityName = itEntity.getSuperClass()) != null);

    return false;
  }

  protected void writeTo(File output, JavaFile content) throws IOException {
    try (Writer writer = new FileWriter(output, StandardCharsets.UTF_8)) {
      content.writeTo(writer, formatter);
    }
  }

  protected void findFrom(File input) throws IOException {
    final List<BaseType<?>> types;
    try {
      types = EntityParser.parse(input);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }

    types.stream()
        .filter(EnumType.class::isInstance)
        .map(BaseType::getName)
        .forEach(definedEnums::add);

    types.stream()
        .filter(Entity.class::isInstance)
        .map(BaseType::getName)
        .forEach(definedEntities::add);
  }

  protected void findAll() throws IOException {
    if (!domainPath.exists()) return;
    for (File file : domainPath.listFiles()) {
      if (file.getName().endsWith(".xml")) {
        findFrom(file);
      }
    }
  }

  protected void process(File input, boolean verbose) throws IOException {
    if (verbose) {
      log.info("Processing: " + input);
    }
    try {
      for (BaseType<?> type : EntityParser.parse(input)) {
        if (type instanceof Entity) entities.put(type.getName(), (Entity) type);
        if (type instanceof EnumType) enums.put(type.getName(), (EnumType) type);
      }
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  protected void processAll(boolean verbose) throws IOException {
    if (!domainPath.exists()) return;
    for (File file : domainPath.listFiles()) {
      if (file.getName().endsWith(".xml")) {
        process(file, verbose);
      }
    }
  }

  private void delete(File file) {
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        delete(f);
      }
    }
    file.delete();
  }

  public void addLookupSource(EntityGenerator generator) throws IOException {
    if (generator == null) return;
    if (generator.definedEntities.isEmpty()) {
      generator.findAll();
    }
    lookup.add(0, generator);
  }

  public void clean() {

    if (!this.outputPath.exists()) return;

    log.info("Cleaning generated sources.");
    log.info("Output path: " + outputPath);

    for (File file : outputPath.listFiles()) {
      delete(file);
    }
  }

  public void start() throws IOException {

    log.info("Generating classes...");
    log.info("Domain path: " + domainPath);
    log.info("Output path: " + outputPath);

    outputPath.mkdirs();

    final Set<File> generated = new HashSet<>();

    if (this.domainPath.exists()) {
      for (File file : domainPath.listFiles()) {
        if (file.getName().endsWith(".xml")) {
          process(file, true);
        }
      }
    }

    // generate enums
    for (String name : enums.keySet()) {
      final List<File> rendered = renderEnum(enums.get(name), true);
      if (rendered != null) {
        generated.addAll(rendered);
      }
    }

    // make sure to generate extended enums from parent modules
    final Multimap<String, EnumType> extendedEnums = LinkedHashMultimap.create();
    for (EntityGenerator generator : lookup) {
      for (String name : generator.definedEnums) {
        if (enums.containsKey(name)) {
          continue;
        }
        if (generator.enums.isEmpty()) {
          generator.processAll(false);
        }
        extendedEnums.putAll(name, generator.enums.get(name));
      }
    }
    for (String name : extendedEnums.keySet()) {
      final List<EnumType> all = new ArrayList<>(extendedEnums.get(name));
      if (all == null || all.size() < 2) {
        continue;
      }
      Collections.reverse(all);
      final List<File> rendered = renderEnum(all, false);
      if (rendered != null) {
        generated.addAll(rendered);
      }
    }

    // generate entities
    for (String name : entities.keySet()) {
      final List<File> rendered = render(entities.get(name), true);
      if (rendered != null) {
        generated.addAll(rendered);
      }
    }

    // make sure to generate extended entities from parent modules
    final Multimap<String, Entity> extendedEntities = LinkedHashMultimap.create();
    for (EntityGenerator generator : lookup) {
      for (String name : generator.definedEntities) {
        if (entities.containsKey(name)) {
          continue;
        }
        if (generator.entities.isEmpty()) {
          generator.processAll(false);
        }
        extendedEntities.putAll(name, generator.entities.get(name));
      }
    }
    for (String name : extendedEntities.keySet()) {
      final List<Entity> all = new ArrayList<>(extendedEntities.get(name));
      if (all == null || all.isEmpty()) {
        continue;
      }
      if (all.size() == 1 && !all.get(0).isModelClass()) { // generate extended Model class in root
        continue;
      }
      Collections.reverse(all);
      final List<File> rendered = render(all, false);
      if (rendered != null) {
        generated.addAll(rendered);
      }
    }

    // clean up obsolete files
    try (Stream<Path> walk = java.nio.file.Files.walk(outputPath.toPath())) {
      walk.map(Path::toFile)
          .filter(f -> f.getName().endsWith(".java") || f.getName().endsWith(".groovy"))
          .filter(f -> !generated.contains(f))
          .forEach(
              f -> {
                log.info("Deleting obsolete file: {}", f);
                f.delete();
              });
    }
  }

  /**
   * Get a {@link EntityGenerator} instance for the given source files.
   *
   * <p>Used by code generator task to add lookup source to core modules.
   *
   * @param files input files
   * @return a {@link EntityGenerator} instance
   */
  public static EntityGenerator forFiles(final Collection<File> files) {
    if (files == null || files.isEmpty()) {
      return null;
    }
    final EntityGenerator gen =
        new EntityGenerator(null, null) {

          @Override
          public void start() throws IOException {}

          @Override
          public void clean() {}

          @Override
          public void addLookupSource(EntityGenerator generator) throws IOException {}

          @Override
          protected void processAll(boolean verbose) throws IOException {
            for (File file : files) {
              process(file, verbose);
            }
          }
        };
    for (File file : files) {
      try {
        gen.findFrom(file);
      } catch (IOException e) {
      }
    }
    return gen;
  }
}
