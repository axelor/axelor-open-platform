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
package com.axelor.meta.schema.actions;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaStore;
import com.axelor.rpc.Response;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@XmlType
public class ActionGroup extends Action {

	@XmlElement(name = "action")
	private List<ActionItem> actions;

	public List<ActionItem> getActions() {
		return actions;
	}

	public void setActions(List<ActionItem> actions) {
		this.actions = actions;
	}

	public void addAction(String name) {
		if (this.actions == null) {
			this.actions = Lists.newArrayList();
		}
		ActionItem item = new ActionItem();
		item.setName(name);
		this.actions.add(item);
	}

	private String getPending(Iterator<ActionItem> actions, String... prepend) {
		final List<String> pending = Lists.newArrayList(prepend);
		while(actions.hasNext()) {
    		pending.add(actions.next().getName());
    	}
    	final String result = Joiner.on(",").skipNulls().join(pending);
    	return Strings.isNullOrEmpty(result) ? null : result;
	}

	private Action findAction(String name) {

		if (name == null || "".equals(name.trim())) {
			return null;
		}

		String actionName = name.trim();

		if (actionName.contains(":")) {

			Action action;
			String[] parts = name.split("\\:", 2);

			if (parts[0].matches("grid|form|tree|portal|calendar|chart|search|html")) {
				ActionView.View view = new ActionView.View();
				view.setType(parts[0]);
				view.setName(parts[1]);
				action = new ActionView();
				((ActionView) action).setViews(ImmutableList.of(view));
			} else {
				ActionMethod.Call method = new ActionMethod.Call();
				method.setController(parts[0]);
				method.setMethod(parts[1]);
				action = new ActionMethod();
				((ActionMethod) action).setCall(method);
			}
			return action;
		} else if (actionName.indexOf("[") > -1 && actionName.endsWith("]")) {
				String idx = actionName.substring(actionName.lastIndexOf('[') + 1, actionName.lastIndexOf(']'));
				actionName = actionName.substring(0, actionName.lastIndexOf('['));
				int index = Integer.parseInt(idx);
				log.debug("continue action-validate: {}", actionName);
				log.debug("continue at: {}", index);
				Action action = MetaStore.getAction(actionName);
				if (action instanceof ActionValidate) {
					((ActionValidate) action).setIndex(index);
				}
				return action;
		}

		return MetaStore.getAction(actionName);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object evaluate(ActionHandler handler) {

		List<Object> result = Lists.newArrayList();
		Iterator<ActionItem> iter = actions.iterator();

		if (getName() != null) {
			log.debug("action-group: {}", getName());
		}

		while(iter.hasNext()) {
			Element element = iter.next();
			String name = element.getName().trim();

			if ("save".equals(name)) {
				if (element.test(handler)) {
					String pending = this.getPending(iter);
	            	log.debug("wait for 'save', pending actions: {}", pending);
					result.add(ImmutableMap.of("save", true, "pending", pending));
				}
				log.debug("action '{}' doesn't meet the condition: {}", "save", element.getCondition());
				break;
			}

			log.debug("action: {}", name);

			Action action = this.findAction(name);
			if (action == null) {
				log.error("action doesn't exist: {}", name);
                continue;
			}

			if (!element.test(handler)) {
				log.debug("action '{}' doesn't meet the condition: {}", element.getName(), element.getCondition());
				continue;
			}

			Object value = action.wrap(handler);
            if (value instanceof Response) {
            	Response res = (Response) value;
            	// if error or this is the only action then return the response
            	if (res.getStatus() != Response.STATUS_SUCCESS || actions.size() == 1) {
            		return res;
            	}
            	value = res.getItem(0);
            }
            if (value == null) {
            	continue;
            }

            // update the context if required
            if (value instanceof Map && ((Map) value).get("values") != null) {
            	Object values = ((Map) value).get("values");
            	if (values instanceof Model) {
            		values = Mapper.toMap(values);
            	}
            	if (values instanceof Map) {
            		handler.getContext().update((Map) values);
            	}
            }

            // stop for reload
            if (value instanceof Map && Objects.equal(Boolean.TRUE, ((Map) value).get("reload"))) {
            	String pending = this.getPending(iter);
            	log.debug("wait for 'reload', pending actions: {}", pending);
				((Map<String, Object>) value).put("pending", pending);
				result.add(value);
            } else if (action instanceof ActionGroup && value instanceof Collection) {
            	result.addAll((Collection<?>) value);
            } else {
            	result.add(value);
            }

            log.debug("action complete: {}", name);

            if (action instanceof ActionValidate && value instanceof Map) {
            	String validate = (String) ((Map) value).get("pending");
            	String pending = this.getPending(iter, validate);
            	log.debug("wait for validation: {}, {}", name, value);
            	log.debug("pending actions: {}", pending);
            	((Map<String, Object>) value).put("pending", pending);
                break;
            }

            if (action instanceof ActionCondition) {
            	if (value instanceof Map || Objects.equal(value, Boolean.FALSE)) {
                	break;
            	}
            }

            if (action instanceof ActionGroup && !result.isEmpty() && iter.hasNext()) {
            	Map<String, Object> last = null;
            	try {
            		last = (Map) result.get(result.size() - 1);
            	} catch (ClassCastException e) {
            	}
            	if (last != null && (last.containsKey("alert") || last.containsKey("error"))) {
            		String previous = (String) last.get("pending");
            		String pending = this.getPending(iter, previous);
            		last.put("pending", pending);
            		log.debug("wait for group validation: {}", action.getName());
            		log.debug("pending actions: {}", pending);
            		break;
            	}
            }
		}
		return result;
	}

	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}

	public static class ActionItem extends Element {

	}

}
