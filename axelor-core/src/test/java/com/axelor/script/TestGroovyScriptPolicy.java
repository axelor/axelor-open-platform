/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.meta.ActionExecutor;
import com.axelor.meta.schema.actions.ActionScript;
import com.axelor.meta.schema.actions.validate.validator.Notify;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.axelor.test.db.Contact;
import groovy.lang.Closure;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.script.SimpleBindings;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.jupiter.api.Test;

/**
 * Scripting policy and sandbox of the Groovy engine (scripts and templates compiled by {@link
 * GroovyScriptSupport}).
 */
class TestGroovyScriptPolicy extends AbstractScriptPolicyTest {

  @Test
  void testPolicy() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Allowed by annotation
    assertEquals(
        "AllowedByAnnotation",
        helper.eval("__bean__(com.axelor.script.policy.AllowedByAnnotation).myMethod()"));
    assertEquals(
        "InnerAllowedByAnnotation",
        helper.eval(
            "__bean__(com.axelor.script.policy.AllowedByAnnotation.InnerAllowed).myMethod()"));
    assertEquals(
        "InnerDefault",
        helper.eval(
            "__bean__(com.axelor.script.policy.AllowedByAnnotation.InnerDefault).myMethod()"));

    // Allowed by configuration
    assertEquals(
        "AllowedByConfiguration",
        helper.eval("com.axelor.script.policy.AllowedByConfiguration.myStaticMethod()"));
    assertEquals(
        "InnerAllowedByConfiguration",
        helper.eval(
            "com.axelor.script.policy.AllowedByConfiguration.InnerAllowed.myStaticMethod()"));

    // Denied by configuration
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByConfiguration.myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "com.axelor.script.policy.DeniedByConfiguration.InnerAllowed.myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "com.axelor.script.policy.DeniedByConfiguration.InnerDenied.myStaticMethod()"));

    // Denied by default
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByDefault.myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByDefault.InnerDenied.myStaticMethod()"));

    assertEquals(
        "SubAllowedByAnnotation",
        helper.eval("__bean__(com.axelor.script.policy.SubAllowedByAnnotation).myMethod()"));

    assertEquals(
        "SubAllowedByConfiguration",
        helper.eval("com.axelor.script.policy.SubAllowedByConfiguration.myStaticMethod()"));

    // Service
    assertEquals(
        "Hello, World!", helper.eval("__bean__(com.axelor.script.policy.MyService).myMethod()"));
    assertEquals(
        "myYetAnotherMethod",
        helper.eval("__bean__(com.axelor.script.policy.MyService).myYetAnotherMethod()"));
    assertEquals(
        2, helper.eval("__bean__(com.axelor.script.policy.MyOtherService).myOtherMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "__bean__(com.axelor.script.policy.MyYetAnotherService).myYetAnotherMethod()"));

    // Property access
    assertEquals(
        "AllowedByAnnotation",
        helper.eval("__bean__(com.axelor.script.policy.AllowedByAnnotation).myValue"));
    assertEquals(
        "AllowedByConfiguration",
        helper.eval("com.axelor.script.policy.AllowedByConfiguration.myStaticValue"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByConfiguration.myStaticValue"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("__bean__(com.axelor.script.policy.DeniedByConfiguration).myValue"));

    // Constant access
    assertEquals("myConstant", helper.eval("com.axelor.script.policy.MyService.MY_CONSTANT"));
    assertEquals(
        List.of("hello", "world"),
        helper.eval("com.axelor.script.policy.MyService.MY_CONSTANT_LIST"));
    assertEquals(
        "myOtherConstant", helper.eval("com.axelor.script.policy.MyOtherService.MY_CONSTANT"));
    assertEquals(
        List.of("hello", "world"),
        helper.eval("com.axelor.script.policy.MyOtherService.MY_CONSTANT_LIST"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.MyYetAnotherService.MY_CONSTANT"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.MyYetAnotherService.MY_CONSTANT_LIST"));

    // Service returning instance of denied class
    assertDoesNotThrow(
        () ->
            helper.eval(
                """
                var myService = __bean__(com.axelor.script.policy.MyService);
                var a = myService.myYetAnotherMethod();

                if (a != "myYetAnotherMethod") {
                  throw new Exception("Should be allowed");
                }

                var myYetAnotherService = myService.getMyYetAnotherService();

                try {
                  var b = myYetAnotherService.MY_CONSTANT;
                  if (b?.length() > 0) {
                    throw new Exception("Should be denied");
                  }
                } catch (com.axelor.script.ScriptPolicyException e) {
                  // Denied as expected
                }
                """));

    // Entity manager
    assertThrows(IllegalArgumentException.class, () -> helper.eval("com.axelor.db.JPA.em()"));
  }

  @Test
  void testActionScript() {
    var contact = ensureContact("John", "Doe", "john.doe@axelor.com");

    var action = new ActionScript();
    var script = new ActionScript.ActScript();
    script.setLanguage("groovy");
    action.setScript(script);

    var request = new ActionRequest();
    request.setModel(Contact.class.getName());
    request.setData(Map.of("context", Mapper.toMap(contact)));
    var handler = Beans.get(ActionExecutor.class).newActionHandler(request);

    // Test $request and $response
    script.setCode(
        """
        final firstName = $request.context.firstName;
        $response.setNotify("Hello, ${firstName}!");
        """);
    var response = (ActionResponse) action.execute(handler);
    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    @SuppressWarnings("unchecked")
    var data = ((List<Map<String, Object>>) response.getData()).get(0);
    @SuppressWarnings("unchecked")
    var notify = (Map<String, Object>) data.get(Notify.KEY);
    assertEquals("Hello, %s!".formatted(contact.getFirstName()), notify.get("message"));

    script.setTransactional(true);

    // Test $json
    script.setCode(
        """
        def record = $json.create("custom_model", [name: "Custom JSON Groovy"]);
        record = $json.save(record);
        $response.setNotify("Saved ${record.name}");
        """);
    response = (ActionResponse) action.execute(handler);
    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    @SuppressWarnings("unchecked")
    var jsonData = ((List<Map<String, Object>>) response.getData()).get(0);
    @SuppressWarnings("unchecked")
    var jsonNotify = (Map<String, Object>) jsonData.get(Notify.KEY);
    assertEquals("Saved Custom JSON Groovy", jsonNotify.get("message"));

    // Test $em
    script.setCode(
        """
        final found = $em.find(Contact, $request.context.id);
        $response.setNotify("Found ${found.firstName} ${found.lastName}");
        """);
    response = (ActionResponse) action.execute(handler);
    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    @SuppressWarnings("unchecked")
    var emData = ((List<Map<String, Object>>) response.getData()).get(0);
    @SuppressWarnings("unchecked")
    var emNotify = (Map<String, Object>) emData.get(Notify.KEY);
    assertEquals(
        "Found %s %s".formatted(contact.getFirstName(), contact.getLastName()),
        emNotify.get("message"));
  }

  @Test
  void testGdkProcessExecutionIsDenied() {
    // A command that does not exist: nothing runs even if the policy fails to block the call.
    final String missingCmd = "axelor-nonexistent-binary-xyz";

    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Positive controls: denied via the receiver check.

    // Receiver is Runtime (denied class).
    assertBlockedByPolicy(helper, "Runtime.getRuntime().exec('" + missingCmd + "'); 1");

    // Constructor of a denied class.
    assertBlockedByPolicy(helper, "new ProcessBuilder('" + missingCmd + "').start(); 1");

    // The reported bypass: a GDK extension method dispatched on an allow-listed receiver.

    // Receiver is java.util.ArrayList (package java.util is allow-listed),
    // dispatched to ProcessGroovyMethods.execute(List).
    assertBlockedByPolicy(helper, "['" + missingCmd + "'].execute(); 1");

    // Receiver is java.lang.String (package java.lang is allow-listed),
    // dispatched to ProcessGroovyMethods.execute(String).
    assertBlockedByPolicy(helper, "'" + missingCmd + "'.execute(); 1");

    // Array receiver variant, dispatched to ProcessGroovyMethods.execute(String[]).
    assertBlockedByPolicy(helper, "(['" + missingCmd + "'] as String[]).execute(); 1");

    // Overload with env/dir arguments (null, so no denied type is constructed as a side effect),
    // dispatched to ProcessGroovyMethods.execute(List, String[], File).
    assertBlockedByPolicy(helper, "['" + missingCmd + "'].execute((String[]) null, null); 1");

    // Spread (recv*.execute()) dispatches execute() on each element, so a receiver whose element
    // type (not the collection type) provides the GDK method must be caught too.
    assertBlockedByPolicy(helper, "(['" + missingCmd + "'] as Set)*.execute(); 1");
    assertBlockedByPolicy(helper, "[k: '" + missingCmd + "'].values()*.execute(); 1");
    assertBlockedByPolicy(helper, "(['" + missingCmd + "'] as String[])*.execute(); 1");

    // Other dispatch routes reaching the same GDK method, which the call-site checks cannot see:
    // they are caught by the SandboxGroovyMethods override of execute() itself.

    // Dynamic method name.
    assertBlockedByPolicy(helper, "'" + missingCmd + "'.\"${'exec' + 'ute'}\"(); 1");
    // invokeMethod with the method name as argument.
    assertBlockedByPolicy(helper, "'" + missingCmd + "'.invokeMethod('execute', null); 1");
    // Implicit call resolved on a closure delegate.
    assertBlockedByPolicy(helper, "'" + missingCmd + "'.with { execute() }; 1");
    assertBlockedByPolicy(helper, "'" + missingCmd + "'.tap { execute() }; 1");
    assertBlockedByPolicy(helper, "'" + missingCmd + "'.identity { execute() }; 1");
    assertBlockedByPolicy(helper, "['" + missingCmd + "'].with { execute() }; 1");
    assertBlockedByPolicy(
        helper, "def c = { -> execute() }; c.delegate = '" + missingCmd + "'; c(); 1");
    // Spread over a receiver that is neither a Collection nor an array.
    assertBlockedByPolicy(helper, "['" + missingCmd + "'].iterator()*.execute(); 1");
  }

  // The sandbox must not depend on a name the script can rebind: a local variable or a closure
  // delegate named like the old policy checker variable must not replace the checks.
  @Test
  void testPolicyChecksCannotBeShadowed() {
    final String missingCmd = "axelor-nonexistent-binary-xyz";
    final String fake =
        "[check: { o -> o }, checkMethod: { o, n -> o }, checkSpread: { o, n -> o },"
            + " timeout: { -> null }]";

    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    assertBlockedByPolicy(
        helper,
        "def __$$policy = " + fake + "; Runtime.getRuntime().exec('" + missingCmd + "'); 1");
    assertBlockedByPolicy(
        helper,
        "[('__$$policy'): "
            + fake
            + "].with { Runtime.getRuntime().exec('"
            + missingCmd
            + "') }; 1");

    // A map delegate no longer hides the checks from the closure (it used to fail with a NPE).
    assertEquals("X", helper.eval("[name: 'x'].with { name.toUpperCase() }"));
  }

  // A spread (recv*.name) dispatches on each element of the receiver, not on the receiver itself,
  // so a denied class hidden in an allowed collection, iterator, etc. must be caught.
  @Test
  void testSpreadElementsAreChecked() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Spread method calls.
    assertBlockedByPolicy(helper, "[System]*.getenv('HOME')");
    assertBlockedByPolicy(helper, "([System] as Set)*.getenv('HOME')");
    assertBlockedByPolicy(helper, "[System].iterator()*.getenv('HOME')");

    // Spread property access.
    assertBlockedByPolicy(helper, "[System]*.env");
    assertBlockedByPolicy(helper, "[System].iterator()*.env");
    assertBlockedByPolicy(helper, "def l = [System]; l*.env");

    // Allowed spreads still work.
    assertEquals(List.of("1", "2"), helper.eval("[1, 2]*.toString()"));
    assertEquals(List.of("A", "B"), helper.eval("['a', 'b'].iterator()*.toUpperCase()"));
    assertEquals(List.of(1, 2), helper.eval("['a', 'bb']*.length()"));
    assertEquals(List.of("a"), helper.eval("[a: 1].entrySet()*.key"));
    assertEquals(null, helper.eval("def l = null; l*.size()"));
  }

  // Inside a closure, calls and properties without a receiver are resolved on its delegate (or
  // owner), which the receiver checks never see. A sandboxed closure must therefore never get a
  // delegate or owner the policy denies, however it is set.
  @Test
  void testClosureDelegateMustBeAllowed() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());
    final String closure = "def c = { -> getenv('HOME') }; ";

    assertBlockedByPolicy(helper, closure + "c.delegate = System; c()");
    assertBlockedByPolicy(helper, closure + "c.setDelegate(System); c()");
    assertBlockedByPolicy(helper, closure + "c['delegate'] = System; c()");
    assertBlockedByPolicy(helper, closure + "c.setProperty('delegate', System); c()");
    assertBlockedByPolicy(helper, closure + "c.invokeMethod('setDelegate', System); c()");
    assertBlockedByPolicy(helper, closure + "c.\"${'setDel' + 'egate'}\"(System); c()");
    assertBlockedByPolicy(helper, closure + "c.rehydrate(System, null, null)()");
    assertBlockedByPolicy(helper, closure + "c.rehydrate(null, System, null)()");

    // Property access resolved on the delegate.
    assertBlockedByPolicy(
        helper, "def c = { -> env }; c.resolveStrategy = 1; c.delegate = System; c()");

    // Nested closure, resolved through its owner's delegate.
    assertBlockedByPolicy(
        helper, "def c = { -> { -> getenv('HOME') }() }; c.delegate = System; c()");
  }

  // Common closure usage must keep working with sandboxed closures.
  @Test
  void testClosuresStillWork() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Script closures are sandboxed.
    assertTrue(helper.eval("{ -> 1 }") instanceof SandboxClosure);

    // Delegate set by GDK methods to an allowed receiver.
    assertEquals("ABC", helper.eval("'abc'.with { toUpperCase() }"));
    assertEquals("abc", helper.eval("'abc'.tap { toUpperCase() }"));
    assertEquals("X", helper.eval("[name: 'x'].with { name.toUpperCase() }"));
    assertEquals(3, helper.eval("[1, 2, 3].with { size() }"));

    // Delegate set explicitly to an allowed object.
    assertEquals(3, helper.eval("def c = { -> size() }; c.delegate = [1, 2, 3]; c()"));
    // resolveStrategy 1 is Closure.DELEGATE_FIRST (the Closure class itself is not allowed).
    assertEquals(
        "X",
        helper.eval(
            "def c = { -> name.toUpperCase() }; c.resolveStrategy = 1;"
                + " c.delegate = [name: 'x']; c()"));
    assertEquals(1, helper.eval("def c = { -> 1 }; c.rehydrate([1], null, null)()"));

    // Iteration and collection methods.
    assertEquals(10, helper.eval("[1, 2, 3].collect { it * 2 }.findAll { it > 2 }.sum()"));
    assertEquals("a1,b2", helper.eval("[a: 1, b: 2].collect { k, v -> k + v }.join(',')"));
    assertEquals(List.of(1, 2, 3), helper.eval("[3, 1, 2].sort { a, b -> a <=> b }"));
    assertEquals(6, helper.eval("[1, 2, 3].inject(0) { acc, v -> acc + v }"));
    assertEquals(
        Map.of(true, List.of(2), false, List.of(1, 3)),
        helper.eval("[1, 2, 3].groupBy { it % 2 == 0 }"));
    assertEquals(List.of(0, 1, 2), helper.eval("def l = []; 3.times { l << it }; l"));
    assertEquals(
        List.of("0:a", "1:b"),
        helper.eval("def l = []; ['a', 'b'].eachWithIndex { v, i -> l << i + ':' + v }; l"));

    // Nested closures, captured variables, default parameters, recursion.
    assertEquals(
        List.of(11, 12),
        helper.eval("def outer = { x -> [1, 2].collect { y -> x + y } }; outer(10)"));
    assertEquals(3, helper.eval("def n = 1; def inc = { -> n += 2 }; inc(); n"));
    assertEquals(5, helper.eval("def f = { x = 5 -> x }; f()"));
    assertEquals(
        120, helper.eval("def fact; fact = { n -> n <= 1 ? 1 : n * fact(n - 1) }; fact(5)"));

    // Closure owner and delegate are readable.
    assertEquals(true, helper.eval("def c = { -> delegate == owner }; c()"));

    // Closure returned to Java and called later.
    Closure<?> closure = (Closure<?>) helper.eval("{ a, b -> a + b }");
    assertEquals(3, closure.call(1, 2));
  }

  // Annotations can trigger AST transformations that run code while the script is compiled, before
  // any runtime check exists, so scripts cannot use any annotation.
  @Test
  void testAnnotationsAreRejected() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // @ASTTest runs its closure at compile time: it must be rejected before it runs.
    final String astTest =
        "@groovy.transform.ASTTest(value = { throw new RuntimeException('COMPILE-TIME') })"
            + " def x = 1; x";
    Throwable ex = assertThrows(RuntimeException.class, () -> helper.eval(astTest));
    assertTrue(hasPolicyException(ex), () -> "Expected a ScriptPolicyException, got: " + ex);
    for (Throwable c = ex; c != null; c = c.getCause()) {
      assertFalse("COMPILE-TIME".equals(c.getMessage()), "The @ASTTest closure must not run");
    }

    // Other transformations: dependency download, script base class replacement, alias.
    assertBlockedByPolicy(helper, "@Grab('org.example:none:1.0') import java.util.List; 1");
    assertBlockedByPolicy(helper, "@groovy.transform.BaseScript groovy.lang.Script base; 1");
    assertBlockedByPolicy(
        helper,
        "@groovy.transform.AnnotationCollector([groovy.transform.ASTTest]) @interface Alias {}; 1");

    // Every position an annotation can be in.
    assertBlockedByPolicy(helper, "@SuppressWarnings('x') def x = 1; x");
    assertBlockedByPolicy(helper, "@groovy.transform.Field def x = 1; x");
    assertBlockedByPolicy(helper, "class X { @groovy.transform.Generated def m() {} }; 1");
    assertBlockedByPolicy(helper, "@SuppressWarnings('x') import java.util.List; 1");
    assertBlockedByPolicy(helper, "@SuppressWarnings('x') def f() { 1 }; f()");
    assertBlockedByPolicy(helper, "def f(@SuppressWarnings('x') x) { x }; f(1)");
    assertBlockedByPolicy(helper, "{ @SuppressWarnings('x') x -> x }(1)");
    assertBlockedByPolicy(helper, "for (@SuppressWarnings('x') int i = 0; i < 1; i++) {}; 1");
    assertBlockedByPolicy(helper, "@groovy.transform.ToString class X {}; 1");
    assertBlockedByPolicy(helper, "class X { @SuppressWarnings('x') def m() {} }; 1");
    assertBlockedByPolicy(helper, "class X { @SuppressWarnings('x') String p }; 1");
    assertBlockedByPolicy(helper, "class X { @SuppressWarnings('x') private String f }; 1");
    assertBlockedByPolicy(helper, "class X { X(@SuppressWarnings('x') String p) {} }; 1");

    // The parser drops annotations on a for-in loop variable and on a catch parameter: they never
    // reach the compiler, so they cannot trigger a transformation.
    final String compileTime =
        "@groovy.transform.ASTTest(value = { throw new RuntimeException('COMPILE-TIME') })";
    assertEquals("compiled", helper.eval("for (" + compileTime + " x in [1]) {}; 'compiled'"));
    assertEquals(
        "compiled",
        helper.eval("try { 1 } catch (" + compileTime + " Exception e) { 2 }; 'compiled'"));

    // @Override and @Deprecated are allowed, with constant values.
    assertEquals(
        "b",
        helper.eval(
            "class A { String toString() { 'a' } };"
                + " class B extends A { @Override String toString() { 'b' } }; new B().toString()"));
    assertEquals(
        "b",
        helper.eval(
            "import java.util.*; class B { @java.lang.Override String toString() { 'b' } };"
                + " new B().toString()"));
    assertEquals(1, helper.eval("@Deprecated def f() { 1 }; f()"));
    assertEquals(
        1, helper.eval("@Deprecated(since = '1.0', forRemoval = true) def f() { 1 }; f()"));
    assertEquals(1, helper.eval("@Deprecated def x = 1; x"));
    assertEquals(1, helper.eval("class C { @Deprecated int v = 1 }; new C().v"));
    assertEquals(1, helper.eval("@java.lang.Deprecated class C { int v = 1 }; new C().v"));

    // ...but only when they cannot refer to another type: an import alias, a star import, a
    // class of that name declared in the script, or non-constant values.
    final String astTestValue = "(value = { throw new RuntimeException('COMPILE-TIME') })";
    Throwable aliased =
        assertThrows(
            Throwable.class,
            () ->
                helper.eval(
                    "import groovy.transform.ASTTest as Override; @Override"
                        + astTestValue
                        + " def x = 1; x"));
    assertTrue(hasPolicyException(aliased), () -> "Expected a ScriptPolicyException: " + aliased);
    for (Throwable c = aliased; c != null; c = c.getCause()) {
      assertFalse("COMPILE-TIME".equals(c.getMessage()), "The @ASTTest closure must not run");
    }
    assertBlockedByPolicy(
        helper, "import groovy.transform.*; class B { @Override String toString() { 'b' } }; 1");
    assertBlockedByPolicy(helper, "import static java.lang.Math.*; @Deprecated def x = 1; x");
    assertBlockedByPolicy(helper, "@interface Deprecated {}; @Deprecated def x = 1; x");
    assertBlockedByPolicy(helper, "@Deprecated(since = { 'x' }) def f() { 1 }; f()");

    // An @ character that is not an annotation is fine.
    assertEquals("user@example.com", helper.eval("'user@example.com'"));
    assertEquals("a@1", helper.eval("def n = 1; \"a@${n}\".toString()"));
    assertEquals(3, helper.eval("def f(x) { x + 1 }; f(2)"));
    assertEquals(2, helper.eval("try { 1 / 0 } catch (ArithmeticException e) { 2 }"));
  }

  // Classes declared in a script are checked like the script itself, wherever their code is.
  @Test
  void testScriptClassesAreChecked() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());
    final String denied = "System.getenv('HOME')";

    assertBlockedByPolicy(helper, "class X { def go() { " + denied + " } }; new X().go()");
    assertBlockedByPolicy(helper, "class X { static go() { " + denied + " } }; X.go()");
    assertBlockedByPolicy(helper, "class X { def v = " + denied + " }; new X().v");
    assertBlockedByPolicy(helper, "class X { static v = " + denied + " }; X.v");
    assertBlockedByPolicy(helper, "class X { static { " + denied + " } }; new X(); 1");
    assertBlockedByPolicy(helper, "class X { { " + denied + " } }; new X(); 1");
    assertBlockedByPolicy(helper, "class X { X() { " + denied + " } }; new X(); 1");
    assertBlockedByPolicy(
        helper, "class X { def go() { [1].collect { " + denied + " } } }; new X().go()");
    assertBlockedByPolicy(
        helper, "class X { class Y { def go() { " + denied + " } } }; new X.Y(new X()).go()");
    assertBlockedByPolicy(helper, "enum E { A; def go() { " + denied + " } }; E.A.go()");
    assertBlockedByPolicy(
        helper, "def r = new Runnable() { void run() { " + denied + " } }; r.run(); 1");

    // The GDK overrides apply to script classes too.
    assertBlockedByPolicy(
        helper, "class X { def go() { 'axelor-nonexistent-binary-xyz'.execute() } }; new X().go()");
  }

  // What a script class inherits is not compiled from the script, so its supertypes must be
  // allowed.
  @Test
  void testScriptClassSupertypesAreChecked() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    assertBlockedByPolicy(
        helper, "class X extends java.io.File { X() { super('/etc/hostname') } }; new X().text");
    assertBlockedByPolicy(helper, "class X extends Thread {}; 1");
    assertBlockedByPolicy(helper, "class X implements java.io.Closeable { void close() {} }; 1");
    assertBlockedByPolicy(helper, "new java.io.File('/etc/hostname') {}.text");

    // finalize() would run on the JVM finalizer thread, outside of any evaluation.
    assertBlockedByPolicy(helper, "class X { void finalize() {} }; 1");

    // A class extending groovy.lang.Script (not SandboxScript) would get the original
    // evaluate(), running code without the policy. It is a script-declared class like any other,
    // so it must be checked too, although Groovy's ClassNode.isScript() is true for it.
    final String nested = "\"System.getenv('HOME')\"";
    assertBlockedByPolicy(
        helper,
        "class Foo extends Script { def run() { null } }; new Foo().evaluate(" + nested + ")");
    assertBlockedByPolicy(helper, "abstract class Foo extends Script {}; 1");
    assertBlockedByPolicy(
        helper, "class O { static class Foo extends Script { def run() { null } } }; 1");
    assertBlockedByPolicy(helper, "new Script() { def run() { evaluate(" + nested + ") } }.run()");
  }

  // A package would let a script declare a class under the name of an existing class, which the
  // script would then resolve to instead of the real one: the sandbox base class, helpers or
  // checks. Scripts cannot declare a package, so their classes are in the default package.
  @Test
  void testPackageDeclarationsAreRejected() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Replacing the SandboxScript base class: evaluate() would run code without the policy.
    assertBlockedByPolicy(
        helper,
        "package com.axelor.script; abstract class SandboxScript extends Script {};"
            + " evaluate(\"System.getenv('HOME')\")");
    // Replacing the checks.
    assertBlockedByPolicy(
        helper,
        "package com.axelor.script; class GroovySandbox { static check(o) { o };"
            + " static checkSpread(o) { o }; static timeout() {}; static constructed(t, o) { o } };"
            + " System.getenv('HOME')");
    // Replacing the __bean__ / __repo__ helpers.
    assertBlockedByPolicy(
        helper,
        "package com.axelor.script; class GroovyScriptSupport { static class Helpers {"
            + " static getBean(Class c) { 'fake' }; static getRepo(Class c) { null } } };"
            + " __bean__(String)");

    // Any package.
    assertBlockedByPolicy(helper, "package com.example; 1");
    assertBlockedByPolicy(helper, "package foo; class A { def v() { 2 } }; new A().v()");

    // Without package, script classes keep working.
    assertEquals(2, helper.eval("class A { def v() { 2 } }; new A().v()"));

    // The package the template engines generate is allowed exactly (it contains no existing
    // class), and the code in it is still sandboxed.
    assertEquals(1, helper.eval("package groovy.tmp.templates; 1"));
    assertBlockedByPolicy(helper, "package groovy.tmp.templates; System.getenv('HOME')");
    assertBlockedByPolicy(
        helper,
        "package groovy.tmp.templates; class GroovySandbox { static check(o) { o } };"
            + " System.getenv('HOME')");
    assertBlockedByPolicy(helper, "package groovy.tmp.templates.sub; 1");
    assertBlockedByPolicy(helper, "package groovy.tmp; 1");
    assertBlockedByPolicy(helper, "package groovy.tmp.templatesx; 1");
  }

  // A script class instance called later by Java code, outside the evaluation, stays sandboxed.
  @Test
  void testScriptClassEscapingTheScriptIsSandboxed() {
    final String missingCmd = "axelor-nonexistent-binary-xyz";

    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());
    Object obj =
        helper.eval(
            "class X { String toString() { '"
                + missingCmd
                + "'.\"${'exec' + 'ute'}\"(); 'x' } }; new X()");

    Throwable ex = assertThrows(RuntimeException.class, obj::toString);
    assertTrue(hasPolicyException(ex), () -> "Expected a ScriptPolicyException, got: " + ex);
  }

  // Common class usage must keep working.
  @Test
  void testScriptClassesStillWork() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Fields, properties, constructors, methods, static members.
    assertEquals(
        "Hello, Ada",
        helper.eval(
            "class Greeter { String name; Greeter(String name) { this.name = name };"
                + " def greet() { \"Hello, ${name}\".toString() } }; new Greeter('Ada').greet()"));
    assertEquals(3, helper.eval("class P { int x = 1; int y = 2 }; def p = new P(); p.x + p.y"));
    assertEquals(5, helper.eval("class M { static int add(int a, int b) { a + b } }; M.add(2, 3)"));
    assertEquals(
        2, helper.eval("class C { static int n = 0; C() { n++ } }; new C(); new C(); C.n"));
    assertEquals(
        "a",
        helper.eval(
            "class D { String v; D() { this('a') }; D(String v) { this.v = v } };" + " new D().v"));

    // Inheritance between script classes, super calls, overriding.
    assertEquals(
        "BA",
        helper.eval(
            "class A { def f() { 'A' } }; class B extends A { def f() { 'B' + super.f() } };"
                + " new B().f()"));
    assertEquals(
        "x:1",
        helper.eval(
            "class A { String n; A(String n) { this.n = n } };"
                + " class B extends A { int v; B(String n, int v) { super(n); this.v = v } };"
                + " def b = new B('x', 1); b.n + ':' + b.v"));
    assertEquals("hi", helper.eval("class A { String toString() { 'hi' } }; new A().toString()"));

    // Allowed supertypes, interfaces, enums, anonymous and nested classes.
    assertEquals(
        List.of(3, 2, 1),
        helper.eval(
            "class Desc implements Comparator<Integer> {"
                + " int compare(Integer a, Integer b) { b <=> a } };"
                + " [1, 3, 2].toSorted(new Desc())"));
    assertEquals(
        "B",
        helper.eval(
            "interface Named { String name() }; class N implements Named {"
                + " String name() { 'B' } }; new N().name()"));
    assertEquals(
        List.of("A", "B"), helper.eval("enum E { A, B }; E.values().collect { it.name() }"));
    assertEquals("B", helper.eval("enum E { A, B }; E.valueOf('B').name()"));
    assertEquals(
        1,
        helper.eval(
            "def c = new Comparator<Integer>() { int compare(Integer a, Integer b) {"
                + " a <=> b } }; c.compare(2, 1)"));
    assertEquals(
        "in", helper.eval("class O { static class I { def v() { 'in' } } }; new O.I().v()"));
    assertEquals(
        "k",
        helper.eval(
            "class Box extends HashMap<String, String> {}; def b = new Box();"
                + " b.put('k', 'v'); b.keySet().first()"));

    // Closures and collections inside class methods.
    assertEquals(
        List.of(2, 4),
        helper.eval("class S { def twice(l) { l.collect { it * 2 } } }; new S().twice([1, 2])"));
  }

  // Methods of allowed classes changing (or reading) state shared by the whole application are
  // guarded, whatever route reaches them; trusted code can still call them.
  @Test
  void testGuardedMethodsAreDenied() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());
    Locale locale = Locale.getDefault();
    Locale format = Locale.getDefault(Locale.Category.FORMAT);
    TimeZone zone = TimeZone.getDefault();
    try {
      final String set = "Locale.FRANCE";
      for (String code :
          List.of(
              "Locale.setDefault(" + set + ")",
              "Locale.setDefault(Locale.Category.FORMAT, " + set + ")",
              "Locale.\"set${'Default'}\"(" + set + ")",
              "Locale.invokeMethod('setDefault', " + set + ")",
              "Locale.default = " + set,
              "Locale.GERMANY.setDefault(" + set + ")",
              "Locale.GERMANY.with { setDefault(" + set + ") }",
              "def c = { -> setDefault("
                  + set
                  + ") }; c.delegate = Locale; c.resolveStrategy = 1; c()",
              "[" + set + "].each(Locale.&setDefault)",
              "Locale.getMethod('setDefault', Locale).invoke(null, " + set + ")",
              "Locale.metaClass = null; Locale.setDefault(" + set + ")",
              "Locale.setMetaClass(null); Locale.setDefault(" + set + ")",
              "Locale.metaClass { }",
              "TimeZone.setDefault(TimeZone.getTimeZone('Pacific/Kiritimati'))",
              "SimpleTimeZone.setDefault(TimeZone.getTimeZone('Pacific/Kiritimati'))",
              "class T extends SimpleTimeZone { T() { super(0, 'T') } }; T.setDefault(new T())",
              "java.time.zone.ZoneRulesProvider.refresh()",
              "ServiceLoader.load(Runnable).iterator().hasNext()",
              "Integer.getInteger('java.specification.version')",
              "Long.getLong('java.specification.version')",
              "Boolean.getBoolean('java.awt.headless')",
              "com.axelor.common.Inflections.getInstance().plural('(.*)', 'zz')",
              "com.axelor.common.Inflections.getInstance().with { irregular('a', 'b') }")) {
        assertBlockedByPolicy(helper, code + "; 1");
        assertEquals(locale, Locale.getDefault(), () -> "Locale changed by: " + code);
        assertEquals(
            format, Locale.getDefault(Locale.Category.FORMAT), () -> "Changed by: " + code);
        assertEquals(zone, TimeZone.getDefault(), () -> "Time zone changed by: " + code);
      }

      // Their other methods still work.
      assertEquals("fr_FR", helper.eval("Locale.FRANCE.toString()"));
      assertEquals(locale, helper.eval("Locale.getDefault()"));
      assertEquals(14L, helper.eval("Integer.valueOf('12') + Long.parseLong('1') + 1"));
      assertEquals(
          com.axelor.common.Inflections.getInstance().pluralize("contact"),
          helper.eval("com.axelor.common.Inflections.getInstance().pluralize('contact')"));

      // Trusted code is not affected.
      InvokerHelper.invokeStaticMethod(Locale.class, "setDefault", new Object[] {Locale.ITALY});
      assertEquals(Locale.ITALY, Locale.getDefault());
    } finally {
      Locale.setDefault(locale);
      Locale.setDefault(Locale.Category.FORMAT, format);
      TimeZone.setDefault(zone);
    }
  }

  // Reflection cannot reach what a direct call cannot.
  @Test
  void testReflectionIsLimitedToPublicMembers() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    assertBlockedByPolicy(helper, "'abc'.getClass().getDeclaredMethod('isLatin1').invoke('abc')");
    assertBlockedByPolicy(
        helper, "def m = 'abc'.getClass().getDeclaredMethod('isLatin1'); m.setAccessible(true)");
    assertBlockedByPolicy(helper, "'abc'.getClass().getDeclaredField('value').get('abc')");
    assertBlockedByPolicy(
        helper, "Locale.getMethod('setDefault', Locale).invoke(null, Locale.FRANCE)");

    assertEquals(3, helper.eval("'abc'.getClass().getMethod('length').invoke('abc')"));
  }

  // A denied class is never constructed, its constructor could have side effects: here, create a
  // file. Groovy also constructs objects when converting a list or map to a type.
  @Test
  void testDeniedClassesAreNotConstructed() throws Exception {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());
    java.nio.file.Path dir =
        java.nio.file.Files.createDirectories(
            java.nio.file.Path.of("build", "tmp", "script-policy"));
    java.nio.file.Path file =
        dir.resolve("constructed-" + java.util.UUID.randomUUID() + ".txt").toAbsolutePath();
    String path = "'" + file.toString().replace("\\", "/") + "'";
    try {
      for (String code :
          List.of(
              "new Formatter(" + path + ")",
              "new java.io.FileOutputStream(" + path + ")",
              "Formatter f = [" + path + "]",
              "java.io.FileOutputStream f = [" + path + "]",
              "[" + path + "] as java.io.FileOutputStream",
              "[" + path + "].asType(java.io.FileOutputStream)",
              "def t = java.io.FileOutputStream; [" + path + "].asType(t)",
              "class X { java.io.FileOutputStream f = [" + path + "] }; new X()",
              "java.io.FileOutputStream g() { [" + path + "] }; g()",
              "def g(java.io.FileOutputStream o) { o }; g(null)",
              // array initializers convert each element to the element type
              "new java.io.FileOutputStream[] { [" + path + "] }",
              "new java.io.FileOutputStream[][] { new java.io.FileOutputStream[] { ["
                  + path
                  + "] } }",
              "def a = new Formatter[] { [" + path + "] }")) {
        assertBlockedByPolicy(helper, code + "; 1");
        assertFalse(java.nio.file.Files.exists(file), () -> "File created by: " + code);
      }

      // Allowed types, interfaces and abstract classes can still be written and converted to.
      assertEquals(
          5,
          helper.eval(
              "List l = [1, 2]; Set s = [1] as Set; BigDecimal d = 1.5; String[] a = ['x'];"
                  + " int i = 2; l.size() + s.size() + i"));
      assertEquals(
          1,
          helper.eval(
              "Closure c = { 1 }; Runnable r = { }; Comparator<Integer> k = { a, b -> a <=> b };"
                  + " c()"));
      assertEquals(
          2026, helper.eval("java.time.LocalDate d = java.time.LocalDate.of(2026, 1, 2); d.year"));
      assertEquals(3, helper.eval("class P { int x; P(int x) { this.x = x } }; P p = [3]; p.x"));
      assertEquals(12, helper.eval("'12' as Integer"));
      assertEquals(2, helper.eval("new ArrayList([1, 2]).size()"));
      assertEquals(
          List.of("a", "b"), Arrays.asList((Object[]) helper.eval("new String[] { 'a', 'b' }")));
      // an array without elements to convert (it is checked as its element type when used)
      assertEquals(
          1, helper.eval("def a = new java.io.File[2]; def b = new java.io.File[] { }; 1"));
    } finally {
      java.nio.file.Files.deleteIfExists(file);
    }
  }

  // Groovy categories and mixins expose methods of any class (e.g. System.getenv, GroovyShell)
  // on allow-listed receivers, so the receiver check does not apply to them.
  @Test
  void testCategoriesAndMixinsAreDenied() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    assertBlockedByPolicy(helper, "use(System) { 'PATH'.getenv() }");
    assertBlockedByPolicy(helper, "def m = 'use'; \"$m\"(System) { 'PATH'.getenv() }");
    assertBlockedByPolicy(helper, "use([System]) { 'PATH'.getenv() }");
    assertBlockedByPolicy(helper, "String.mixin(GroovyShell); 'x'.evaluate('1 + 1')");
  }

  // A statically imported method is called without a receiver, so its class is checked when the
  // script is compiled.
  @Test
  void testStaticImportsAreChecked() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    assertBlockedByPolicy(helper, "import static java.lang.System.getenv; getenv('PATH')");
    assertBlockedByPolicy(helper, "import static java.lang.System.getenv as env; env('PATH')");
    assertBlockedByPolicy(helper, "import static java.lang.System.*; getenv('PATH')");

    // Allowed classes can still be statically imported.
    assertEquals(2, helper.eval("import static java.lang.Math.max; max(1, 2)"));

    // So can classes declared in the script (not compiled yet when imports are checked; their code
    // is checked like the rest of the script).
    assertEquals(1, helper.eval("import static Foo.bar; class Foo { static bar() { 1 } }; bar()"));
    assertEquals(2, helper.eval("import static Foo.*; class Foo { static bar() { 2 } }; bar()"));
    assertBlockedByPolicy(
        helper,
        "import static Foo.bar; class Foo { static bar() { System.getenv('HOME') } }; bar()");
  }

  // A closure created by a script stays sandboxed when called later, outside the evaluation.
  @Test
  void testClosureEscapingTheScriptIsSandboxed() {
    final String missingCmd = "axelor-nonexistent-binary-xyz";

    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());
    Closure<?> closure =
        (Closure<?>) helper.eval("{ -> '" + missingCmd + "'.\"${'exec' + 'ute'}\"() }");

    Throwable ex = assertThrows(RuntimeException.class, closure::call);
    assertTrue(hasPolicyException(ex), () -> "Expected a ScriptPolicyException, got: " + ex);
  }

  // The GDK overrides only deny sandboxed callers: trusted code keeps the GDK behavior.
  @Test
  void testGdkOverridesDoNotAffectTrustedCode() {
    final String missingCmd = "axelor-nonexistent-binary-xyz";

    // Reaches ProcessGroovyMethods.execute(String), which fails as the command does not exist.
    Throwable ex =
        assertThrows(
            Exception.class, () -> InvokerHelper.invokeMethod(missingCmd, "execute", null));
    assertFalse(hasPolicyException(ex), () -> "Expected no ScriptPolicyException, got: " + ex);
    assertTrue(
        String.valueOf(ex.getMessage()).contains("Cannot run program"),
        () -> "Expected the process to be started, got: " + ex);
  }

  // GDK IO/resource reads must stay denied. Today the receiver check already blocks them (URL/File
  // are not allow-listed); this guards that surface against a future change reopening file/URL
  // exfiltration. Targets are nonexistent paths, so an unblocked read only throws FileNotFound.
  @Test
  void testGdkResourceAccessIsDenied() {
    final String missingPath = "/axelor-nonexistent-path-xyz";

    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // URL reads (receiver is java.net.URL): ResourceGroovyMethods/IOGroovyMethods.
    assertBlockedByPolicy(helper, "'file:" + missingPath + "'.toURL().text");
    assertBlockedByPolicy(helper, "'file:" + missingPath + "'.toURL().getText()");
    assertBlockedByPolicy(helper, "'file:" + missingPath + "'.toURL().bytes");
    assertBlockedByPolicy(helper, "'file:" + missingPath + "'.toURL().openStream(); 1");
    assertBlockedByPolicy(helper, "new URL('file:" + missingPath + "').text");

    // File read via coercion (receiver is java.io.File): ResourceGroovyMethods.getText(File).
    assertBlockedByPolicy(helper, "('" + missingPath + "' as File).text");
  }

  // A nested evaluation escapes the sandbox entirely: Script.evaluate(String) / Eval / new
  // GroovyShell() compile the nested code without the policy's AST customizers, so inside it even
  // Runtime/ProcessBuilder run unchecked.
  //
  // Eval.me and new GroovyShell() are denied by the receiver check (their class is not
  // allow-listed).
  // Script.evaluate(...) is not caught that way because the receiver is the trusted running script,
  // so SandboxScript, the base class of compiled scripts, overrides it to throw.
  @Test
  void testNestedEvaluationIsSandboxed() {
    final String missingCmd = "axelor-nonexistent-binary-xyz";

    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Already denied by the receiver check (guards).
    assertBlockedByPolicy(helper, "Eval.me('1+1')");
    assertBlockedByPolicy(helper, "new GroovyShell(); 1");

    // Sandbox escape via Script.evaluate: the nested code would run unchecked and reach the process
    // layer; the evaluate() call itself is denied.
    assertBlockedByPolicy(
        helper, "evaluate(\"new ProcessBuilder('" + missingCmd + "').start(); 1\")");
    assertBlockedByPolicy(
        helper, "evaluate(\"Runtime.getRuntime().exec('" + missingCmd + "'); 1\")");
    assertBlockedByPolicy(helper, "evaluate(\"['" + missingCmd + "'].execute(); 1\")");

    // Other dispatch routes to evaluate().
    final String nested = "\"new ProcessBuilder('" + missingCmd + "').start(); 1\"";
    assertBlockedByPolicy(helper, "this.evaluate(" + nested + ")");
    assertBlockedByPolicy(helper, "invokeMethod('evaluate', " + nested + ")");
    assertBlockedByPolicy(helper, "this.\"${'eval' + 'uate'}\"(" + nested + ")");
    assertBlockedByPolicy(helper, "[1].with { evaluate(" + nested + ") }");

    // The other Script method compiling and running code: run(File, String[]). The File is null,
    // so the call reaches the SandboxScript override itself. (A script cannot get a File, nor even
    // cast to it, so evaluate(File) cannot be reached.)
    assertBlockedByPolicy(helper, "run(null, [] as String[])");
    assertBlockedByPolicy(helper, "invokeMethod('run', [null, [] as String[]] as Object[])");
    assertBlockedByPolicy(helper, "this.\"${'r' + 'un'}\"(null, [] as String[])");
  }
}
