/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.json;

import com.axelor.auth.db.Role;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record JsonReferenceFieldDTO(
    // Core & Identity
    String name,
    String title,
    String type,

    // Model Context
    String model,
    String modelField,

    // Database & Sequence
    Integer sequence,
    Integer columnSequence,

    // Basic Boolean flags
    Boolean hidden,
    Boolean required,
    Boolean readonly,
    Boolean nameField,
    Boolean visibleInGrid,

    // Conditional visibility & constraints
    String showIf,
    String hideIf,
    String requiredIf,
    String readonlyIf,

    // Limits & Precision
    Integer minSize,
    Integer maxSize,
    Integer precision,
    Integer scale,
    String pattern,

    // Target Relations / Type Spec
    String targetModel,
    String targetJsonModel,
    String selection,
    String enumType,
    String defaultValue,
    String domain,

    // UI & Views
    String widget,
    String widgetAttrs,
    String help,
    String formView,
    String gridView,

    // Interactive Expr / Actions
    String onChange,
    String onClick,
    String valueExpr,

    // Context field mapping
    String contextField,
    String contextFieldTarget,
    String contextFieldTargetName,
    String contextFieldValue,
    String contextFieldTitle,

    // Security / Visibility
    String includeIf,
    Set<Long> roleIds,

    // Audit / Change Tracking
    Boolean tracked,
    String trackEvent,
    String trackCondition)
    implements Serializable {

  public static JsonReferenceFieldDTO from(MetaJsonField field) {
    return new JsonReferenceFieldDTO(
        // Core & Identity
        field.getName(),
        field.getTitle(),
        field.getType(),

        // Model Context
        field.getModel(),
        field.getModelField(),

        // Database & Sequence
        field.getSequence(),
        field.getColumnSequence(),

        // Basic Boolean flags
        field.getHidden(),
        field.getRequired(),
        field.getReadonly(),
        field.getNameField(),
        field.getVisibleInGrid(),

        // Conditional visibility & constraints
        field.getShowIf(),
        field.getHideIf(),
        field.getRequiredIf(),
        field.getReadonlyIf(),

        // Limits & Precision
        field.getMinSize(),
        field.getMaxSize(),
        field.getPrecision(),
        field.getScale(),
        field.getRegex(),

        // Target Relations / Type Spec
        field.getTargetModel(),
        Optional.ofNullable(field.getTargetJsonModel()).map(MetaJsonModel::getName).orElse(null),
        field.getSelection(),
        field.getEnumType(),
        field.getDefaultValue(),
        field.getDomain(),

        // UI & Views
        field.getWidget(),
        field.getWidgetAttrs(),
        field.getHelp(),
        field.getFormView(),
        field.getGridView(),

        // Interactive Expr / Actions
        field.getOnChange(),
        field.getOnClick(),
        field.getValueExpr(),

        // Context field mapping
        field.getContextField(),
        field.getContextFieldTarget(),
        field.getContextFieldTargetName(),
        field.getContextFieldValue(),
        field.getContextFieldTitle(),

        // Security / Visibility
        field.getIncludeIf(),
        field.getRoles() == null
            ? Collections.emptySet()
            : field.getRoles().stream().map(Role::getId).collect(Collectors.toUnmodifiableSet()),

        // Audit / Change Tracking
        field.getTracked(),
        field.getTrackEvent(),
        field.getTrackCondition());
  }

  public boolean isJsonModelTarget() {
    return targetModel == null || MetaJsonRecord.class.getName().equals(targetModel);
  }

  public String resolveTargetModel() {
    return targetModel != null ? targetModel : MetaJsonRecord.class.getName();
  }

  public Map<String, Object> toMap() {
    Map<String, Object> map = new HashMap<>();

    // Core & Identity
    put(map, "name", name);
    put(map, "title", title);
    put(map, "type", type);

    // Model Context
    put(map, "model", model);
    put(map, "modelField", modelField);

    // Database & Sequence
    put(map, "sequence", sequence);
    put(map, "columnSequence", columnSequence);

    // Basic Boolean flags
    put(map, "hidden", hidden);
    put(map, "required", required);
    put(map, "readonly", readonly);
    put(map, "nameField", nameField);
    put(map, "visibleInGrid", visibleInGrid);

    // Conditional visibility & constraints
    put(map, "showIf", showIf);
    put(map, "hideIf", hideIf);
    put(map, "requiredIf", requiredIf);
    put(map, "readonlyIf", readonlyIf);

    // Limits & Precision
    put(map, "minSize", minSize);
    put(map, "maxSize", maxSize);
    put(map, "precision", precision);
    put(map, "scale", scale);
    put(map, "pattern", pattern);

    // Target Relations / Type Spec
    put(map, "targetModel", targetModel);
    put(map, "targetJsonModel", targetJsonModel);
    put(map, "selection", selection);
    put(map, "enumType", enumType);
    put(map, "defaultValue", defaultValue);
    put(map, "domain", domain);

    // UI & Views
    put(map, "widget", widget);
    put(map, "widgetAttrs", widgetAttrs);
    put(map, "help", help);
    put(map, "formView", formView);
    put(map, "gridView", gridView);

    // Interactive Expr / Actions
    put(map, "onChange", onChange);
    put(map, "onClick", onClick);
    put(map, "valueExpr", valueExpr);

    // Context field mapping
    put(map, "contextField", contextField);
    put(map, "contextFieldTarget", contextFieldTarget);
    put(map, "contextFieldTargetName", contextFieldTargetName);
    put(map, "contextFieldValue", contextFieldValue);
    put(map, "contextFieldTitle", contextFieldTitle);

    // Security / Visibility
    put(map, "includeIf", includeIf);
    put(map, "roleIds", roleIds);

    // Audit / Change Tracking
    put(map, "tracked", tracked);
    put(map, "trackEvent", trackEvent);
    put(map, "trackCondition", trackCondition);

    return map;
  }

  /** Puts value to map, skipping null and false. */
  private void put(Map<String, Object> map, String key, Object value) {
    if (value != null && !Boolean.FALSE.equals(value)) {
      map.put(key, value);
    }
  }
}
