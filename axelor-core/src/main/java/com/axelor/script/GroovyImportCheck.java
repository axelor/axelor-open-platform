/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.script.GroovyScriptSupport.Helpers;
import java.util.stream.Stream;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

/**
 * Validates the classes of static imports against the policy.
 *
 * <p>A statically imported method is called without a receiver ({@code import static
 * java.lang.System.getenv; getenv('PATH')}), so the runtime receiver check never sees its class.
 * Such imports are therefore checked once, at compile time, once their types are resolved.
 */
class GroovyImportCheck extends CompilationCustomizer {

  GroovyImportCheck() {
    super(CompilePhase.CANONICALIZATION);
  }

  @Override
  public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
    ModuleNode module = source.getAST();
    if (module == null) {
      return;
    }
    Stream.concat(
            module.getStaticImports().values().stream(),
            module.getStaticStarImports().values().stream())
        .map(ImportNode::getType)
        .map(GroovyTypes::resolve)
        // nothing to check (e.g. a class declared in the script), or the helpers imported by
        // GroovyScriptSupport itself
        .filter(type -> type != null && type != Helpers.class)
        .forEach(type -> ScriptPolicy.getInstance().check(type));
  }
}
