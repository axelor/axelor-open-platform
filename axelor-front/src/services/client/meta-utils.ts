import _ from "lodash";

import { i18n } from "./i18n";
import { ViewData } from "./meta";
import { ActionView, Field, Property, Schema } from "./meta.types";

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

export function processFields(fields: Property[] | Record<string, Property>) {
  let result: Record<string, Property> = {};
  if (_.isArray(fields)) {
    _.forEach(fields, (field) => {
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

export function processSelection(field: Schema) {
  if ((field.selection || field.selectionList) && !field.widget) {
    field.widget = "selection";
  }
  _.each(field.selectionList, (item) => {
    if (_.isString(item.data)) {
      item.data = JSON.parse(item.data);
    }
  });
}

export function processWidget(field: Schema) {
  const attrs: Record<string, any> = {};
  _.each(field.widgetAttrs || {}, (value, name) => {
    let val = value;
    if (value === "null") val = null;
    if (name === "widget" && value) val = _.kebabCase(value);
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
      ].indexOf(name) !== -1 &&
      /^(-)?\d+(\.\d+)?$/.test(value)
    ) {
      val = +value;
    }
    attrs[_.camelCase(name)] = val;
  });
  if (field.widget) {
    field.widget = _.kebabCase(field.widget);
  }
  field.widgetAttrs = attrs;
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

  _.each(view.items, (item) => {
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

export function findViewFields(
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
      _.each(items, (child) => {
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
    _.each(viewer.fields, (item) => {
      pushIn(item.name, collect);
    });
    if (viewer.fields) {
      viewer.fields = processFields(viewer.fields);
    }
  }

  function getNonDefaultTargetName(item: Schema, fields?: Record<string, Property>) {
    const { name, targetName } = item;
    if (name && targetName && fields) {
      const { targetName: fieldTargetName } = fields[name] ?? {};
      if (fieldTargetName && fieldTargetName !== targetName) {
        return targetName as string;
      }
    }
  }

  _.each(fields, (item) => {
    if (item.editor) acceptEditor(item);
    if (item.viewer) acceptViewer(item);
    if (item.name && item.type === "panel-related") {
      pushIn(item.name, items);
    } else if (item.items) {
      findViewFields(viewFields, item, result);
    } else if (item.name && item.type === "field") {
      pushIn(item.name, items);
      const targetName = getNonDefaultTargetName(item, viewFields);
      if (targetName) {
        const collect =
          result.related[item.name] || (result.related[item.name] = []);
        pushIn(targetName, collect);
      }
    }
  });

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

  if (meta.jsonAttrs && view && view.items) {
    if (view.type === "grid") {
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
          jsonFields: meta.jsonAttrs,
        });
        return items;
      })(view.items);
    }
    if (view.type === "form") {
      const hasCustomAttrsField = Object.values(meta.fields ?? {}).some(
        (f) => f.jsonField === "attrs",
      );
      !hasCustomAttrsField &&
        view.items.push({
          type: "panel",
          title: i18n.get("Attributes"),
          itemSpan: 12,
          items: [
            {
              type: "field",
              name: "attrs",
              jsonFields: meta.jsonAttrs,
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

    _.forEach(view.items, (item) => {
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

    _.forEach(view.toolbar, (item) => {
      if (help[item.name]) {
        item.help = help[item.name].help;
      }
    });

    if (items.length) {
      view.items = items;
    }
  })();

  _.forEach(view.items, (item, itemIndex) => {
    processWidget(item);
    processSelection(item);

    // -to-many ?
    if (Array.isArray(view.fields)) {
      view.fields = processFields(view.fields);
    }

    const fields = view.fields ?? meta.fields ?? {};

    if (item.name) {
      _.forEach(fields[item.name], (value, key) => {
        if (!Object.hasOwn(item, key)) {
          item[key] = value;
        }
      });
    }

    ["canNew", "canView", "canEdit", "canRemove", "canSelect"].forEach(
      (name) => {
        if (item[name] === "false" || item[name] === "true") {
          item[name] = item[name] === "true";
        }
      },
    );

    if (item.items) {
      processView(meta, item, view);
    }

    if (item.password) {
      item.widget = "password";
    }

    // convert dotted json fields
    if (item.jsonField && item.name?.includes(".")) {
      const jsonField = fields?.[item.name] ?? {};
      const jsonItem = {
        jsonField,
        ...item,
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
      item.jsonFields.sort((x: Schema, y: Schema) => {
        return x.sequence - y.sequence;
      });
      item.jsonFields.forEach((field: Schema) => {
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
            if (_.startsWith(key, "x-")) {
              field.widgetAttrs[key.substring(2)] = field.widgetAttrs[key];
              delete field.widgetAttrs[key];
            }
          }
        }
        processWidget(field);
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
          field.showTitle = false;
          field.colSpan = colSpan || 12;
        }
        if (panel) {
          panel.items?.push(field);
        } else {
          editor.items?.push(field);
        }
      });

      if (panelTab) {
        editor.items?.push(panelTab);
      }

      item.widget = "json-field";
      item.editor = editor;
      if (!item.viewer) {
        item.editor.viewer = true;
      }
    }
  });

  // process view boolean attributes starts with can
  _.each(view, (value, key) => {
    if (key.startsWith("can") && ["false", "true"].includes(value)) {
      view[key] = value === "true";
    }
  });

  // include json fields in grid
  if (view.type === "grid") {
    let items: Schema[] = [];
    _.forEach(view.items, (item) => {
      if (item.jsonFields) {
        _.forEach(item.jsonFields, (field) => {
          const type = field.type || "text";
          if (type.indexOf("-to-many") === -1 && field.visibleInGrid) {
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
    view.items?.unshift({
      colSpan: 12,
      type: "field",
      name: "$wkfStatus",
      showTitle: false,
      widget: "wkf-status",
    });
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
