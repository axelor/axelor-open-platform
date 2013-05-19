package com.axelor.meta;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TestResolver {

	private ModuleResolver resolver = new ModuleResolver();

	@Test
	public void test() {
		
		resolver.add("axelor-auth", 	"axelor-core");
		resolver.add("axelor-wkf", 		"axelor-auth", "axelor-core");
		resolver.add("axelor-meta", 	"axelor-data");
		
		resolver.add("axelor-x");
		
		resolver.add("axelor-sale", 	"axelor-contact" );
		resolver.add("axelor-data", 	"axelor-auth", "axelor-core");
		resolver.add("axelor-contact", 	"axelor-auth", "axelor-core","axelor-meta");
		resolver.add("axelor-project", 	"axelor-sale");

		List<String> expected = Lists.newArrayList(
				"axelor-core",
				"axelor-auth",
				"axelor-data",
				"axelor-meta",
				"axelor-contact",
				"axelor-sale");

		Assert.assertEquals(expected, resolver.resolve("axelor-sale"));

		List<String> all = resolver.all();
		
		Assert.assertEquals("axelor-core", all.get(0));
		Assert.assertEquals("axelor-project", all.get(all.size() - 1));
		
	}
}
