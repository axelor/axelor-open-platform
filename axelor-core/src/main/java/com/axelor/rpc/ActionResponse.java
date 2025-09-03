/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc;

import com.axelor.common.StringUtils;
import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.file.temp.TempFiles;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.validate.validator.Alert;
import com.axelor.meta.schema.actions.validate.validator.Error;
import com.axelor.meta.schema.actions.validate.validator.Info;
import com.axelor.meta.schema.actions.validate.validator.Notify;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An implementation of {@link Response} to be used with controllers. */
@XmlType
@XmlRootElement(name = "response")
public class ActionResponse extends Response {

  private Map<String, Object> dataMap;

  private static final Logger log = LoggerFactory.getLogger(ActionResponse.class);

  private Map<String, Object> dataMap() {
    if (dataMap == null) {
      dataMap = new HashMap<>();
      List<Object> data = new ArrayList<>();
      data.add(dataMap);
      setData(data);
    }
    return dataMap;
  }

  @SuppressWarnings("all")
  private void set(String name, Object value) {
    dataMap().put(name, value);
  }

  /**
   * Set the <i>reload</i> flag.
   *
   * <p>The client user <i>reload</i> flag to refresh the view.
   *
   * @param reload whether to reload client view
   */
  public void setReload(boolean reload) {
    set("reload", reload);
  }

  /**
   * Set the <i>canClose</i> flag.
   *
   * <p>The client uses the <i>canClose</i> flag to decide whether the view can be closed.
   *
   * @param canClose whether the view can be closed.
   */
  public void setCanClose(boolean canClose) {
    set("canClose", canClose);
  }

  /**
   * Set an info message.
   *
   * <p>This message will be shown on the client screen as a dialog.
   *
   * @param message the message to show on client
   */
  public void setInfo(String message) {
    setInfo(message, null);
  }

  /**
   * Set an info message.
   *
   * <p>This message will be shown on the client screen as a dialog.
   *
   * @param message the message to show on client
   * @param title the title of the modal
   */
  public void setInfo(String message, String title) {
    setInfo(message, title, null);
  }

  /**
   * Set an info message.
   *
   * <p>This message will be shown on the client screen as a dialog.
   *
   * @param message the message to show on client
   * @param title the title of the modal
   * @param confirmBtnTitle the title of the confirm button
   */
  public void setInfo(String message, String title, String confirmBtnTitle) {
    setMessage(Info.KEY, message, title, confirmBtnTitle, null, null);
  }

  /**
   * Set a notification message.
   *
   * <p>The message will be show on the client screen as a notification.
   *
   * @param message the message to show on client
   */
  public void setNotify(String message) {
    setNotify(message, null);
  }

  /**
   * Set a notification message.
   *
   * <p>The message will be show on the client screen as a notification.
   *
   * @param message the message to show on client
   * @param title the title of the notification box
   */
  public void setNotify(String message, String title) {
    setMessage(Notify.KEY, message, title, null, null, null);
  }

  /**
   * Set an alert message.
   *
   * <p>The message will be shown on the client screen as an alert dialog.
   *
   * @param message the message to show as an alert
   */
  public void setAlert(String message) {
    setAlert(message, null);
  }

  /**
   * Set an alert message.
   *
   * <p>The message will be shown on the client screen as an alert dialog.
   *
   * @param message the message to show as an alert
   * @param title the title of the modal
   */
  public void setAlert(String message, String title) {
    setAlert(message, title, null, null, null);
  }

  /**
   * Set an alert message.
   *
   * <p>The message will be shown on the client screen as an alert dialog.
   *
   * @param message the message to show as an alert
   * @param title the title of the modal
   * @param confirmBtnTitle the title of the confirm button
   * @param cancelBtnTitle the title of the cancel button
   * @param action action to be executed on error or alert message to make corrective measures, when
   *     error dialog is closed or alert dialog is canceled.
   */
  public void setAlert(
      String message, String title, String confirmBtnTitle, String cancelBtnTitle, String action) {
    setMessage(Alert.KEY, message, title, confirmBtnTitle, cancelBtnTitle, action);
  }

  /**
   * Set an error message.
   *
   * <p>The message will be shown on the client screen as an error dialog.
   *
   * @param message the message to show as an error
   */
  public void setError(String message) {
    setError(message, null);
  }

  /**
   * Set an error message.
   *
   * <p>The message will be shown on the client screen as an error dialog.
   *
   * @param message the message to show as an error
   * @param title the title of the modal
   */
  public void setError(String message, String title) {
    setError(message, title, null, null);
  }

  /**
   * Set an error message.
   *
   * <p>The message will be shown on the client screen as an error dialog.
   *
   * @param message the message to show as an error
   * @param title the title of the modal
   * @param confirmBtnTitle the title of the confirm button
   * @param action action to be executed on error or alert message to make corrective measures, when
   *     error dialog is closed or alert dialog is canceled.
   */
  public void setError(String message, String title, String confirmBtnTitle, String action) {
    setMessage(Error.KEY, message, title, confirmBtnTitle, null, action);
  }

  /**
   * Set a message on the client screen
   *
   * @param type the type of the message
   * @param message the message to show
   * @param title the title of the modal/notification
   * @param confirmBtnTitle the title of the confirm button
   * @param cancelBtnTitle the title of the cancel button
   * @param action action to be executed on error or alert message to make corrective measures, when
   *     error dialog is closed or alert dialog is canceled.
   */
  private void setMessage(
      String type,
      String message,
      String title,
      String confirmBtnTitle,
      String cancelBtnTitle,
      String action) {
    final Map<String, Object> map = new HashMap<>();
    map.put("message", message);
    if (StringUtils.notBlank(title)) {
      map.put("title", title);
    }
    if (StringUtils.notBlank(confirmBtnTitle)) {
      map.put("confirmBtnTitle", confirmBtnTitle);
    }
    if (StringUtils.notBlank(cancelBtnTitle)) {
      map.put("cancelBtnTitle", cancelBtnTitle);
    }
    if (StringUtils.notBlank(action)) {
      map.put("action", action);
    }
    set(type, map);
  }

  /**
   * Set the comma separated list of pending actions.
   *
   * <p>This can be used along with {@link #setAlert(String)}, {@link #setError(String)} methods.
   *
   * @param actions the list of pending actions
   */
  public void setPending(String actions) {
    set("pending", actions);
  }

  /**
   * Set the file path to be exported.
   *
   * <p>The file path is copied to a dedicated temporary file for pending export.
   *
   * <p>The client will initiate downloading the export file.
   *
   * @param path the path to the export file
   */
  public void setExportFile(String path) {
    setExportFile(Path.of(path));
  }

  /**
   * Set the file path to be exported.
   *
   * <p>The file path is copied to a dedicated temporary file for pending export.
   *
   * <p>The client will initiate downloading the export file.
   *
   * @param path the path to the export file
   * @param fileName the name of the downloaded export file
   */
  public void setExportFile(String path, String fileName) {
    setExportFile(Path.of(path), fileName);
  }

  /**
   * Set the file path to be exported.
   *
   * <p>The file path is copied to a dedicated temporary file for pending export.
   *
   * <p>The client will initiate downloading the export file.
   *
   * @param path the path to the export file
   */
  public void setExportFile(Path path) {
    setExportFile(path, null);
  }

  /**
   * Set the file path to be exported.
   *
   * <p>The file path is copied to a dedicated temporary file for pending export.
   *
   * <p>The client will initiate downloading the export file.
   *
   * @param path the path to the export file
   * @param fileName the name of the downloaded export file
   */
  public void setExportFile(Path path, String fileName) {
    var name = StringUtils.notBlank(fileName) ? fileName : path.getFileName().toString();
    var fullPath = path.isAbsolute() ? path : TempFiles.getTempPath().resolve(path);

    try (var stream = Files.newInputStream(fullPath)) {
      setExportFile(stream, name);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Set the file stream to be exported.
   *
   * <p>This creates a pending export file from the given stream.
   *
   * <p>The client will initiate downloading the export file.
   *
   * @param stream the stream to the export file
   * @param fileName the name of the downloaded export file
   */
  public void setExportFile(InputStream stream, String fileName) {
    var token = Beans.get(PendingExportService.class).add(stream);
    set("exportFile", fileName);
    set("exportToken", token);
  }

  /**
   * Set record values.
   *
   * <p>The client will update current view with these values.
   *
   * <p>The context can be a {@link Map}, {@link Context} or {@link Model} proxy obtained with
   * {@link Context#asType(Class)}. Managed instance of {@link Model} should be avoided.
   *
   * @param context the context to set, a map or context proxy
   * @see #setValue(String, Object)
   * @see Context#asType(Class)
   * @throws IllegalArgumentException if passed context is detached non-proxy entity.
   */
  public void setValues(Object context) {
    boolean managed = false;
    if (context instanceof ContextEntity
        || context instanceof Map
        || (managed = context instanceof Model && JPA.em().contains(context))) {
      if (managed) {
        log.warn(
            "managed instance passed as context: {}#{}",
            EntityHelper.getEntityClass(context),
            ((Model) context).getId());
      }
      set("values", context);
    } else {
      throw new IllegalArgumentException("Invalid context object.");
    }
  }

  /**
   * Set value for the given field.
   *
   * @param fieldName name of the field
   * @param value field name
   * @see #setValues(Object)
   */
  @SuppressWarnings("all")
  public void setValue(String fieldName, Object value) {
    Map<String, Object> values = (Map) dataMap().get("values");
    if (values == null) {
      values = new HashMap<>();
      setValues(values);
    }
    final Object permittedValue = toPermitted(value);
    values.put(fieldName, permittedValue);
  }

  /**
   * Turns given bean into map of id, $version, and namecolumn if user has no read permission.
   *
   * @param bean object
   * @return original bean or compact map of id, $version, and namecolumn if not permitted
   */
  @Nullable
  private Object toPermitted(Object bean) {
    if (!(bean instanceof Model)) {
      return bean;
    }

    final Model model = (Model) bean;
    final Class<? extends Model> beanClass =
        EntityHelper.getEntityClass(model).asSubclass(Model.class);

    if (Beans.get(JpaSecurity.class).isPermitted(JpaSecurity.CAN_READ, beanClass, model.getId())) {
      return model;
    }

    final Mapper mapper = Mapper.of(beanClass);
    final Map<String, Object> map = new HashMap<>();
    map.put("id", mapper.getProperty("id").get(model));
    map.put("$version", mapper.getProperty("version").get(model));
    Optional.ofNullable(mapper.getNameField())
        .ifPresent(property -> map.put(property.getName(), property.get(model)));

    return map;
  }

  /**
   * Inform the client to open the given view.
   *
   * @param view the view to show
   */
  public void setView(Map<String, Object> view) {
    set("view", view);
  }

  /**
   * Inform the client to open a view for the given model.
   *
   * @param title the view title
   * @param model the model name
   * @param mode the view mode (e.g. form, grid etc)
   * @param domain the filter
   */
  public void setView(String title, String model, String mode, String domain) {
    final Map<String, Object> view = new HashMap<>();
    view.put("title", title);
    view.put("model", model);
    view.put("type", mode);
    view.put("domain", domain);
    setView(view);
  }

  /**
   * Send an arbitrary signal to the client view with the specified data.
   *
   * @param signal signal name
   * @param data signal data
   */
  public void setSignal(String signal, Object data) {
    set("signal", signal);
    set("signal-data", data);
  }

  /**
   * Set field attributes.
   *
   * <p>The client view may update the view fields with the given attributes.
   *
   * @param attrs attribute map for the fields
   */
  public void setAttrs(Map<String, Map<String, Object>> attrs) {
    set("attrs", attrs);
  }

  /**
   * Set an attribute of a field.
   *
   * @param fieldName name of the field
   * @param attr attribute name
   * @param value attribute value
   */
  @SuppressWarnings("all")
  public void setAttr(String fieldName, String attr, Object value) {

    Map<String, Map<String, Object>> attrs = null;
    try {
      attrs = (Map) ((Map) getItem(0)).get("attrs");
    } catch (Exception e) {
    }

    if (attrs == null) {
      attrs = new HashMap<>();
    }

    Map<String, Object> my = attrs.get(fieldName);
    if (my == null) {
      my = new HashMap<>();
    }

    my.put(attr, value);
    attrs.put(fieldName, my);

    setAttrs(attrs);
  }

  /**
   * Set the <code>required</code> attribute for the given field.
   *
   * @param fieldName name of the field
   * @param required true or false
   */
  public void setRequired(String fieldName, boolean required) {
    setAttr(fieldName, "required", required);
  }

  /**
   * Set the <code>readonly</code> attribute for the given field.
   *
   * @param fieldName name of the field
   * @param readonly true or false
   */
  public void setReadonly(String fieldName, boolean readonly) {
    setAttr(fieldName, "readonly", readonly);
  }

  /**
   * Set the <code>hidden</code> attribute for the given field.
   *
   * @param fieldName name of the field
   * @param hidden true or false
   */
  public void setHidden(String fieldName, boolean hidden) {
    setAttr(fieldName, "hidden", hidden);
  }

  /**
   * Set the <code>color</code> attribute for the given field.
   *
   * @param fieldName name of the field
   * @param color CSS compatible color value
   */
  public void setColor(String fieldName, String color) {
    setAttr(fieldName, "color", color);
  }
}
