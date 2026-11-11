/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import groovy.lang.GroovyObject;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
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
 * <p>This runs once types are resolved, in the {@link CompilePhase#CANONICALIZATION} phase.
 */
class GroovyClassCheck extends CompilationCustomizer {

  private static final ClassNode SANDBOX_CODE = ClassHelper.make(SandboxCode.class);

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
  }

  private static void checkSupertype(ClassNode type) {
    // declared in the same script: checked on its own
    if (type == null || type.isPrimaryClassNode()) {
      return;
    }
    Class<?> klass = type.getTypeClass();
    if (klass == Object.class || klass == GroovyObject.class || klass == SandboxCode.class) {
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
}
