/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.actions;

import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.db.repo.MetaJsonRecordRepository;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.script.GroovyScriptHelper;
import com.axelor.script.JavaScriptScriptHelper;
import com.axelor.script.ScriptAllowed;
import com.axelor.script.ScriptHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.inject.persist.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;
import org.eclipse.persistence.oxm.annotations.XmlValueExtension;

public class ActionScript extends Action {

  private static final String LANGUAGE_JS = "js";

  private static final String KEY_REQUEST = "$request";
  private static final String KEY_RESPONSE = "$response";
  private static final String KEY_JSON = "$json";
  private static final String KEY_EM = "$em";

  @JsonIgnore
  @XmlElement(name = "script")
  private ActScript script;

  public ActScript getScript() {
    return script;
  }

  public void setScript(ActScript script) {
    this.script = script;
  }

  private ScriptHelper getScriptHelper(Bindings bindings) {
    return LANGUAGE_JS.equalsIgnoreCase(script.language)
        ? new JavaScriptScriptHelper(bindings)
        : new GroovyScriptHelper(bindings);
  }

  private Object run(ActionHandler handler) {
    final Bindings bindings = new SimpleBindings();
    final ActionRequest request = handler.getRequest();
    final ActionResponse response = new ActionResponse();
    bindings.put(KEY_REQUEST, request);
    bindings.put(KEY_RESPONSE, response);
    bindings.put(KEY_JSON, Beans.get(MetaJsonRecordRepository.class));
    if (Boolean.TRUE.equals(script.transactional)) {
      bindings.put(KEY_EM, wrap(JPA.em()));
    }
    ScriptHelper helper = getScriptHelper(bindings);
    try {
      helper.eval(script.code.trim(), bindings);
    } catch (ScriptException e) {
      if ("<eval>".equals(e.getFileName())) {
        e =
            new ScriptException(
                e.getMessage()
                    .replace(
                        "<eval>", "<strong>&lt;action-script name=" + getName() + "&gt;</strong>"));
      }
      response.setException(e);
    } catch (Exception e) {
      response.setException(e);
    } finally {
      // Close JavaScript context if applicable
      if (helper instanceof AutoCloseable closeable) {
        try {
          closeable.close();
        } catch (Exception ignored) {
          // ignore close exceptions
        }
      }
    }
    return response;
  }

  /**
   * Wraps the entity manager so the script policy allows it: the proxy implements {@link
   * ScriptEntityManager}, which carries {@link ScriptAllowed}.
   */
  private static EntityManager wrap(EntityManager em) {
    return (EntityManager)
        Proxy.newProxyInstance(
            ScriptEntityManager.class.getClassLoader(),
            new Class<?>[] {ScriptEntityManager.class},
            (proxy, method, args) -> {
              try {
                return method.invoke(em, args);
              } catch (InvocationTargetException e) {
                throw e.getCause();
              }
            });
  }

  @Override
  public Object evaluate(ActionHandler handler) {
    return Boolean.TRUE.equals(script.transactional)
        ? Beans.get(ActRunner.class).run(this, handler)
        : run(handler);
  }

  public static class ActRunner {

    @Transactional
    public Object run(ActionScript action, ActionHandler handler) {
      return action.run(handler);
    }
  }

  @XmlType
  public static class ActScript extends Action.Element {

    @XmlAttribute private String language;

    @XmlAttribute private Boolean transactional;

    @XmlCDATA @XmlValue @XmlValueExtension private String code;

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getLanguage() {
      return language;
    }

    public void setLanguage(String language) {
      this.language = language;
    }

    public Boolean getTransactional() {
      return transactional;
    }

    public void setTransactional(Boolean transactional) {
      this.transactional = transactional;
    }
  }
}
