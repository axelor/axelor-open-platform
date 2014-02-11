package com.axelor.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;

/**
 * The application modules can provide implementations of the
 * {@link AxelorModule} which are automatically scanned and installed my the
 * {@link AppModule} before the application starts.
 * 
 * The module implementations can {@link #configure()} the guice bindings.
 */
public abstract class AxelorModule extends AbstractModule {
	
	protected Logger log = LoggerFactory.getLogger(getClass());
}
