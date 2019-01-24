/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.ArrayList;
import java.util.List;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * JUnit4 test runner that creates injector using the modules provided with {@link GuiceModules}
 * configuration.
 *
 * <p>Here is a simple test:
 *
 * <pre>
 *
 * &#064;RunWith(GuiceRunner.class)
 * &#064;GuiceModules({MyModule.class})
 * public class MyTest {
 *
 * 	&#064;Inject
 * 	Foo foo;
 *
 * 	public void testFooInjected() {
 * 		assertNotNull(foo);
 * 	}
 * }
 *
 * </pre>
 */
public class GuiceRunner extends BlockJUnit4ClassRunner {

  private final Injector injector;

  public GuiceRunner(Class<?> klass) throws InitializationError {
    super(klass);
    this.injector = Guice.createInjector(getModules(klass));
  }

  protected List<Module> getModules(Class<?> klass) throws InitializationError {

    GuiceModules guiceModules = klass.getAnnotation(GuiceModules.class);
    if (guiceModules == null) {
      throw new InitializationError("No Guice modules specified.");
    }

    List<Module> modules = new ArrayList<Module>();

    for (Class<? extends Module> c : guiceModules.value()) {
      try {
        modules.add(c.newInstance());
      } catch (Exception e) {
        throw new InitializationError(e);
      }
    }
    return modules;
  }

  @Override
  public Object createTest() {
    return injector.getInstance(getTestClass().getJavaClass());
  }

  /**
   * Get the Guice injector.
   *
   * @return the injector
   */
  protected Injector getInjector() {
    return injector;
  }

  @Override
  protected void validateZeroArgConstructor(List<Throwable> errors) {
    // Guice can inject constroctor args
  }
}
