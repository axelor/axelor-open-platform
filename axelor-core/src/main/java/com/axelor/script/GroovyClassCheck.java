/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import groovy.lang.GroovyObject;
import java.lang.reflect.Modifier;
import java.util.Collection;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

/**
 * Validates the classes declared in scripts and marks them as sandboxed code.
 *
 * <p>The code of a script-declared class is checked like the script itself, so its instances are
 * allowed as receivers (see {@link SandboxCode}). What it inherits is not compiled from the script,
 * so its superclass and interfaces must be allowed by the policy: otherwise {@code class X extends
 * java.io.File} would expose {@code File} through {@code X}.
 *
 * <p>{@code finalize()} is rejected: the JVM calls it on its own thread, outside of any evaluation.
 *
 * <p>An interface cannot extend {@link GroovyObject}: the JDK proxy a map or a closure is converted
 * to would read properties through an unchecked metaclass (see {@link #checkInterfaceSupertype}).
 *
 * <p>A class implementing {@link Collection} gets a {@code getMetaClass()} method returning {@link
 * GroovySandbox#collectionMetaClass(Object)}, which validates the elements a property is read from
 * (Groovy reads a property a collection does not have from each of its elements), and cannot
 * declare its own. Groovy gets the metaclass of an object from this method, for every dynamic call
 * and property access on it, so it covers the instances created before any other code of the class
 * runs (static fields, enum constants).
 *
 * <p>This runs once types are resolved, in the {@link CompilePhase#CANONICALIZATION} phase.
 */
class GroovyClassCheck extends CompilationCustomizer {

  private static final ClassNode SANDBOX_CODE = ClassHelper.make(SandboxCode.class);

  private static final ClassNode COLLECTION = ClassHelper.make(Collection.class);

  private static final ClassNode SANDBOX = ClassHelper.make(GroovySandbox.class);

  GroovyClassCheck() {
    super(CompilePhase.CANONICALIZATION);
  }

  @Override
  public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
    // the class generated from the script statements extends SandboxScript (isScript() would also
    // be true for any declared class extending groovy.lang.Script, which must be checked)
    if (classNode.isScriptBody()) {
      return;
    }

    checkSupertype(classNode.getSuperClass());
    for (ClassNode type : classNode.getInterfaces()) {
      checkSupertype(type);
      if (classNode.isInterface()) {
        checkInterfaceSupertype(classNode, type);
      }
    }

    MethodNode finalize = classNode.getDeclaredMethod("finalize", Parameter.EMPTY_ARRAY);
    if (finalize != null) {
      throw new ScriptPolicyException(
          String.format(
              "Method 'finalize' not allowed in class '%s' (line %d).",
              classNode.getName(), finalize.getLineNumber()));
    }

    if (!classNode.implementsInterface(SANDBOX_CODE)) {
      classNode.addInterface(SANDBOX_CODE);
    }

    if (!classNode.isInterface() && classNode.implementsInterface(COLLECTION)) {
      addCollectionMetaClass(classNode);
    }
  }

  private static void addCollectionMetaClass(ClassNode classNode) {
    MethodNode declared = classNode.getDeclaredMethod("getMetaClass", Parameter.EMPTY_ARRAY);
    if (declared != null) {
      throw new ScriptPolicyException(
          String.format(
              "Method 'getMetaClass' not allowed in class '%s' (line %d).",
              classNode.getName(), declared.getLineNumber()));
    }
    classNode.addMethod(
        "getMetaClass",
        Modifier.PUBLIC,
        ClassHelper.METACLASS_TYPE,
        Parameter.EMPTY_ARRAY,
        ClassNode.EMPTY_ARRAY,
        new ReturnStatement(
            new StaticMethodCallExpression(
                SANDBOX,
                "collectionMetaClass",
                new ArgumentListExpression(VariableExpression.THIS_EXPRESSION))));
  }

  private static void checkSupertype(ClassNode type) {
    Class<?> klass = GroovyTypes.resolve(type);
    if (klass == null
        || klass == Object.class
        || klass == GroovyObject.class
        || klass == SandboxCode.class) {
      return;
    }
    ScriptPolicy.getInstance().check(klass);
    // a subclass would inherit their guarded static methods, outside of their guarding metaclass
    for (Class<?> guarded : SandboxGuardedMethods.classes()) {
      if (guarded.isAssignableFrom(klass)) {
        throw new ScriptPolicyException(
            String.format("Class '%s' cannot be extended in scripts.", klass.getName()));
      }
    }
  }

  /**
   * Rejects {@link GroovyObject} (and its subinterfaces) as a supertype of an interface. A map or a
   * closure converted to the interface is a JDK proxy, sandboxed code as the interface is, whose
   * default {@code getProperty(name)} reads the property through the metaclass of the proxy class,
   * which reads it from each element of a collection, unchecked. Classes are not concerned: they
   * implement {@link GroovyObject} anyway, and a collection gets a metaclass checking its elements.
   */
  private static void checkInterfaceSupertype(ClassNode classNode, ClassNode type) {
    Class<?> klass = GroovyTypes.resolve(type);
    if (klass != null && GroovyObject.class.isAssignableFrom(klass)) {
      throw new ScriptPolicyException(
          String.format(
              "Interface '%s' cannot extend '%s' in scripts (line %d).",
              classNode.getName(), klass.getName(), classNode.getLineNumber()));
    }
  }
}
