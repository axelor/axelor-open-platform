/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 * A custom Groovy AST transformer to validate property accesses against the policy settings.
 *
 * <p>This is done at a compile phase when local variables and class names are resolved: before, a
 * qualified class name ({@code com.axelor.example.Foo}) is still a chain of properties.
 *
 * <p>For example:
 *
 * <pre>
 * def value = foo.bar
 * def value2 = com.axelor.example.Foo.BAR
 * def value3 = foo.bar.baz
 * </pre>
 *
 * would become:
 *
 * <pre>
 * def value = GroovySandbox.checkProperty(foo, 'bar').bar
 * def value2 = GroovySandbox.checkProperty(com.axelor.example.Foo, 'BAR').BAR
 * def value3 = GroovySandbox.checkProperty(GroovySandbox.checkProperty(foo, 'bar').bar, 'baz').baz
 * </pre>
 *
 * <p>The compiler also turns a call of a local variable ({@code x()}) into a call of its {@code
 * call} method ({@code x.call()}) in this phase, after {@link GroovyCheckTransformation} checked
 * the receiver of {@code x()}: the variable is checked here instead.
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
      // the expression transformer does not visit the code of closures
      if (expr instanceof ClosureExpression closure) {
        if (closure.getCode() != null) {
          closure.getCode().visit(this);
        }
        return closure;
      }
      if (expr instanceof PropertyExpression prop) {
        Expression obj = prop.getObjectExpression();
        // a call result is already checked by GroovyCheckTransformation
        if (!(obj instanceof MethodCallExpression)
            && !GroovyCheckTransformation.isReceiverCheck(obj)) {
          // the owner first: in a chain (a.b.c), a.b is itself a property access to check
          return GroovyCheckTransformation.checkedPropertyX(
              prop, transform(obj), transform(prop.getProperty()));
        }
      }
      if (expr instanceof MethodCallExpression call && isLocalVariableCall(call)) {
        MethodCallExpression res = (MethodCallExpression) super.transform(expr);
        res.setObjectExpression(
            GroovyCheckTransformation.receiverCheckX(res.getObjectExpression(), false));
        return res;
      }
      return super.transform(expr);
    }

    /** Whether the call is a call of a local variable, {@code x()} compiled as {@code x.call()}. */
    private static boolean isLocalVariableCall(MethodCallExpression call) {
      return !call.isImplicitThis()
          && "call".equals(call.getMethodAsString())
          && call.getObjectExpression() instanceof VariableExpression variable
          && !variable.isThisExpression()
          && !variable.isSuperExpression();
    }
  }
}
