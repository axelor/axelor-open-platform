/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * JUnit4 test runner that creates injector using the modules provided with
 * {@link GuiceModules} configuration.
 * 
 * Here is a simple test:
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
 * 
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
