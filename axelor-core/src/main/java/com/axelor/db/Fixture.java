package com.axelor.db;

import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

/**
 * This class can be used to load test data for JPA entities.
 * 
 * <p>
 * It processes YAML files located in {@code META-INF/fixtures}. A fixture
 * consists of two files with {@code .resolver.yml} and {@code .data.yml}
 * suffixes.
 * 
 * <p>
 * The resolver file defiles short names for fully qualified entity name to use
 * in the data file.
 * 
 * <p>
 * For example, the following schema:
 * 
 * <pre>
 * 
 * &#64;Entity
 * &#64;Table(name = "PERSON_GROUP")
 * public class Group extends Model {
 *     private String name;
 *     private String title;
 *     ...
 *     ...
 * }
 * 
 * &#64;Entity
 * public class Person extends Model {
 *     private String name;
 *     private String age;
 *     private String lang;
 *     
 *     &#64;ManyToMany
 *     private List<Group> groups;
 *     ...
 *     ...
 *     ...
 * </pre>
 * 
 * The fixtures should be defined like this:
 * 
 * 1. demo.resolver.yml
 * 
 * <pre>
 * Group: com.axelor.domain.Group
 * Person: com.axelor.domain.Person
 * </pre>
 * 
 * 2. demo.data.yml
 * 
 * <pre>
 *  - !Group: &family
 *   name: family
 *   title: Family
 * 
 * - !Group: &friends
 *   name: friends
 *   title: Friends
 *   
 * - !Group: &business
 *   name: business
 *   title: Business
 * 
 * - !Person:
 *   name: Some
 *   age: 27
 *   lang: en
 *   groups:
 *     - *family
 *   
 * - !Person:
 *   name: One
 *   age: 23
 *   lang: fr
 *   groups:
 *     - *friends
 *     - *business
 * 
 * - !Person:
 *   name: Else
 *   age: 31
 *   lang: hi
 *   groups:
 *     - *business
 * </pre>
 * 
 * <p>
 * In order to use the fixture data, you should inject Guice {@link Injector}
 * object before using the {@link Fixture} instance.
 * 
 * <pre>
 * &#64;RunWith(GuiceRunner.class)
 * &#64;GuiceModules({MyModule.class})
 * class FixtureTest {
 * 
 *     &#64;Inject
 *     Fixture fixture;
 *     
 *     &#64;Inject
 *     Manager&lt;Person&gt; mp;
 *     
 *     &#64;Inject
 *     Manager&lt;Group&gt; mg;
 *     
 *     &#64;Before
 *     public void setUp() {
 *         fixture.load("data");
 *         fixture.load("some_other_fixture");
 *     }
 *     
 *     &#64;Test
 *     public void testCount() {
 *         Assert.assertEqual(3, mg.all().count());
 *         ...
 *     }
 *     ...
 * }
 * </pre>
 */
public class Fixture {
	
	private InputStream read(String resource) {
		return Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(("META-INF/fixtures/" + resource));
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public void load(String fixture) {

		Yaml yaml = new Yaml();

		InputStream is = read(fixture + ".resolver.yml");
		InputStream ds = read(fixture + ".data.yml");

		if (is == null || ds == null) {
			throw new IllegalArgumentException("No such fixture: " + fixture
					+ ".yml");
		}

		final Map<String, String> classes = (Map<String, String>) yaml.load(is);
		final Map<Node, Object> objects = Maps.newLinkedHashMap();
		
		Constructor ctor = new Constructor() {
			
			@Override
			protected Object constructObject(Node node) {
				
				Object obj = super.constructObject(node);
				
				if (objects.containsKey(node)) {
					return objects.get(node);
				}
				
				if (obj instanceof Model) {
					objects.put(node, obj);
					return obj;
				}
				return obj;
			}
		};
		
		for (String key : classes.keySet()) {
			try {
				Class<?> klass = Class.forName(classes.get(key));
				ctor.addTypeDescription(new TypeDescription(klass, "!" + key
						+ ":"));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(String.format(
						"Invalid fixture resolver '%s': %s: %s", fixture, key,
						classes.get(key)));
			}
		}

		Yaml data = new Yaml(ctor);
		data.load(ds);
		
		for(Object item : Lists.reverse(Lists.newArrayList(objects.values()))) {
			try {
				JPA.manage((Model) item);
			}catch(Exception e) {
			}
		}
	}
}
