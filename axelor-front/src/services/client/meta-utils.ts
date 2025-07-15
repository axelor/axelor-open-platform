import camelCase from "lodash/camelCase";
import forEach from "lodash/forEach";
import isArray from "lodash/isArray";
import isString from "lodash/isString";
import startsWith from "lodash/startsWith";
import uniqueId from "lodash/uniqueId";

import { toKebabCase } from "@/utils/names";
import { findViewItem } from "@/utils/schema";
import { i18n } from "./i18n";
import { ViewData, viewFields as fetchViewFields } from "./meta";
import { ActionView, Field, JsonField, Property, Schema } from "./meta.types";

function processJsonForm(view: Schema) {
  if (view.type !== "form") return view;
  if (view.model !== "com.axelor.meta.db.MetaJsonRecord") return view;

  const panel = view.items?.[0];
  const jsonField = panel?.items?.[0];
  const jsonFields = jsonField?.jsonFields ?? [];

  const first = jsonFields?.[0];
  if (first?.type === "panel" && panel) {
    panel.type = "panel-json";
    if (first.widgetAttrs) {
      const attrs = JSON.parse(first.widgetAttrs);
      view.width = view.width || attrs.width;
    }
  }

  return view;
}

const TOP_LEVEL_ITEM = Symbol("TOP_LEVEL_ITEM");

type ItemSchema = Schema & {
  [key in typeof TOP_LEVEL_ITEM]?: boolean;
};

export function isTopLevelItem(schema: ItemSchema) {
  return schema[TOP_LEVEL_ITEM];
}

export function toTopLevelItem(schema: ItemSchema) {
  schema.showFrame = schema.showFrame ?? true;
  schema[TOP_LEVEL_ITEM] = true;
  return schema;
}

export function processFields(fields: Property[] | Record<string, Property>) {
  let result: Record<string, Property> = {};
  if (isArray(fields)) {
    forEach(fields, (field) => {
      field.type = field.type || "STRING";
      field.title = field.title || field.autoTitle;
      if (field.name) {
        result[field.name] = field;
        // if nested field then make it readonly
        if (!field.jsonField && field.name.indexOf(".") > -1) {
          field.readonly = true;
          field.required = false;
        }
      }
      processSelection(field);
    });
  } else {
    result = fields || {};
  }
  return result;
}

export function processSelection(field: Schema, editable?: boolean) {
  if (editable) {
    if (field.widget === "radio-select") field.widget = "selection";
    else if (field.widget === "checkbox-select") field.widget = "multi-select";
  }

  if ((field.selection || field.selectionList) && !field.widget) {
    field.widget = "selection";
  }
  forEach(field.selectionList, (item) => {
    if (isString(item.data)) {
      item.data = JSON.parse(item.data);
    }
  });
}

function processWidgetAttrs(field: Schema) {
  const attrs: Record<string, any> = {};

  forEach(field.widgetAttrs || {}, (value, name) => {
    let val = value;
    if (value === "null") val = null;
    if (name === "widget" && value) val = toKebabCase(value);
    else if (
      [
        "exclusive",
        "showBars",
        "canCopy",
        "lite",
        "labels",
        "big",
        "seconds",
        "canSuggest",
        "canReload",
        "callOnSave",
        "showTitle",
        "editable",
        "canMove",
        "canExport",
        "showFrame",
        "sidebar",
        "stacked",
        "attached",
        "required",
        "hidden",
        "readonly",
        "canCollapse",
        "tab",
        "toggle",
        "multiline",
        "translatable",
        "hideModeSwitch",
        "selectionShowCheckbox",
        "ratingFill",
        "ratingHighlightSelected",
        "sliderShowMinMax",
        "stepperCompleted",
        "stepperShowDescription",
        "colorPickerShowAlpha",
        "barcodeDisplayValue",
        "resetState",
      ].indexOf(name) !== -1
    ) {
      val = String(value)?.toLowerCase?.() === "true";
    } else if (
      [
        "rowSpan",
        "cols",
        "limit",
        "precision",
        "scale",
        "searchLimit",
        "colOffset",
        "colSpan",
        "itemSpan",
        "minSize",
        "maxSize",
        "step",
        "barcodeWidth",
      ].indexOf(name) !== -1 &&
      /^(-)?\d+(\.\d+)?$/.test(value)
    ) {
      val = +value;
    }
    attrs[camelCase(name)] = val;
  });

  return attrs;
}

export function processWidget(field: Schema) {
  if (!field.uid) {
    field.uid = uniqueId("w");
  }
  if (field.widget) {
    field.widget = toKebabCase(field.widget);
  }
  field.widgetAttrs = processWidgetAttrs(field);
}

function UseIncluded(view: Schema) {
  function UseMenubar(menubar: Schema[]) {
    if (!menubar) return;
    let my = view.menubar || menubar;
    if (my !== menubar && menubar) {
      my = my.concat(menubar);
    }
    view.menubar = my;
  }
  function UseToolbar(toolbar: Schema[]) {
    if (!toolbar) return;
    let my = view.toolbar || toolbar;
    if (my !== toolbar) {
      my = my.concat(toolbar);
    }
    view.toolbar = my;
  }
  function UseItems(view: Schema) {
    return UseIncluded(view);
  }

  let items: Schema[] = [];

  forEach(view.items, (item) => {
    if (item.type === "include") {
      if (item.view) {
        items = items.concat(UseItems(item.view));
        UseMenubar(item.view.menubar);
        UseToolbar(item.view.toolbar);
      }
    } else {
      items.push(item);
    }
  });

  return items;
}

export async function findViewFields(
  viewFields: Record<string, Property>,
  view: Schema,
  res?: { fields: string[]; related: Record<string, string[]> },
) {
  const result = res || {
    fields: [],
    related: {},
  };
  const items = result.fields;
  let fields = view.items;

  if (!fields) return result;
  if (view.items && !view._included) {
    view._included = true;
    fields = view.items = UseIncluded(view);
  }

  function pushIn(value: string, target: string[]) {
    if (!target.includes(value)) {
      target.push(value);
    }
  }

  function acceptEditor(item: Schema) {
    let collect = items;
    const editor = item.editor;
    if (item.name && item.target) {
      collect = result.related[item.name] || (result.related[item.name] = []);
    }
    if (editor.fields) {
      editor.fields = processFields(editor.fields);
    }
    const acceptItems = (items: Schema[] = []) => {
      forEach(items, (child) => {
        if (child.name && child.type === "field") {
          pushIn(child.name, collect);
          const targetName = getNonDefaultTargetName(child, editor.fields);
          if (targetName) {
            pushIn(`${child.name}.${targetName}`, collect);
          }
        } else if (child.type === "panel") {
          acceptItems(child.items);
        }
        if (child.widget === "ref-select") {
          pushIn(child.related, collect);
        }
        if (typeof child.depends === "string") {
          child.depends.split(/\s*,\s*/).forEach((name) => {
            pushIn(name, collect);
          });
        }
        processWidget(child);
      });
    };
    acceptItems(editor.items);
  }

  function acceptViewer(item: Schema) {
    let collect = items;
    const viewer = item.viewer;
    if (item.name && item.target) {
      collect = result.related[item.name] || (result.related[item.name] = []);
    }
    forEach(viewer.fields, (item) => {
      pushIn(item.name, collect);
    });
    if (viewer.fields) {
      viewer.fields = processFields(viewer.fields);
    }
  }

  function getNonDefaultTargetName(
    item: Schema,
    fields?: Record<string, Property>,
  ) {
    const { name, targetName } = item;
    if (name && targetName && fields) {
      const { targetName: fieldTargetName } = fields[name] ?? {};
      if (fieldTargetName && fieldTargetName !== targetName) {
        return targetName as string;
      }
    }
  }

  for (let i = 0; i < fields.length; i++) {
    const item = fields[i];
    if (item.editor) acceptEditor(item);
    if (item.viewer) acceptViewer(item);
    if (item.name && item.type === "panel-related") {
      pushIn(item.name, items);
      // fetch view item fields definitions if fields doesn't exist
      if (Object.keys(item.fields ?? {}).length === 0 && item.items?.length) {
        try {
          const { fields } = await fetchViewFields(
            viewFields?.[item.name]?.target || item.target,
            item.items
              .filter((item) => item.type === "field" && item.name)
              .map((item) => item.name) as string[],
          );
          item.fields = fields;
        } catch (err) {
          //ignore
        }
      }
    } else if (item.items) {
      await findViewFields(viewFields, item, result);
    } else if (item.name && item.type === "field") {
      pushIn(item.name, items);
      const targetName = getNonDefaultTargetName(item, viewFields);
      if (targetName) {
        const collect =
          result.related[item.name] || (result.related[item.name] = []);
        pushIn(targetName, collect);
      }
    }
  }

  if (view.type === "calendar") {
    items.push(view.eventStart);
    items.push(view.eventStop);
    items.push(view.colorBy);
  }
  if (view.type === "kanban") {
    items.push(view.columnBy);
    items.push(view.sequenceBy);
  }

  if (view.type === "gantt") {
    items.push(view.taskUser);
  }

  return result;
}

export function accept(params: ActionView) {
  return params.views?.reduce((prev, view) => {
    const type = view.type;
    params.viewType = params.viewType || type;
    return { ...prev, [type]: view };
  }, {});
}

export function processView(
  meta: ViewData<any>,
  view: Schema,
  parent?: Schema,
) {
  meta = meta || {};
  view = view || {};

  if (
    meta.jsonFields &&
    meta.jsonFields["attrs"] &&
    Object.keys(meta.jsonFields["attrs"]).length > 0 &&
    view &&
    view.items
  ) {
    const hasCustomAttrsField =
      Object.values(meta.fields ?? {}).some((f) => f.jsonField === "attrs") ||
      findViewItem(meta, "attrs") != null;

    if (view.type === "grid" && !hasCustomAttrsField) {
      const findLast = (
        array: any[],
        callback: (element: Schema, index?: number, array?: any[]) => boolean,
      ) => {
        for (let index = (array || []).length - 1; index >= 0; --index) {
          const element = array[index];
          if (callback(element, index, array)) {
            return element;
          }
        }
      };

      const lastShownIsButton = (itemList: Schema[]) => {
        const found = findLast(itemList, (item) => {
          return item && !item.hidden;
        });
        return found && found.type === "button";
      };

      view.items = ((items) => {
        let index = items.findIndex((x) => x.type === "button");
        if (index < 0 || !lastShownIsButton(items)) {
          index = items.length;
        }
        items.splice(index, 0, {
          type: "field",
          name: "attrs",
          jsonFields: Object.values(meta.jsonFields["attrs"]),
        });
        return items;
      })(view.items);
    }

    if (view.type === "form" && !hasCustomAttrsField) {
      view.items.push({
        type: "panel",
        title: i18n.get("Attributes"),
        itemSpan: 12,
        items: [
          {
            type: "field",
            name: "attrs",
            jsonFields: Object.values(meta.jsonFields["attrs"]),
          },
        ],
      });
    }
  }

  view = processJsonForm(view);
  if (meta.fields) {
    meta.fields = processFields(meta.fields);
  }

  (() => {
    let helps = (meta.helps = meta.helps || {});
    const items: Schema[] = [];

    if (Array.isArray(view.helpOverride) && view.helpOverride.length) {
      helps = meta.helps = view.helpOverride.reduce(
        (all, help) => {
          const { type, field } = help;
          return {
            ...all,
            [type]: {
              ...all[type],
              [field]: help,
            },
          };
        },
        {} as typeof helps,
      );

      if (helps?.tooltip?.__top__) {
        view.help = helps.tooltip.__top__.help;
      }
    }

    const help: Schema = helps.tooltip ?? {};
    const placeholder: Schema = helps.placeholder ?? {};
    const inline: Schema = helps.inline ?? {};

    forEach(view.items, (item) => {
      if (item.name && help[item.name]) {
        item.help = help[item.name].help;
      }
      if (meta.view && meta.view.type === "form") {
        if (item.name && placeholder[item.name]) {
          item.placeholder = placeholder[item.name].help;
        }
        if (item.name && inline[item.name] && !inline[item.name].used) {
          inline[item.name].used = true;
          items.push({
            type: "help",
            text: inline[item.name].help,
            css: inline[item.name].style,
            colSpan: 12,
          });
        }
      }
      items.push(item);
    });

    forEach(view.toolbar, (item) => {
      if (help[item.name]) {
        item.help = help[item.name].help;
      }
    });

    if (items.length) {
      view.items = items;
    }
  })();

  if (view.items) {
    // Skip custom fields added in view with forceHidden
    view.items = view.items.filter((x) => {
      if (x.name?.includes(".") && x.jsonField) {
        const jsonField = (view.fields ?? meta.fields ?? {})?.[x.name] ?? {};
        if (jsonField.forceHidden) {
          return false;
        }
      }
      return true;
    });
  }

  forEach(view.items, (item, itemIndex) => {
    if (["panel", "panel-related"].includes(item.type ?? "") && !parent) {
      toTopLevelItem(item);
    } else if (item.type === "panel-tabs") {
      item.items?.forEach((sub) => {
        toTopLevelItem(sub);
        sub.showFrame = true;
      });
    }

    processWidget(item);
    processSelection(item, meta?.view?.editable);

    // -to-many ?
    if (Array.isArray(view.fields)) {
      view.fields = processFields(view.fields);
    }

    const fields = view.fields ?? meta.fields ?? {};

    if (item.name) {
      const field = fields[item.name];
      forEach(fields[item.name], (value, key) => {
        if (!Object.hasOwn(item, key)) {
          item[key] = value;
        }
      });
      const isCustomField =
        item.name.includes(".") && meta.fields?.[item.name.split(".")[0]]?.json;

      // in case when custom field is used in form
      // but it doesn't exist in custom fields of that model
      // another case : it was deleted/removed
      if (isCustomField && !field) {
        item.serverType = "STRING";
        item.readonly = true;
        item.readonlyIf = "true";
      }
    }

    [
      "canNew",
      "canView",
      "canEdit",
      "canRemove",
      "canSelect",
      "canDelete",
    ].forEach((name) => {
      if (item[name] === "false" || item[name] === "true") {
        item[name] = item[name] === "true";
      }
    });

    if (item.items) {
      processView(meta, item, view);
    }

    if (item.password) {
      item.widget = "password";
    }

    const isFormField = !["grid", "panel-related"].includes(view.type ?? "");

    // convert dotted json fields
    if (isFormField && item.jsonField && item.name?.includes(".")) {
      const jsonField = fields?.[item.name] ?? {};
      const getWidgetAttrs = (field: Schema) => {
        if (typeof field.widgetAttrs === "string") {
          field.widgetAttrs = JSON.parse(field.widgetAttrs);
        }
        return processWidgetAttrs(field);
      };
      const jsonItem = {
        ...jsonField,
        ...item,
        widgetAttrs: { ...getWidgetAttrs(jsonField), ...getWidgetAttrs(item) },
        colSpan: 12,
        name: item.jsonPath,
        type: jsonField.type,
      };
      item = {
        type: "field",
        name: item.jsonField,
        jsonFields: [jsonItem],
        json: true,
        cols: 12,
        colSpan: item.colSpan ?? 6,
        showTitle: false,
      };
      processWidget(item);
      if (view.items) {
        view.items[itemIndex] = item;
      }
    }

    if (item.jsonFields && item.widget !== "json-raw") {
      const editor: Schema = {
        layout: view.type === "panel-json" ? "table" : undefined,
        flexbox: true,
        items: [],
      };
      let panel: Schema | null = null;
      let panelTab: Schema | null = null;

      item.jsonFields = item.jsonFields.filter(
        (x: JsonField) => !(x as JsonField).forceHidden,
      );
      item.jsonFields.sort((x: Schema, y: Schema) => {
        return x.sequence - y.sequence;
      });
      item.jsonFields.forEach((field: Schema) => {
        if (field.nameField) {
          field.nameColumn = field.nameField;
        }

        if (field.widgetAttrs) {
          if (typeof field.widgetAttrs === "string") {
            field.widgetAttrs = JSON.parse(field.widgetAttrs);
          }
          if (field.widgetAttrs.showTitle !== undefined) {
            field.showTitle = field.widgetAttrs.showTitle;
          }
          if (field.widgetAttrs.multiline) {
            field.type = "text";
          }
          if (field.widgetAttrs.targetName) {
            field.targetName = field.widgetAttrs.targetName;
          }

          // remove x- prefix from all widget attributes
          for (const key in field.widgetAttrs) {
            if (startsWith(key, "x-")) {
              field.widgetAttrs[key.substring(2)] = field.widgetAttrs[key];
              delete field.widgetAttrs[key];
            }
          }
        }
        processWidget(field);
        processSelection(field, meta?.view?.editable);
        // apply all widget attributes directly on field
        Object.assign(field, field.widgetAttrs);
        if (field.type === "panel" || field.type === "separator") {
          field.visibleInGrid = false;
        }
        if (field.type === "panel") {
          panel = { ...field, items: [] };
          if ((field.widgetAttrs || {}).sidebar && parent) {
            panel.sidebar = true;
            parent.width = "large";
          }
          if ((field.widgetAttrs || {}).tab) {
            panelTab = panelTab || {
              type: "panel-tabs",
              colSpan: 12,
              items: [],
            };
            panelTab.items?.push(panel);
          } else {
            editor.items?.push(panel);
          }
          return;
        }
        if (field.type !== "separator") {
          field.title = field.title || field.autoTitle;
        }
        const colSpan = (field.widgetAttrs || {}).colSpan || field.colSpan;
        if (field.type === "one-to-many") {
          field.type = "many-to-many";
          field.canSelect = false;
        }
        if (
          field.type === "separator" ||
          (field.type === "many-to-many" && !field.widget)
        ) {
          field.colSpan = colSpan || 12;
        }
        if (panel) {
          panel.items?.push(field);
        } else {
          editor.items?.push(field);
        }
      });

      item.jsonFields = (item.jsonFields as JsonField[]).reduce(
        (acc, x) => ({ ...acc, [x.name]: x }),
        {},
      );

      if (panelTab) {
        editor.items?.push(panelTab);
      }

      if (isFormField) {
        item.widget = "json-field";
        item.editor = editor;
        if (!item.viewer) {
          item.editor.viewer = true;
        }
      }
    }
  });

  // process view boolean attributes starts with can
  forEach(view, (value, key) => {
    if (key.startsWith("can") && ["false", "true"].includes(value)) {
      view[key] = value === "true";
    }
  });

  if (view.type === "grid") {
    if (view.widget) {
      view.widget = toKebabCase(view.widget);
    }
    // include json fields in grid
    let items: Schema[] = [];
    forEach(view.items, (item) => {
      if (item.jsonFields) {
        forEach(item.jsonFields, (field) => {
          const type = field.type || "text";
          if (
            type.indexOf("-to-many") === -1 &&
            field.visibleInGrid &&
            !field.forceHidden
          ) {
            items.push({ ...field, name: item.name + "." + field.name });
          }
        });
      } else {
        if (
          item.name?.includes(".") &&
          meta.fields &&
          !Array.isArray(meta.fields) &&
          meta.fields[item.name]?.jsonField
        ) {
          const field = meta.fields[item.name];
          if (typeof field.widgetAttrs === "string") {
            field.widgetAttrs = JSON.parse(field.widgetAttrs);
          }
          processWidget(field);
          if (
            typeof field.widgetAttrs === "object" &&
            field.widgetAttrs?.targetName
          ) {
            field.targetName = field.widgetAttrs.targetName;
          }
        }
        items.push(item);
      }
    });
    items = items.sort((x, y) => {
      return x.columnSequence - y.columnSequence;
    });
    view.items = items;
  }

  if (view.type === "form") {
    // more attrs action, except for mail
    if (view.model !== "com.axelor.mail.db.MailMessage") {
      const moreAttrs = "com.axelor.meta.web.MetaController:moreAttrs";
      view.onNew = view.onNew ? view.onNew + "," + moreAttrs : moreAttrs;
      view.onLoad = view.onLoad ? view.onLoad + "," + moreAttrs : moreAttrs;
    }

    // all top-level items should span to 12 cols by default
    view.items?.forEach((item) => {
      item.colSpan = item.colSpan ?? 12;
    });

    // wkf status
    const wkfStatusItem = {
      colSpan: 12,
      type: "field",
      name: "$wkfStatus",
      showTitle: false,
      widget: "wkf-status",
    };
    processWidget(wkfStatusItem);
    view.items?.unshift(wkfStatusItem);
  }

  if (meta.view.type !== "form") {
    view.items?.forEach((item) => {
      const serverType = (meta?.fields?.[item.name ?? ""] as unknown as Field)
        ?.type;
      if (item.type === "field" && serverType) {
        item.serverType = item.serverType || serverType;
      }
    });
  }
}

export function processWidgets(schema: Schema, parent?: Schema) {
  schema.colSpan = schema.colSpan ?? parent?.itemSpan;

  if (schema.sidebar) {
    schema.itemSpan = schema.itemSpan ?? 12;
  }

  if (Array.isArray(schema.items)) {
    schema.items.forEach((x) => processWidgets(x, schema));
    if (!parent) {
      schema.colSpan = schema.colSpan ?? 12;
    }
  }

  return schema;
}
