/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.script.GroovyScriptSupport.PolicyChecker;
import java.util.function.Consumer;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.DoWhileStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.LoopingStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 * A custom groovy ast transformer to validate type of objects against the policy settings.
 *
 * <p>This transformer will transform the ctor/method/property calls by wrapping the object side in
 * `__$$check(Object)` helper.
 *
 * <p>It will also transform loop and if blocks and adds timeout check.
 *
 * <p>For example:
 *
 * <pre>
 * def value = some.method()
 *
 * while(true) {
 *   println('Hello')
 * }
 * </pre>
 *
 * would become:
 *
 * <pre>
 * def value = __$$policy.check(some).method()
 *
 * while(true) {
 *   __$$policy.timeout()
 *   __$$policy.check(this).println('Hello')
 * }
 * </pre>
 */
@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
public class GroovyCheckTransformation extends AbstractASTTransformation {

  @Override
  public void visit(ASTNode[] nodes, SourceUnit source) {

    CheckTransformer checkTransformer = new CheckTransformer(source);
    LoopTransformer loopTransformer = new LoopTransformer(source);

    for (ASTNode node : nodes) {
      if (node instanceof ClassNode classNode) {
        checkTransformer.visitClass(classNode);
        loopTransformer.visitClass(classNode);
      }
    }
  }

  static class CheckTransformer extends ClassCodeExpressionTransformer {

    private final SourceUnit unit;

    private boolean inClass;

    public CheckTransformer(SourceUnit unit) {
      this.unit = unit;
    }

    @Override
    protected SourceUnit getSourceUnit() {
      return unit;
    }

    private MethodCallExpression checkX(Expression expr) {
      return new MethodCallExpression(
          new VariableExpression(PolicyChecker.NAME),
          new ConstantExpression(PolicyChecker.CALL_CHECK),
          new ArgumentListExpression(expr));
    }

    @Override
    public Expression transform(Expression expr) {

      // only process script class content
      inClass = inClass || expr instanceof ClassExpression;

      if (inClass && expr instanceof PropertyExpression) {
        PropertyExpression prop = (PropertyExpression) super.transform(expr);
        Expression obj = prop.getObjectExpression();
        if (obj instanceof MethodCallExpression) {
          PropertyExpression res =
              new PropertyExpression(checkX(obj), prop.getProperty(), prop.isSafe());
          res.setSpreadSafe(prop.isSpreadSafe());
          return res;
        }
        return prop;
      }

      if (inClass && expr instanceof ConstructorCallExpression) {
        return checkX(super.transform(expr));
      }

      if (inClass && expr instanceof MethodCallExpression) {
        MethodCallExpression method = (MethodCallExpression) super.transform(expr);
        MethodCallExpression res =
            new MethodCallExpression(
                checkX(method.getObjectExpression()), method.getMethod(), method.getArguments());
        res.setSafe(method.isSafe());
        res.setSpreadSafe(method.isSpreadSafe());
        return res;
      }

      if (inClass && expr instanceof ClosureExpression ce) {
        if (ce.getCode() != null) {
          ce.getCode().visit(this);
        }
        return ce;
      }

      return super.transform(expr);
    }
  }

  static class LoopTransformer extends ClassCodeExpressionTransformer {

    private final SourceUnit unit;

    public LoopTransformer(SourceUnit unit) {
      this.unit = unit;
    }

    @Override
    protected SourceUnit getSourceUnit() {
      return unit;
    }

    @Override
    public void visitForLoop(ForStatement stmt) {
      super.visitForLoop(stmt);
      transformLoop(stmt);
    }

    @Override
    public void visitWhileLoop(WhileStatement stmt) {
      super.visitWhileLoop(stmt);
      transformLoop(stmt);
    }

    @Override
    public void visitDoWhileLoop(DoWhileStatement stmt) {
      super.visitDoWhileLoop(stmt);
      transformLoop(stmt);
    }

    private <T extends LoopingStatement> void transformLoop(T stmt) {
      addTimeout(stmt.getLoopBlock(), stmt::setLoopBlock);
    }

    private void addTimeout(Statement block, Consumer<BlockStatement> accept) {
      BlockStatement wrapped = toBlockStatement(block);
      wrapped.getStatements().add(0, checkX());
      accept.accept(wrapped);
    }

    private BlockStatement toBlockStatement(Statement stmt) {
      if (stmt instanceof BlockStatement blockStmt) {
        return blockStmt;
      }
      BlockStatement block = new BlockStatement();
      block.addStatement(stmt);
      return block;
    }

    private Statement checkX() {
      return new ExpressionStatement(
          new MethodCallExpression(
              new VariableExpression(PolicyChecker.NAME),
              new ConstantExpression(PolicyChecker.CALL_TIMEOUT),
              ArgumentListExpression.EMPTY_ARGUMENTS));
    }
  }
}
