import { request } from "@/services/client/client";
import { DataContext } from "@/services/client/data.types";
import {
  FormView,
  MenuItem,
  Property,
  Schema,
  SearchField,
  SearchView,
  View,
} from "@/services/client/meta.types";
import { LoadingCache } from "@/utils/cache";
import { toKebabCase } from "@/utils/names";
import { getFieldServerType, getWidget } from "../form/builder/utils";
import { ViewData } from "@/services/client/meta";
import { toTopLevelItem } from "@/services/client/meta-utils";

const cache = new LoadingCache<Promise<MenuItem[]>>();

export async function fetchMenus(parent?: string) {
  const queryString = parent ? `?parent=${parent}` : "";
  const url = `ws/search/menu${queryString}`;

  return await cache.get(url, async () => {
    const resp = await request({ url, method: "GET" });

    if (resp.ok) {
      const { status, data } = await resp.json();
      return status === 0 ? (data ?? []) : [];
    }

    return [];
  });
}

export async function searchData({
  data,
  limit,
}: {
  data: DataContext;
  limit?: number;
}) {
  const resp = await request({
    url: "ws/search",
    method: "POST",
    body: {
      data,
      limit,
    },
  });

  if (resp.ok) {
    const { status, data = [] } = await resp.json();
    return status === 0 ? data : [];
  }

  return Promise.reject(500);
}

export function prepareSearchFields(fields?: SearchField[]) {
  return (fields || []).reduce((_fields, _field) => {
    const field = { ..._field, type: _field.type?.toUpperCase?.() };

    if (field.type === "REFERENCE") {
      field.type = "MANY_TO_ONE";
      field.canNew = "false";
      field.canEdit = "false";
      if (field.multiple) {
        field.type = "ONE_TO_MANY";
        field.widget = "tag-select";
      }
    }
    if ((field.selection || field.selectionList) && !field.widget) {
      field.widget = "Selection";
    }

    field.serverType =
      getFieldServerType({ ...field, type: "field" }, field) ?? "STRING";
    field.widget = getWidget(field, null);

    if (["INTEGER", "LONG", "DECIMAL"].includes(field.serverType)) {
      field.nullable = true;
    }

    return field.name
      ? {
          ..._fields,
          [field.name]: { ...field, type: "field" },
        }
      : _fields;
  }, {}) as Record<string, Property>;
}

export function processSearchField(item: Schema) {
  if ((item as Schema).items) {
    (item as Schema).items?.forEach(processSearchField);
  } else {
    (item as Schema).canDirty = false;
  }

  const type = toKebabCase(item.widget ?? item.serverType ?? item.type);

  switch (type) {
    case "many-to-one":
    case "one-to-one":
    case "suggest-box":
      item.canNew = false;
      item.canEdit = false;
      break;
    case "one-to-many":
    case "many-to-many":
    case "master-detail":
      item.hidden = true;
      break;
  }
}

export function prepareSearchFormMeta<T extends View>(meta: ViewData<T>) {
  const { model, view } = meta;
  const { name, title } = view;
  const { searchFields = [] } = view as SearchView;
  const fields = prepareSearchFields(searchFields);

  return {
    ...meta,
    view: {
      type: "form",
      model,
      items: [
        toTopLevelItem({
          colSpan: 12,
          name,
          type: "panel",
          title,
          items: (() => {
            const items = searchFields.map((field) => ({
              ...field,
              ...fields[field.name ?? ""],
            }));
            items.forEach(processSearchField);
            return items;
          })(),
        }),
      ],
    },
    fields: fields as Record<string, Property>,
  } as unknown as ViewData<FormView>;
}
