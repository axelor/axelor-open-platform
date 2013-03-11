package com.axelor.web;

import java.util.Set;

import com.axelor.db.JpaModule;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.rpc.filter.Filter;
import com.google.inject.AbstractModule;

public class TestModule extends AbstractModule {

	public static class MySecurity implements JpaSecurity {

		@Override
		public Filter getFilter(AccessType type, Class<? extends Model> model,
				Object... ids) {
			return null;
		}

		@Override
		public Set<AccessType> perms(Class<? extends Model> model) {
			return null;
		}

		@Override
		public Set<AccessType> perms(Class<? extends Model> model, Long id) {
			return null;
		}

		@Override
		public Set<AccessType> perms(Model entity) {
			return null;
		}

		@Override
		public void check(AccessType type, Class<? extends Model> model) {
			
		}

		@Override
		public void check(AccessType type, Class<? extends Model> model, Long id) {
			
		}

		@Override
		public void check(AccessType type, Model entity) {
			
		}
	}

	@Override
	protected void configure() {
		
		// bind fake security implementation
		bind(JpaSecurity.class).to(MySecurity.class);
		
		// initialize JPA
		install(new JpaModule("testUnit", true, false));
	}
}
