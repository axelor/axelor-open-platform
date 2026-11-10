/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import groovy.transform.Generated;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.transform.BaseScriptASTTransformation;

/**
 * Rejects annotations in scripts and templates, except {@code @Override} and {@code @Deprecated}.
 *
 * <p>Some annotations trigger AST transformations that run code while the script is compiled,
 * before any runtime check exists: {@code @ASTTest} runs arbitrary code, {@code @Grab} downloads
 * and loads jars, {@code @BaseScript} replaces the {@link SandboxScript} base class... A blocklist
 * would not be safe ({@code @AnnotationCollector} aliases, transformations provided by libraries),
 * so annotations are rejected, except the two plain Java annotations useful in script classes.
 *
 * <p>Names are not resolved yet in this phase, so {@code @Override} and {@code @Deprecated} are
 * only allowed when they cannot mean anything else: written fully qualified, or by simple name in a
 * script that cannot redefine that name (no import of that name, no star import, no package, no
 * class of that name). Their values must be constants.
 *
 * <p>This runs in the {@link CompilePhase#CONVERSION} phase, before the annotation-triggered
 * transformations are collected and applied (from {@link CompilePhase#SEMANTIC_ANALYSIS}).
 */
class GroovyAnnotationCheck extends CompilationCustomizer {

  private static final ClassNode GENERATED = ClassHelper.make(Generated.class);

  private static final Set<String> JAVA_ANNOTATIONS = Set.of("Override", "Deprecated");

  private static final String JAVA_LANG = "java.lang.";

  private static final String GLOBAL_TRANSFORMS =
      "META-INF/services/org.codehaus.groovy.transform.ASTTransformation";

  GroovyAnnotationCheck() {
    super(CompilePhase.CONVERSION);
  }

  /**
   * Lists the global AST transformations available to the given class loader, to disable them all.
   *
   * <p>Global transformations apply to every compiled source and react to what it contains (e.g.
   * {@code GrabAnnotationTransformation} for {@code @Grab}). They run in the {@link
   * CompilePhase#CONVERSION} phase before any compilation customizer, so this check cannot reject
   * their trigger in time: they are disabled instead. Scripts do not need any.
   *
   * <p>They are found the same way as the Groovy compiler does, from the service files.
   */
  static Set<String> globalTransforms(ClassLoader loader) {
    Set<String> names = new LinkedHashSet<>();
    try {
      for (URL url : Collections.list(loader.getResources(GLOBAL_TRANSFORMS))) {
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
          reader
              .lines()
              .map(String::trim)
              .filter(line -> !line.isEmpty() && !line.startsWith("#"))
              .forEach(names::add);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to list the global AST transformations", e);
    }
    return names;
  }

  @Override
  public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
    ModuleNode module = source.getAST();
    if (module != null) {
      if (module.getPackage() != null) {
        reject(module.getPackage(), module);
      }
      Stream.of(
              module.getImports(),
              module.getStarImports(),
              List.copyOf(module.getStaticImports().values()),
              List.copyOf(module.getStaticStarImports().values()))
          .flatMap(List::stream)
          .forEach(node -> reject(node, module));
    }
    new AnnotationVisitor(source, module).visitClass(classNode);
  }

  private static void reject(AnnotatedNode node, ModuleNode module) {
    List<AnnotationNode> annotations = node.getAnnotations();
    if (annotations == null) {
      return;
    }
    for (AnnotationNode annotation : annotations) {
      if (isSandboxBaseScript(node, annotation)
          || isGeneratedMarker(annotation)
          || isJavaAnnotation(annotation, module)) {
        continue;
      }
      String name = annotation.getClassNode().getName();
      String hint =
          JAVA_ANNOTATIONS.contains(name)
              ? String.format(" Use '@%s%s' in this script.", JAVA_LANG, name)
              : "";
      throw new ScriptPolicyException(
          String.format(
              "Annotation '@%s' not allowed (line %d).%s", name, annotation.getLineNumber(), hint));
    }
  }

  /**
   * Whether the annotation is {@code @Override} or {@code @Deprecated}, with constant values only:
   * written fully qualified, or by simple name when the script cannot make that name refer to
   * another type.
   */
  private static boolean isJavaAnnotation(AnnotationNode annotation, ModuleNode module) {
    String name = annotation.getClassNode().getName();
    boolean qualified = name.startsWith(JAVA_LANG);
    String simpleName = qualified ? name.substring(JAVA_LANG.length()) : name;
    if (!JAVA_ANNOTATIONS.contains(simpleName)) {
      return false;
    }
    if (!annotation.getMembers().values().stream().allMatch(ConstantExpression.class::isInstance)) {
      return false;
    }
    return qualified || (module != null && !canRedefine(module, simpleName));
  }

  /** Whether a simple type name may resolve to something else than the {@code java.lang} type. */
  private static boolean canRedefine(ModuleNode module, String simpleName) {
    return module.getPackage() != null
        || !module.getStarImports().isEmpty()
        || !module.getStaticStarImports().isEmpty()
        || module.getImport(simpleName) != null
        || module.getStaticImports().containsKey(simpleName)
        || module.getClasses().stream()
            .map(ClassNode::getName)
            .anyMatch(
                className ->
                    className.equals(simpleName)
                        || className.endsWith("." + simpleName)
                        || className.endsWith("$" + simpleName));
  }

  /**
   * Whether the annotation is the one Groovy itself adds to the script class to apply the
   * configured script base class ({@link SandboxScript}): a bare {@code @BaseScript}, without
   * members nor source position. A {@code @BaseScript} written in a script always has a position.
   */
  private static boolean isSandboxBaseScript(AnnotatedNode node, AnnotationNode annotation) {
    return node instanceof ClassNode classNode
        && classNode.isScriptBody()
        && SandboxScript.class.getName().equals(classNode.getSuperClass().getName())
        && BaseScriptASTTransformation.MY_TYPE.equals(annotation.getClassNode())
        && annotation.getMembers().isEmpty()
        && annotation.getLineNumber() < 0;
  }

  /**
   * Whether the annotation is the {@code @Generated} marker the compiler itself adds to the members
   * it generates (e.g. the methods of an enum), without source position. It triggers no
   * transformation. A {@code @Generated} written in a script always has a position.
   */
  private static boolean isGeneratedMarker(AnnotationNode annotation) {
    return GENERATED.equals(annotation.getClassNode())
        && annotation.getMembers().isEmpty()
        && annotation.getLineNumber() < 0;
  }

  private static void reject(Parameter[] parameters, ModuleNode module) {
    if (parameters != null) {
      for (Parameter parameter : parameters) {
        reject(parameter, module);
      }
    }
  }

  /** Visits every node that can carry an annotation. */
  private static class AnnotationVisitor extends ClassCodeVisitorSupport {

    private final SourceUnit source;
    private final ModuleNode module;

    AnnotationVisitor(SourceUnit source, ModuleNode module) {
      this.source = source;
      this.module = module;
    }

    @Override
    protected SourceUnit getSourceUnit() {
      return source;
    }

    // classes, fields, properties, methods and constructors
    @Override
    public void visitAnnotations(AnnotatedNode node) {
      reject(node, module);
      super.visitAnnotations(node);
    }

    @Override
    protected void visitConstructorOrMethod(MethodNode node, boolean isConstructor) {
      reject(node.getParameters(), module);
      super.visitConstructorOrMethod(node, isConstructor);
    }

    // local variables
    @Override
    public void visitDeclarationExpression(DeclarationExpression expression) {
      reject(expression, module);
      super.visitDeclarationExpression(expression);
    }

    @Override
    public void visitClosureExpression(ClosureExpression expression) {
      reject(expression.getParameters(), module);
      super.visitClosureExpression(expression);
    }

    // The parser currently drops annotations on for-in variables and catch parameters, checked in
    // case it no longer does.
    @Override
    public void visitForLoop(ForStatement statement) {
      reject(statement.getVariable(), module);
      super.visitForLoop(statement);
    }

    @Override
    public void visitCatchStatement(CatchStatement statement) {
      reject(statement.getVariable(), module);
      super.visitCatchStatement(statement);
    }
  }
}
