import _ from "lodash";

import { i18n } from "./i18n";
import { ViewData } from "./meta";
import { ActionView, Field, Property, Schema } from "./meta.types";

function processJsonForm(view: Schema) {
  if (view.type !== "form") return view;
  if (view.model !== "com.axelor.meta.db.MetaJsonRecord") return view;

  var panel = view.items?.[0];
  var jsonField = panel?.items?.[0];
  var jsonFields = jsonField?.jsonFields ?? [];

  var first = jsonFields?.[0];
  if (first?.type === "panel" && panel) {
    panel.type = "panel-json";
    if (first.widgetAttrs) {
      var attrs = JSON.parse(first.widgetAttrs);
      view.width = view.width || attrs.width;
    }
  }

  return view;
}

function processFields(fields: Property[] | Record<string, Property>) {
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
  var attrs: Record<string, any> = {};
  _.each(field.widgetAttrs || {}, (value, name) => {
    var val = value;
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
    var my = view.toolbar || toolbar;
    if (my !== toolbar) {
      my = my.concat(toolbar);
    }
    view.toolbar = my;
  }
  function UseItems(view: Schema) {
    return UseIncluded(view);
  }

  var items: Schema[] = [];

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
  view: Schema,
  res?: { fields: string[]; related: Record<string, string[]> }
) {
  var result = res || {
    fields: [],
    related: {},
  };
  var items = result.fields;
  var fields = view.items;

  if (!fields) return result;
  if (view.items && !view._included) {
    view._included = true;
    fields = view.items = UseIncluded(view);
  }

  function acceptEditor(item: Schema) {
    var collect = items;
    var editor = item.editor;
    if (item.name && item.target) {
      collect = result.related[item.name] || (result.related[item.name] = []);
    }
    if (editor.fields) {
      editor.fields = processFields(editor.fields);
    }
    var acceptItems = (items: Schema[] = []) => {
      _.each(items, (child) => {
        if (
          child.name &&
          collect.indexOf(child.name) === -1 &&
          child.type === "field"
        ) {
          collect.push(child.name);
        } else if (child.type === "panel") {
          acceptItems(child.items);
        }
        if (child.widget === "ref-select") {
          collect.push(child.related);
        }
        if (typeof child.depends === "string") {
          child.depends.split(/\s*,\s*/).forEach((name) => {
            collect.push(name);
          });
        }
        processWidget(child);
      });
    };
    acceptItems(editor.items);
  }

  function acceptViewer(item: Schema) {
    var collect = items;
    var viewer = item.viewer;
    if (item.name && item.target) {
      collect = result.related[item.name] || (result.related[item.name] = []);
    }
    _.each(viewer.fields, (item) => {
      collect.push(item.name);
    });
    if (viewer.fields) {
      viewer.fields = processFields(viewer.fields);
    }
  }

  _.each(fields, (item) => {
    if (item.editor) acceptEditor(item);
    if (item.viewer) acceptViewer(item);
    if (item.name && item.type === "panel-related") {
      items.push(item.name);
    } else if (item.items) {
      findViewFields(item, result);
    } else if (item.name && item.type === "field") {
      items.push(item.name);
    }

    // process tag-select
    processWidget(item);
    if (item.widget === "tag-select") {
      // fetch colors
      if (item.name && item.colorField) {
        (result.related[item.name] || (result.related[item.name] = [])).push(
          item.colorField
        );
      }
      // fetch target names
      if (item.name && item.targetName) {
        (result.related[item.name] || (result.related[item.name] = [])).push(
          item.targetName
        );
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
  parent?: Schema
) {
  meta = meta || {};
  view = view || {};

  if (meta.jsonAttrs && view && view.items) {
    if (view.type === "grid") {
      function findLast(
        array: any[],
        callback: (element: Schema, index?: number, array?: any[]) => boolean
      ) {
        for (var index = (array || []).length - 1; index >= 0; --index) {
          var element = array[index];
          if (callback(element, index, array)) {
            return element;
          }
        }
      }

      function lastShownIsButton(itemList: Schema[]) {
        var found = findLast(itemList, (item) => {
          return item && !item.hidden;
        });
        return found && found.type === "button";
      }

      view.items = ((items) => {
        var index = items.findIndex((x) => x.type === "button");
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
        (f) => f.jsonField === "attrs"
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
    var helps = (meta.helps = meta.helps || {});
    var items: Schema[] = [];

    if (Array.isArray(view.helpOverride) && view.helpOverride.length) {
      helps = meta.helps = view.helpOverride.reduce((all, help) => {
        const { type, field } = help;
        return {
          ...all,
          [type]: {
            ...all[type],
            [field]: help,
          },
        };
      }, {} as typeof helps);

      if (helps?.tooltip?.__top__) {
        view.help = helps.tooltip.__top__.help;
      }
    }

    var help: Schema = helps.tooltip ?? {};
    var placeholder: Schema = helps.placeholder ?? {};
    var inline: Schema = helps.inline ?? {};

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

  _.forEach(view.items, (item) => {
    processWidget(item);
    processSelection(item);

    // -to-many ?
    if (Array.isArray(view.fields)) {
      view.fields = processFields(view.fields);
    }

    const fields = view.fields ?? meta.fields ?? {};

    if (item.name) {
      _.forEach(fields[item.name], (value, key) => {
        if (!item.hasOwnProperty(key)) {
          item[key] = value;
        }
      });
    }

    ["canNew", "canView", "canEdit", "canRemove", "canSelect"].forEach(
      (name) => {
        if (item[name] === "false" || item[name] === "true") {
          item[name] = item[name] === "true";
        }
      }
    );

    if (item.items) {
      processView(meta, item, view);
    }

    if (item.password) {
      item.widget = "password";
    }

    if (item.jsonFields && item.widget !== "json-raw") {
      var editor: Schema = {
        layout: view.type === "panel-json" ? "table" : undefined,
        flexbox: true,
        items: [],
      };
      var panel: Schema | null = null;
      var panelTab: Schema | null = null;
      item.jsonFields.sort((x: Schema, y: Schema) => {
        return x.sequence - y.sequence;
      });
      item.jsonFields.forEach((field: Schema) => {
        if (field.widgetAttrs) {
          field.widgetAttrs = JSON.parse(field.widgetAttrs);
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
          for (var key in field.widgetAttrs) {
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
        var colSpan = (field.widgetAttrs || {}).colSpan || field.colSpan;
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
    var items: Schema[] = [];
    _.forEach(view.items, (item) => {
      if (item.jsonFields) {
        _.forEach(item.jsonFields, (field) => {
          var type = field.type || "text";
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
          var field = meta.fields[item.name];
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
    // more attrs action
    var moreAttrs = "com.axelor.meta.web.MetaController:moreAttrs";
    view.onNew = view.onNew ? view.onNew + "," + moreAttrs : moreAttrs;
    view.onLoad = view.onLoad ? view.onLoad + "," + moreAttrs : moreAttrs;
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
