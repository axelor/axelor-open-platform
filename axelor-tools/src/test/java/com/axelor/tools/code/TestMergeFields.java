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
package com.axelor.tools.code;

import com.axelor.common.FileUtils;
import com.axelor.db.annotations.Widget;
import com.axelor.tools.code.entity.EntityGenerator;
import org.hibernate.annotations.Type;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMergeFields {

    private static final Path srcGenPath = Path.of("build/src-gen");
    private static final Path targetPath = srcGenPath.resolve("com/axelor/merge");

    private static ClassLoader classLoader;

    @BeforeAll
    public static void generateCode() throws IOException {
        clean();
        generate();
        compile();
    }

    @Test
    public void testMerge() throws Exception {
        String fieldName = "myString";
        assertEquals("My String", getAnnotation(fieldName, Widget.class).title());
        assertEquals(10, getAnnotation(fieldName, Size.class).max());
        assertNotNull(getAnnotation(fieldName, NotNull.class));
        assertEquals("AAA", getField(fieldName).get(loadClass().getDeclaredConstructor().newInstance()));
    }

    @Test
    public void testOverride() throws Exception {
        String fieldName = "myString2";

        assertEquals("Test New", getAnnotation(fieldName, Widget.class).title());
        assertNull(getAnnotation(fieldName, Size.class));
        assertEquals("BBB", getField(fieldName).get(loadClass().getDeclaredConstructor().newInstance()));
        assertNull(getAnnotation(fieldName, NotNull.class));
        assertFalse(getAnnotation(fieldName, Widget.class).hidden());
        assertArrayEquals(new String[]{"myString2", "myString"}, getAnnotation(fieldName, Widget.class).search());
        assertEquals(0, loadClass().getAnnotation(Table.class).indexes().length);

        // Transient and relational fields can be overridden
        assertEquals("New", getAnnotation("myTransient", Widget.class).title());
        assertEquals("New", getAnnotation("myCollection", Widget.class).title());
    }

    @Test
    public void testNotAllowed() throws Exception {
        assertNull(getAnnotation("myTransient", Transient.class));

        assertEquals(1, loadClass().getDeclaredConstructors().length);
        assertNull(getAnnotation("myField", Type.class));
        assertNull(getAnnotation("myField", JoinColumn.class));

        assertTrue(getField("myDate").getType().isAssignableFrom(LocalDateTime.class));

        assertTrue(getField("myInt").getType().isAssignableFrom(Integer.class));

        assertEquals("myParent", getAnnotation("myCollection", OneToMany.class).mappedBy());
    }

    private static void compile() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        File temp = Files.createTempDirectory(null).toFile();
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(temp));

        final List<File> files = new ArrayList<>();

        try (Stream<Path> walk = java.nio.file.Files.walk(targetPath)) {
            walk.map(Path::toFile)
                    .filter(f -> f.getName().endsWith(".java"))
                    .forEach(files::add);
        }

        JavaCompiler.CompilationTask task = compiler.getTask(null,
                fileManager, null, null, null, fileManager.getJavaFileObjects(files.toArray(new File[files.size()])));

        boolean result = task.call();
        if (!result) {
            throw new RuntimeException(diagnostics.getDiagnostics().stream().map(it -> it.getMessage(Locale.getDefault())).collect(Collectors.joining("\n")));
        }

        classLoader = new URLClassLoader(new URL[]{ temp.toURI().toURL() }, Thread.currentThread().getContextClassLoader());
    }

    private static void generate() throws IOException {
        File domainPath = new File("src/test/resources/merging/module2");
        EntityGenerator gen = new EntityGenerator(domainPath, srcGenPath.toFile());

        domainPath = new File("src/test/resources/merging/module1");
        EntityGenerator lookup = new EntityGenerator(domainPath, srcGenPath.toFile());

        gen.addLookupSource(lookup);
        gen.start();
    }

    private static void clean() throws IOException {
        if (targetPath.toFile().exists()) {
            FileUtils.deleteDirectory(targetPath);
        }
    }

    <T extends Annotation> T getAnnotation(String name, Class<T> annotation) throws Exception {
        return getField(name).getDeclaredAnnotation(annotation);
    }

    Field getField(String name) throws Exception {
        Field field = loadClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    Class<?> loadClass() throws Exception {
        return classLoader.loadClass("com.axelor.merge.db.MyEntity");
    }

}
