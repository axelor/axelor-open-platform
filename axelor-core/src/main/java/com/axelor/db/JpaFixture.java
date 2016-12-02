/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
package com.axelor.db;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.Tag;

import com.axelor.common.ResourceUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

/**
 * This class can be used to load test data for JPA entities.
 * 
 * <p>
 * It processes YAML files located in {@code /fixtures}.
 * 
 * <p>
 * For example, the following schema:
 * 
 * <pre>
 * 
 * &#64;Entity
 * &#64;Table(name = "CONTACT_CIRCLE")
 * public class Circle extends Model {
 *     private String code;
 *     private String name;
 *     ...
 *     ...
 * }
 * 
 * &#64;Entity
 * &#64;Table(name = "CONTACT_CONTACT")
 * public class Contact extends Model {
 *     private String firstName;
 *     private String lastName;
 *     private String email;
 *     
 *     &#64;ManyToMany
 *     private Set&lt;Circle&gt; circles;
 *     ...
 *     ...
 *     ...
 * </pre>
 * 
 * The fixtures should be defined like this:
 * 
 * <pre>
 *  - !Circle: &amp;family
 *   code: family
 *   name: Family
 * 
 * - !Circle: &amp;friends
 *   code: friends
 *   name: Friends
 *   
 * - !Circle: &amp;business
 *   code: business
 *   name: Business
 * 
 * - !Contact:
 *   firstName: John
 *   lastName: Smith
 *   email: j.smith@gmail.com
 *   circles:
 *     - *friends
 *     - *business
 *   
 * - !Contact:
 *   firstName: Tin
 *   lastName: Tin
 *   email: tin.tin@gmail.com
 *   circles:
 *     - *business
 * </pre>
 * 
 * <p>
 * In order to use the fixture data, the {@link JpaFixture} must be injected.
 * 
 * <pre>
 * &#64;RunWith(GuiceRunner.class)
 * &#64;GuiceModules({MyModule.class})
 * class FixtureTest {
 * 
 *     &#64;Inject
 *     private JpaFixture fixture;
 *     
 *     &#64;Before
 *     public void setUp() {
 *         fixture.load("demo-data.yml");
 *         fixture.load("demo-data-extra.yml");
 *     }
 *     
 *     &#64;Test
 *     public void testCount() {
 *         Assert.assertEqual(2, Contact.all().count());
 *         ...
 *     }
 *     ...
 * }
 * </pre>
 */
public class JpaFixture {

	private InputStream read(String resource) {
		return ResourceUtils.getResourceStream("fixtures/" + resource);
	}

	@Transactional
	public void load(String fixture) {

		final InputStream stream = read(fixture);
		final Map<Node, Object> objects = Maps.newLinkedHashMap();

		if (stream == null) {
			throw new IllegalArgumentException("No such fixture found: " + fixture);
		}
		
		final Constructor ctor = new Constructor() {
			{
				yamlClassConstructors.put(NodeId.scalar, new TimeStampConstruct());
			}
			
			class TimeStampConstruct extends Constructor.ConstructScalar {

				Construct dateConstructor = yamlConstructors.get(Tag.TIMESTAMP);
				
				@Override
				public Object construct(Node nnode) {
					if (nnode.getTag().equals(Tag.TIMESTAMP)) {
						Date date = (Date) dateConstructor.construct(nnode);
						if (nnode.getType() == LocalDate.class) {
							return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
						}
						if (nnode.getType() == LocalDateTime.class) {
							return date.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
						}
						return date.toInstant().atZone(ZoneOffset.UTC);
					} else {
						return super.construct(nnode);
					}
				}

			}
			
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
		
		for(Class<?> klass : JPA.models()) {
			ctor.addTypeDescription(new TypeDescription(klass, "!" + klass.getSimpleName() + ":"));
		}

		Yaml data = new Yaml(ctor);
		data.load(stream);
		
		for(Object item : Lists.reverse(Lists.newArrayList(objects.values()))) {
			try {
				JPA.manage((Model) item);
			}catch(Exception e) {
			}
		}
	}
}
