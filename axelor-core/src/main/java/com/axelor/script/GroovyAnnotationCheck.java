/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

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
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.transform.BaseScriptASTTransformation;

/**
 * Rejects any annotation in scripts and templates.
 *
 * <p>Some annotations trigger AST transformations that run code while the script is compiled,
 * before any runtime check exists: {@code @ASTTest} runs arbitrary code, {@code @Grab} downloads
 * and loads jars, {@code @BaseScript} replaces the {@link SandboxScript} base class... Scripts do
 * not need annotations, so all of them are rejected.
 *
 * <p>This runs in the {@link CompilePhase#CONVERSION} phase, before the annotation-triggered
 * transformations are collected and applied (from {@link CompilePhase#SEMANTIC_ANALYSIS}).
 */
class GroovyAnnotationCheck extends CompilationCustomizer {

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
        reject(module.getPackage());
      }
      Stream.of(
              module.getImports(),
              module.getStarImports(),
              List.copyOf(module.getStaticImports().values()),
              List.copyOf(module.getStaticStarImports().values()))
          .flatMap(List::stream)
          .forEach(GroovyAnnotationCheck::reject);
    }
    new AnnotationVisitor(source).visitClass(classNode);
  }

  private static void reject(AnnotatedNode node) {
    List<AnnotationNode> annotations = node.getAnnotations();
    if (annotations == null) {
      return;
    }
    for (AnnotationNode annotation : annotations) {
      if (!isSandboxBaseScript(node, annotation)) {
        throw new ScriptPolicyException(
            String.format(
                "Annotation '@%s' not allowed (line %d).",
                annotation.getClassNode().getName(), annotation.getLineNumber()));
      }
    }
  }

  /**
   * Whether the annotation is the one Groovy itself adds to the script class to apply the
   * configured script base class ({@link SandboxScript}): a bare {@code @BaseScript}, without
   * members nor source position. A {@code @BaseScript} written in a script always has a position.
   */
  private static boolean isSandboxBaseScript(AnnotatedNode node, AnnotationNode annotation) {
    return node instanceof ClassNode classNode
        && classNode.isScript()
        && SandboxScript.class.getName().equals(classNode.getSuperClass().getName())
        && BaseScriptASTTransformation.MY_TYPE.equals(annotation.getClassNode())
        && annotation.getMembers().isEmpty()
        && annotation.getLineNumber() < 0;
  }

  private static void reject(Parameter[] parameters) {
    if (parameters != null) {
      for (Parameter parameter : parameters) {
        reject(parameter);
      }
    }
  }

  /** Visits every node that can carry an annotation. */
  private static class AnnotationVisitor extends ClassCodeVisitorSupport {

    private final SourceUnit source;

    AnnotationVisitor(SourceUnit source) {
      this.source = source;
    }

    @Override
    protected SourceUnit getSourceUnit() {
      return source;
    }

    // classes, fields, properties, methods and constructors
    @Override
    public void visitAnnotations(AnnotatedNode node) {
      reject(node);
      super.visitAnnotations(node);
    }

    @Override
    protected void visitConstructorOrMethod(MethodNode node, boolean isConstructor) {
      reject(node.getParameters());
      super.visitConstructorOrMethod(node, isConstructor);
    }

    // local variables
    @Override
    public void visitDeclarationExpression(DeclarationExpression expression) {
      reject(expression);
      super.visitDeclarationExpression(expression);
    }

    @Override
    public void visitClosureExpression(ClosureExpression expression) {
      reject(expression.getParameters());
      super.visitClosureExpression(expression);
    }

    // The parser currently drops annotations on for-in variables and catch parameters, checked in
    // case it no longer does.
    @Override
    public void visitForLoop(ForStatement statement) {
      reject(statement.getVariable());
      super.visitForLoop(statement);
    }

    @Override
    public void visitCatchStatement(CatchStatement statement) {
      reject(statement.getVariable());
      super.visitCatchStatement(statement);
    }
  }
}
