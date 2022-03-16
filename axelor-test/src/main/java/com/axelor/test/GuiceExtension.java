package com.axelor.test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.RequestScoper;
import com.google.inject.servlet.ServletScopes;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JUnit5 test extension that creates injector using the modules provided with {@link GuiceModules}
 * configuration.
 *
 * <p>Here is a simple test:
 *
 * <pre>
 *
 * &#064;ExtendWith(GuiceExtension.class)
 * &#064;GuiceModules({MyModule.class})
 * public class MyTest {
 *
 *  &#064;Inject
 *  Foo foo;
 *
 *  public void testFooInjected() {
 *      assertNotNull(foo);
 *  }
 * }
 *
 * </pre>
 */
public class GuiceExtension
    implements BeforeAllCallback, AfterAllCallback, TestInstanceFactory, InvocationInterceptor {

  private Injector injector;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    assert injector == null;
    context.getTestClass().ifPresent(klass -> injector = Guice.createInjector(getModules(klass)));
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    injector = null;
  }

  protected List<Module> getModules(Class<?> klass) throws TestInstantiationException {

    GuiceModules guiceModules = klass.getAnnotation(GuiceModules.class);
    if (guiceModules == null) {
      throw new TestInstantiationException("No Guice modules specified.");
    }

    List<Module> modules = new ArrayList<>();

    for (Class<? extends Module> c : guiceModules.value()) {
      try {
        modules.add(c.getDeclaredConstructor().newInstance());
      } catch (Exception e) {
        throw new TestInstantiationException("Unable to create Injector", e);
      }
    }

    modules.add(
        new AbstractModule() {
          @Override
          protected void configure() {
            bindScope(RequestScoped.class, ServletScopes.REQUEST);
          }
        });

    return modules;
  }

  @Override
  public Object createTestInstance(
      TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext)
      throws TestInstantiationException {
    return injector.getInstance(factoryContext.getTestClass());
  }

  @Override
  public void interceptTestMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {

    final RequestScoper scope = ServletScopes.scopeRequest(Collections.emptyMap());
    try (RequestScoper.CloseableScope ignored = scope.open()) {
      InvocationInterceptor.super.interceptTestMethod(
          invocation, invocationContext, extensionContext);
    }
  }
}
