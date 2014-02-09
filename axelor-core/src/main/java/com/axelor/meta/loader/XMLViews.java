package com.axelor.meta.loader;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.AbstractView;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

public class XMLViews {
	
	private static final Logger log = LoggerFactory.getLogger(XMLViews.class);

	private static final String LOCAL_SCHEMA = "object-views_1.0.xsd";
	private static final String REMOTE_SCHEMA = "object-views_" + ObjectViews.VERSION + ".xsd";
	private static final String JAXB_INDENT_KEY = "com.sun.xml.internal.bind.indentString";
	private static final String JAXB_INDENT_STRING = "  ";
	
	private static Marshaller marshaller;
	private static Unmarshaller unmarshaller;

	static {
		try {
			init();
		} catch (JAXBException | SAXException e) {
			throw Throwables.propagate(e);
		}
	}

	private static void init() throws JAXBException, SAXException {
		if (unmarshaller != null) {
			return;
		}
		JAXBContext context = JAXBContext.newInstance(ObjectViews.class);
		unmarshaller = context.createUnmarshaller();
		marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, ObjectViews.NAMESPACE + " " + ObjectViews.NAMESPACE + "/" + REMOTE_SCHEMA);
		marshaller.setProperty(JAXB_INDENT_KEY, JAXB_INDENT_STRING);

		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(Resources.getResource(LOCAL_SCHEMA));

		unmarshaller.setSchema(schema);
		marshaller.setSchema(schema);
	}

	public static ObjectViews unmarshal(InputStream stream) throws JAXBException {
		synchronized (unmarshaller) {
			return (ObjectViews) unmarshaller.unmarshal(stream);
		}
	}
	
	public static ObjectViews unmarshal(String xml) throws JAXBException {
		synchronized (unmarshaller) {
			return (ObjectViews) unmarshaller.unmarshal(new StringReader(prepareXML(xml)));
		}
	}
	
	public static void marshal(ObjectViews views, Writer writer) throws JAXBException {
		synchronized (marshaller) {
			marshaller.marshal(views, writer);
		}
	}
	
	private static String prepareXML(String xml) {
		StringBuilder sb = new StringBuilder("<?xml version='1.0' encoding='UTF-8'?>\n");
		sb.append("<object-views")
		  .append(" xmlns='").append(ObjectViews.NAMESPACE).append("'")
		  .append(" xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'")
		  .append(" xsi:schemaLocation='").append(ObjectViews.NAMESPACE).append(" ")
		  .append(ObjectViews.NAMESPACE + "/" + REMOTE_SCHEMA).append("'")
		  .append(">\n")
		  .append(xml)
		  .append("\n</object-views>");
		return sb.toString();
	}

	private static String strip(String xml) {
		String[] lines = xml.split("\n");
		StringBuilder sb = new StringBuilder();
		for(int i = 2 ; i < lines.length - 1 ; i ++) {
			sb.append(lines[i] + "\n");
		}
		sb.deleteCharAt(sb.length()-1);

		Pattern p = Pattern.compile("^" + JAXB_INDENT_STRING, Pattern.MULTILINE);
		return p.matcher(sb).replaceAll("");
	}
	
	@SuppressWarnings("all")
	public static String toXml(Object obj, boolean strip) {

		ObjectViews views = new ObjectViews();
		StringWriter writer = new StringWriter();

		if (obj instanceof Action) {
			views.setActions(ImmutableList.of((Action) obj));
		}
		if (obj instanceof AbstractView) {
			views.setViews(ImmutableList.of((AbstractView) obj));
		}
		if (obj instanceof List) {
			views.setViews((List) obj);
		}
		try {
			marshal(views, writer);
		} catch (JAXBException e) {
			log.error(e.getMessage(), e);
		}
		if (strip) {
			return strip(writer.toString());
		}
		return writer.toString();
	}
	
	public static ObjectViews fromXML(String xml) throws JAXBException {
		if (Strings.isNullOrEmpty(xml))
			return null;

		if (!xml.trim().startsWith("<?xml"))
			xml = prepareXML(xml);

		StringReader reader = new StringReader(xml);
		return (ObjectViews) unmarshaller.unmarshal(reader);
	}
	
	public static Map<String, Object> findViews(String model, Map<String, String> views) {
		final Map<String, Object> result = Maps.newHashMap();
		if (views == null || views.isEmpty()) {
			views = ImmutableMap.of("grid", "", "form", "");
		}
		for(String type : views.keySet()) {
			final String name = views.get(type);
			final AbstractView view = findView(model, name, type);
			try {
				result.put(type, view);
			} catch (Exception e) {
			}
		}
		return result;
	}

	public static AbstractView findView(String model, String name, String type) {
		MetaView view = null;
		User user = AuthUtils.getUser();
		Long group = user != null && user.getGroup() != null ? user.getGroup().getId() : null;

		if (name != null) {
			view = MetaView.findByName(name, model, group);
			if (view == null) {
				view = MetaView.findByName(name, model);
				if (view == null) {
					view = MetaView.findByName(name);
				}
			}
		}

		if (view == null) {
			view = MetaView.findByType(type, model, group);
			if (view == null) {
				view = MetaView.findByType(type, model);
			}
		}

		try {
			return ((ObjectViews) XMLViews.unmarshal(view.getXml())).getViews().get(0);
		} catch (Exception e) {
		}
		return null;
	}

	public static AbstractView findView(String name, String module) {
		MetaView view = MetaView.all()
				.filter("self.name = :name AND self.module = :module")
				.bind("name", name)
				.bind("module", module)
				.order("-priority")
				.cacheable().autoFlush(false)
				.fetchOne();
		try {
			return ((ObjectViews) XMLViews.unmarshal(view.getXml())).getViews().get(0);
		} catch (Exception e) {
		}
		return null;
	}

	public static Action findAction(String name) {
		MetaAction action = MetaAction.findByName(name);
		try {
			return ((ObjectViews) XMLViews.unmarshal(action.getXml())).getActions().get(0);
		} catch (Exception e) {
		}
		return null;
	}
}
