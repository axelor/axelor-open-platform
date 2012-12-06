package com.axelor.test;

import static junit.framework.Assert.*;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;

@GuiceModules(InjectedTest.Module.class)
public class InjectedTest extends GuiceTest {

	static class Module extends AbstractModule {
		
		@Override
		protected void configure() {
		}
	}
	
	@Inject
	Injector injector;
	
	@Test
	public void test() {
		assertNotNull(injector);
	}
}
