/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 * A custom Groovy AST transformer to validate property accesses against the policy settings.
 *
 * <p>This is done at a compile phase when local variables are resolved.
 *
 * <p>For example:
 *
 * <pre>
 * def value = foo.bar
 * def value2 = com.axelor.example.Foo.BAR
 * </pre>
 *
 * would become:
 *
 * <pre>
 * def value = GroovySandbox.check(foo).bar
 * def value2 = GroovySandbox.check(com.axelor.example.Foo).BAR
 * </pre>
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class GroovyCheckPropTransformation extends AbstractASTTransformation {

  @Override
  public void visit(ASTNode[] nodes, SourceUnit source) {
    LocalVariableTransformer transformer = new LocalVariableTransformer(source);
    for (ASTNode node : nodes) {
      if (node instanceof ClassNode classNode) {
        transformer.visitClass(classNode);
      }
    }
  }

  static class LocalVariableTransformer extends ClassCodeExpressionTransformer {

    private final SourceUnit unit;

    public LocalVariableTransformer(SourceUnit unit) {
      this.unit = unit;
    }

    @Override
    protected SourceUnit getSourceUnit() {
      return unit;
    }

    @Override
    public Expression transform(Expression expr) {
      if (expr instanceof PropertyExpression prop) {
        Expression obj = prop.getObjectExpression();
        // a call result is already checked by GroovyCheckTransformation
        if (!(obj instanceof MethodCallExpression)
            && !GroovyCheckTransformation.isSandboxCall(obj)) {
          PropertyExpression res =
              new PropertyExpression(
                  GroovyCheckTransformation.receiverCheckX(obj, prop.isSpreadSafe()),
                  prop.getProperty(),
                  prop.isSafe());
          res.setSpreadSafe(prop.isSpreadSafe());
          return res;
        }
      }
      return super.transform(expr);
    }
  }
}
