import { Property, Schema, ViewType } from "@/services/client/meta.types";
import { ViewData } from "@/services/client/meta";

export function processViewItem(schema: Schema, field?: Schema) {
  const serverType = schema?.serverType || field?.type;
  const more = serverType ? { serverType } : {};
  return {
    ...field,
    ...schema,
    ...schema?.widgetAttrs,
    ...more,
  };
}

export function findViewItem<T extends ViewType>(
  meta: ViewData<T>,
  fieldName: string,
) {
  const { view, fields = {} } = meta;
  return walkSchema(view, fields, [], ({ path, schema, field }) => {
    const name = path.join(".");
    if (name === fieldName) {
      return processViewItem(schema, field);
    }
  });
}

export function findViewItems<T extends ViewType>(
  meta: ViewData<T>,
  predicate: (item: Schema) => boolean = () => true,
) {
  function collectItems(schema: Schema): Schema[] {
    const items = schema.type !== "panel-related" ? (schema.items ?? []) : [];
    const nested = items.flatMap((item) => collectItems(item));
    return [...items.filter(predicate), ...nested];
  }
  return collectItems(meta.view);
}

export function findJsonFieldItem<T extends ViewType>(
  meta: ViewData<T>,
  fieldName: string,
) {
  const { jsonFields } = meta;
  const [jsonField, ...jsonParts] = fieldName.split(".");

  if (jsonParts.length > 0) {
    const jsonPath = jsonParts.join(".");
    const fieldInfo =
      jsonFields?.[jsonField]?.[jsonPath] ??
      jsonFields?.[jsonField]?.[jsonParts[0]];

    if (fieldInfo) return fieldInfo;
  }

  for (const [modelField, _fields] of Object.entries(jsonFields ?? {})) {
    if (jsonField !== modelField && _fields[jsonField]) {
      return _fields[jsonField];
    }
    if (_fields[fieldName]) {
      return _fields[fieldName];
    }
  }
}

function findFields(schema: Schema) {
  const fields: Record<string, Property> =
    schema.fields ?? schema.editor?.fields ?? {};
  return fields;
}

function walkSchema(
  schema: Schema,
  schemaFields: Record<string, Property>,
  schemaPath: string[],
  callback: (params: {
    path: string[];
    name: string;
    schema: Schema;
    field?: Schema;
  }) => Schema | undefined,
): Schema | undefined {
  const name = schema.name;
  const items =
    schema.json && schema.jsonFields
      ? Object.values(schema.jsonFields)
      : (schema.items ?? schema.editor?.items ?? []);
  const path = name ? [...schemaPath, ...name.split(".")] : schemaPath;

  if (name) {
    const res = callback({
      path,
      name,
      schema,
      field: schemaFields[name],
    });
    if (res) return res;
  }

  const isRelation = schema.fields ?? schema.editor?.fields;
  const parentPath = isRelation ? path : schemaPath;
  const parentFields = isRelation ? findFields(schema) : schemaFields;

  for (const item of items) {
    const res = walkSchema(item, parentFields, parentPath, callback);
    if (res) return res;
  }
}
