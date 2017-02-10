/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.rpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * An implementation of {@link Response} to be used with controllers.
 * 
 */
@XmlType
@XmlRootElement(name = "response")
public class ActionResponse extends Response {

	final Map<String, Object> _data = Maps.newHashMap();

	@SuppressWarnings("all")
	private void set(String name, Object value) {
		if (getData() == null) {
			List<Object> data = Lists.newArrayList();
			data.add(_data);
			setData(data);
		}
		_data.put(name, value);
	}

	/**
	 * Set the <i>reload</i> flag.
	 * <p>
	 * The client user <i>reload</i> flag to refresh the view.
	 * </p>
	 * 
	 * @param reload
	 *            whether to reload client view
	 */
	public void setReload(boolean reload) {
		set("reload", reload);
	}

	/**
	 * Set the <i>canClose</i> flag.
	 * <p>
	 * The client uses the <i>canClose</i> flag to decide whether the view can
	 * be closed.
	 * 
	 * @param canClose
	 *            whether the view can be closed.
	 */
	public void setCanClose(boolean canClose) {
		set("canClose", canClose);
	}

	/**
	 * Set a flash message.
	 * <p>
	 * This message will be shown on the client screen as a dialog.
	 * </p>
	 * 
	 * @param flash
	 *            the message to show on client
	 */
	public void setFlash(String flash) {
		set("flash", flash);
	}

	/**
	 * Set a notification message.
	 * <p>
	 * The message will be show on the client screen as a notification.
	 * </p>
	 * 
	 * @param message
	 *            the message to show on client
	 */
	public void setNotify(String message) {
		set("notify", message);
	}

	/**
	 * Set an alert message.
	 * <p>
	 * The message will be shown on the client screen as an alert dialog.
	 * </p>
	 * 
	 * @param message
	 *            the message to show as an alert
	 */
	public void setAlert(String message) {
		set("alert", message);
	}

	/**
	 * Set an error message.
	 * <p>
	 * The message will be shown on the client screen as an error dialog.
	 * </p>
	 * 
	 * @param message
	 *            the message to show as an error
	 */
	public void setError(String message) {
		set("error", message);
	}

	/**
	 * Set the comma separated list of pending actions.
	 * <p>
	 * This can be used along with {@link #setAlert(String)},
	 * {@link #setError(String)} methods.
	 * </p>
	 * 
	 * @param actions
	 *            the list of pending actions
	 */
	public void setPending(String actions) {
		set("pending", actions);
	}

	/**
	 * Set the file path relative to the data export directory.
	 * <p>
	 * The client will initiate downloading the exported file.
	 * </p>
	 * 
	 * @param path
	 *            the relative path to the exported file
	 */
	public void setExportFile(String path) {
		set("exportFile", path);
	}
	
	/**
	 * Set record values.
	 * <p>
	 * The client updates current view with these values.
	 * </p>
	 * 
	 * @param context
	 *            a map or a model instance
	 * @see #setValue(String, Object)
	 */
	public void setValues(Object context) {
		set("values", context);
	}

	/**
	 * Set value for the given field.
	 * 
	 * @param fieldName
	 *            name of the field
	 * @param value
	 *            field name
	 * @see #setValues(Object)
	 */
	@SuppressWarnings("all")
	public void setValue(String fieldName, Object value) {
		Map<String, Object> values = (Map) _data.get("values");
		if (values == null) {
			values = Maps.newHashMap();
		}
		values.put(fieldName, value);
		setValues(values);
	}

	/**
	 * Inform the client to open the given view.
	 * 
	 * @param view
	 *            the view to show
	 */
	public void setView(Map<String, Object> view) {
		set("view", view);
	}

	/**
	 * Inform the client to open a view for the given model.
	 * 
	 * @param title
	 *            the view title
	 * @param model
	 *            the model name
	 * @param mode
	 *            the view mode (e.g. form, grid etc)
	 * @param domain
	 *            the filter
	 */
	public void setView(String title, String model, String mode, String domain) {
		Map<String, Object> view = Maps.newHashMap();
		view.put("title", title);
		view.put("model", model);
		view.put("type", mode);
		view.put("domain", domain);
		setView(view);
	}

	/**
	 * Send an arbitrary signal to the client view with the specified data.
	 * 
	 * @param signal
	 *            signal name
	 * @param data
	 *            signal data
	 */
	public void setSignal(String signal, Object data) {
		set("signal", signal);
		set("signal-data", data);
	}

	/**
	 * Set field attributes.
	 * <p>
	 * The client view may update the view fields with the given attributes.
	 * </p>
	 * 
	 * @param attrs
	 *            attribute map for the fields
	 */
	public void setAttrs(Map<String, Map<String, Object>> attrs) {
		set("attrs", attrs);
	}

	/**
	 * Set an attribute of a field.
	 * 
	 * @param fieldName
	 *            name of the field
	 * @param attr
	 *            attribute name
	 * @param value
	 *            attribute value
	 */
	@SuppressWarnings("all")
	public void setAttr(String fieldName, String attr, Object value) {

		Map<String, Map<String, Object>> attrs = null;

		try {
			attrs = (Map) ((Map) getItem(0)).get("attrs");
		} catch (Exception e) {
		}

		if (attrs == null) {
			attrs = new HashMap<String, Map<String, Object>>();
		}

		Map<String, Object> my = attrs.get(fieldName);
		if (my == null) {
			my = new HashMap<String, Object>();
		}

		my.put(attr, value);
		attrs.put(fieldName, my);

		setAttrs(attrs);
	}

	/**
	 * Set the <code>required</code> attribute for the given field.
	 * 
	 * @param fieldName
	 *            name of the field
	 * @param required
	 *            true or false
	 */
	public void setRequired(String fieldName, boolean required) {
		setAttr(fieldName, "required", required);
	}

	/**
	 * Set the <code>readonly</code> attribute for the given field.
	 * 
	 * @param fieldName
	 *            name of the field
	 * @param readonly
	 *            true or false
	 */
	public void setReadonly(String fieldName, boolean readonly) {
		setAttr(fieldName, "readonly", readonly);
	}

	/**
	 * Set the <code>hidden</code> attribute for the given field.
	 * 
	 * @param fieldName
	 *            name of the field
	 * @param hidden
	 *            true or false
	 */
	public void setHidden(String fieldName, boolean hidden) {
		setAttr(fieldName, "hidden", hidden);
	}

	/**
	 * Set the <code>color</code> attribute for the given field.
	 * 
	 * @param fieldName
	 *            name of the field
	 * @param color
	 *            CSS compatible color value
	 */
	public void setColor(String fieldName, String color) {
		setAttr(fieldName, "color", color);
	}
}
