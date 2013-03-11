package com.axelor;

import java.util.Set;

import com.axelor.db.JpaModule;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.rpc.filter.Filter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.AbstractModule;

public class MyModule extends AbstractModule {
	
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
		
		bind(JpaSecurity.class).to(MySecurity.class);

		install(new JpaModule("testUnit", false, true));
		
		ObjectMapper mapper = new ObjectMapper();
		
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		bind(ObjectMapper.class).toInstance(mapper);
	}

}
