package com.axelor.meta.views;

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
	
	private String getPending(Iterator<ActionItem> actions) {
		List<String> pending = Lists.newArrayList();
    	while(actions.hasNext()) {
    		pending.add(actions.next().getName());
    	}
    	return Joiner.on(",").join(pending);
	}
	
	private Action findAction(String name) {
		
		if (name == null || "".equals(name.trim()))
			return null;

		name = name.trim();
		if (name.contains(":")) {
			String[] parts = name.split("\\:");
			Action.Call method = new Action.Call();
			ActionMethod action = new ActionMethod();
			
			method.setController(parts[0]);
			method.setMethod(parts[1]);
			action.setElements(ImmutableList.of(method));

			return action;
		}

		return MetaStore.getAction(name);
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
			Act act = iter.next();
			String name = act.getName().trim();
			
			if ("save".equals(name)) {
				if (act.test(handler)) {
					String pending = this.getPending(iter);
	            	log.debug("wait for 'save', pending actions: {}", pending);
					result.add(ImmutableMap.of("save", true, "pending", pending));
				}
				log.debug("action '{}' doesn't meet the condition: {}", "save", act.getCondition());
				break;
			}
			
			log.debug("action: {}", name);

			Action action = this.findAction(name);
			if (action == null) {
				log.error("action doesn't exist: {}", name);
                continue;
			}

			if (!act.test(handler)) {
				log.debug("action '{}' doesn't meet the condition: {}", act.getName(), act.getCondition());
				continue;
			}
			
			Object value = action.wrap(handler);
            if (value instanceof Response) {
            	Response res = (Response) value;
            	if (res.getStatus() != Response.STATUS_SUCCESS) {
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

            if (action instanceof ActionValidate && iter.hasNext()) {
            	String pending = this.getPending(iter);
            	log.debug("wait for validation: {}, {}", name, value);
            	log.debug("pending actions: {}", pending);
				((Map<String, Object>) value).put("pending", pending);
                break;
            }
		}
		return result;
	}

	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}

	public static class ActionItem extends Act {
		
	}

}
