import { Box } from "@axelor/ui";
import { useCallback, useEffect, useMemo, useRef } from "react";
import { useAtomCallback } from "jotai/utils";
import { atom, useAtom } from "jotai";
import { useLocation } from "react-router-dom";
import isEqual from "lodash/isEqual";
import uniqueId from "lodash/uniqueId";

import {
  ActionView,
  FormView,
  GridView,
  Property,
  Schema,
  SearchField,
  SearchResultField,
  SearchView,
} from "@/services/client/meta.types";
import { ViewProps } from "../types";
import { ViewData } from "@/services/client/meta";
import { alerts } from "@/components/alerts";
import { Form, useFormHandlers } from "../form/builder";
import { useActionExecutor } from "../form/builder/scope";
import { SearchObjects, SearchObjectsState } from "./search-objects";
import { Grid as GridComponent } from "@/views/grid/builder";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { openTab_internal as openTab } from "@/hooks/use-tabs";
import { useGridState } from "../grid/builder/utils";
import { useViewTab } from "@/view-containers/views/scope";
import { findActionView } from "@/services/client/meta-cache";
import { i18n } from "@/services/client/i18n";
import { isLegacyExpression, parseAngularExp, parseExpression } from "@/hooks/use-parser/utils";
import { DEFAULT_SEARCH_PAGE_SIZE } from "@/utils/app-settings.ts";
import { toKebabCase } from "@/utils/names";
import { searchData } from "./utils";
import styles from "./search.module.scss";
import { getFieldServerType, getWidget } from "@/views/form/builder/utils";

function prepareFields(fields: SearchView["searchFields"]) {
  return (fields || []).reduce((fields, _field) => {
    const field = { ..._field, type: _field.type?.toUpperCase?.() };

    if (field.type === "REFERENCE") {
      field.type = "MANY_TO_ONE";
    }
    if ((field.selection || field.selectionList) && !field.widget) {
      field.widget = "Selection";
    }

    field.serverType = getFieldServerType({...field, type: "field"}, field) ?? "STRING";
    field.widget = getWidget(field, null);

    if (["INTEGER", "LONG", "DECIMAL"].includes(field.serverType)) {
      field.nullable = true;
    }

    return field.name
      ? {
          ...fields,
          [field.name]: { ...field, type: "field", canDirty: false },
        }
      : fields;
  }, {}) as Record<string, Property>;
}

export function Search(props: ViewProps<SearchView>) {
  const location = useLocation();
  const recordsAtom = useMemo(() => atom<DataRecord[]>([]), []);
  const [records, setRecords] = useAtom(recordsAtom);
  const [state, setState] = useGridState();
  const searchObjectsAtom = useMemo(
    () =>
      atom<SearchObjectsState>({
        selectValue: [],
        actionCategory: null,
        actionSubCategory: null,
        action: null,
      }),
    [],
  );
  const record = useRef({}).current;
  const { meta } = props;
  const { view } = meta;
  const { name, selects, actionMenus, limit = DEFAULT_SEARCH_PAGE_SIZE } = view;
  const { action } = useViewTab();
  const { params } = action;
  const queryString = (location.search || "").slice(1);

  const getContext = useCallback<() => DataContext>(
    () => ({
      ...action.context,
      _viewName: action.name,
      _viewType: action.viewType,
      _views: action.views,
    }),
    [action],
  );

  const formMeta = useMemo(() => {
    const { view } = meta;
    const { title, name, selects, searchFields } = view;
    const fields = prepareFields(searchFields);
    const model = meta.model || selects?.find((s) => s.model)?.model;

    function process(item: SearchField) {
      const $item = (fields[item.name] || {}) as Schema;
      switch (toKebabCase($item.widget ?? $item.serverType ?? "")) {
        case "many-to-one":
        case "one-to-one":
        case "suggest-box":
          $item.canNew = false;
          $item.canEdit = false;
          break;
        case "one-to-many":
        case "many-to-many":
        case "master-detail":
          $item.hidden = true;
          break;
      }
      return $item;
    }

    return {
      ...meta,
      model,
      view: {
        type: "form",
        model,
        items: [
          {
            colSpan: 12,
            name,
            type: "panel",
            title,
            items: searchFields?.map(process),
          },
        ],
      },
      fields: fields as Record<string, Property>,
    } as unknown as ViewData<FormView>;
  }, [meta]);

  const gridView = useMemo(() => {
    const { name, hilites, buttons, resultFields } = view;
    const fields = prepareFields(
      resultFields,
    ) as unknown as SearchResultField[];
    const items = [...(Object.values(fields) || [])];
    const objItemInd = items?.findIndex((item) => item.name === "object");
    const objItem = {
      title: i18n.get("Object"),
      name: "_modelTitle",
    } as SearchField;

    if (items[objItemInd]) {
      items[objItemInd] = {
        ...items[objItemInd],
        ...objItem,
        ...(+(items[objItemInd]?.width ?? -1) === 0 && {
          hidden: true,
        }),
      };
    } else {
      items?.unshift(objItem);
    }

    return {
      name,
      type: "grid",
      hilites,
      items: [...(items || []), ...(buttons || [])].filter(
        (item) => item.hidden !== true,
      ),
    } as GridView;
  }, [view]);

  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(formMeta, record);

  const onEdit = useAtomCallback(
    useCallback(
      (get, set, record: DataRecord, readonly: boolean = false) => {
        const { $$id, _model, _modelTitle, _form, _grid } = record;
        if (!_model) return;
        const viewTitle = selects?.find((s) => s.model === _model)?.viewTitle;
        const records = get(recordsAtom);
        const ids = records
          .filter((r) => r._model === _model)
          .map((r) => r.$$id)
          .join(",");

        const title =
          (viewTitle
            ? (isLegacyExpression(viewTitle) ? parseAngularExp(viewTitle) : parseExpression(viewTitle))({
              ...record,
              id: $$id,
            })
            : "") || _modelTitle;

        openTab({
          title,
          model: _model,
          name: uniqueId("$act"),
          viewType: "form",
          views: [
            { type: "grid", name: _grid },
            { type: "form", name: _form },
          ],
          params: {
            forceEdit: !readonly,
            forceTitle: true,
          },
          context: {
            _showRecord: $$id,
          },
          ...(ids && {
            domain: `self.id IN (${ids})`,
          }),
        });
      },
      [recordsAtom, selects],
    ),
  );

  const onView = useCallback(
    (record: DataRecord) => onEdit(record, true),
    [onEdit],
  );

  const onSearch = useAtomCallback(
    useCallback(
      async (get, set, options?: ActionView["params"]) => {
        const { record } = get(formAtom);
        const { selectValue } = get(searchObjectsAtom);

        if (!Object.values(record).some((x) => x)) {
          setRecords((records) => (records.length ? [] : records));
          return;
        }

        const recordList = await searchData({
          data: {
            ...record,
            __name: name,
            __selected: selectValue.length
              ? selectValue.map((x) => x.model)
              : null,
          },
          limit,
        });
        const records = recordList.map((record: DataRecord, ind: number) => {
          const selectFields = selects?.find((s) => s.model === record._model)
            ?.fields;
          selectFields?.forEach((field) => {
            if (field.as && field.selectionList && record[field.as]) {
              record[field.as] = record[field.as]
                .split(/\s*,\s*/)
                .map(
                  (v: string) =>
                    field.selectionList?.find(
                      (item) => `${item.value}` === `${v}`,
                    )?.title ?? v,
                )
                .join(", ");
            }
          });
          return {
            ...record,
            $$id: record.id,
            id: ind + 1,
          };
        });
        setRecords(records);

        if (records.length === 0) {
          alerts.info({
            message: i18n.get("No records found."),
          });
          // check options to trigger on edit for initial search request only
        } else if (options?.showSingle && records.length === 1) {
          onEdit(records[0], !options?.forceEdit);
        }
      },
      [formAtom, searchObjectsAtom, setRecords, selects, name, limit, onEdit],
    ),
  );

  const onClear = useAtomCallback(
    useCallback(
      (get, set) => {
        set(formAtom, {
          ...get(formAtom),
          record: {},
          dirty: false,
        });
        setRecords([]);
      },
      [formAtom, setRecords],
    ),
  );

  const { rows, selectedRows } = state;
  const selected = rows?.[selectedRows?.[0] ?? -1]?.record;

  const onGo = useAtomCallback(
    useCallback(
      async (get) => {
        const { action } = get(searchObjectsAtom);
        const actionName = action?.action;
        if (actionName) {
          const actionView = await findActionView(actionName);
          openTab({
            ...actionView,
            name: uniqueId("$act"),
            viewType: "form",
            ...(selected && {
              domain: `self.id IN (${selected.id})`,
            }),
            context: {
              ...actionView.context,
              _ref: { ...(selected ?? {}), _action: actionName },
            },
          });
        }
      },
      [selected, searchObjectsAtom],
    ),
  );

  const setFormValues = useAtomCallback(
    useCallback(
      (get, set, record: DataRecord) => {
        const state = get(formAtom);
        if (isEqual(state.record, record)) return;
        set(formAtom, {
          ...state,
          record,
          dirty: false,
        });
        setRecords([]);
        onSearch(params);
      },
      [formAtom, params, setRecords, onSearch],
    ),
  );

  const handleFormKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLDivElement>) => {
      if (e.key === "Enter" && !e.defaultPrevented) {
        onSearch();
      }
    },
    [onSearch],
  );

  const gridActionExecutor = useActionExecutor(gridView, {
    getContext,
    onRefresh: onSearch,
  });

  useEffect(() => {
    const params: Record<string, string> = queryString
      ?.split("&")
      .reduce((obj, str) => {
        let [key, value] = str.split("=");
        return { ...obj, [key]: value };
      }, {});

    const values = Object.keys(formMeta.fields || {}).reduce((values, name) => {
      const field = formMeta.fields?.[name];
      let value: any = params[name];
      if (!value) return values;

      if (field?.targetName) {
        if (isNaN(value)) {
          value = null;
        } else {
          value = { id: value };
        }
      }
      return {
        ...values,
        [name]: value,
      };
    }, {});

    setFormValues(values);
  }, [queryString, formMeta, setFormValues]);

  return (
    <Box
      d="flex"
      flexDirection="column"
      flex={1}
      p={2}
      className={styles.container}
    >
      <Box onKeyDown={handleFormKeyDown}>
        <Form
          schema={formMeta.view}
          fields={formMeta.fields}
          readonly={false}
          formAtom={formAtom}
          actionHandler={actionHandler}
          actionExecutor={actionExecutor}
          recordHandler={recordHandler}
          {...({} as any)}
        />
      </Box>
      <SearchObjects
        hasActions={!params?.hideActions}
        selects={selects}
        actionMenus={actionMenus}
        stateAtom={searchObjectsAtom}
        onGo={onGo}
        onSearch={onSearch}
        onClear={onClear}
      />
      <Box d="flex" flex={1}>
        <GridComponent
          showEditIcon
          allowCheckboxSelection={false}
          readonly={false}
          records={records}
          view={gridView}
          state={state}
          setState={setState}
          actionExecutor={gridActionExecutor}
          onEdit={onEdit}
          onView={onView}
        />
      </Box>
    </Box>
  );
}
