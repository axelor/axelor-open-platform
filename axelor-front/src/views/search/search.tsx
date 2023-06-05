import { Box } from "@axelor/ui";
import { useCallback, useEffect, useMemo, useRef } from "react";
import { useAtomCallback } from "jotai/utils";
import { atom, useAtom, useSetAtom } from "jotai";
import { useLocation } from "react-router-dom";
import isEqual from "lodash/isEqual";
import uniqueId from "lodash/uniqueId";

import {
  ActionView,
  FormView,
  GridView,
  Property,
  SearchView,
} from "@/services/client/meta.types";
import { ViewProps } from "../types";
import { ViewData } from "@/services/client/meta";
import { Form, useFormHandlers } from "../form/builder";
import { SearchObjects, SearchObjectsState } from "./search-objects";
import { Grid as GridComponent } from "@/views/grid/builder";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { openTab_internal as openTab } from "@/hooks/use-tabs";
import { useGridActionExecutor, useGridState } from "../grid/builder/utils";
import { useViewDirtyAtom, useViewTab } from "@/view-containers/views/scope";
import { findActionView } from "@/services/client/meta-cache";
import { i18n } from "@/services/client/i18n";
import { searchData } from "./utils";
import styles from "./search.module.scss";

function prepareFields(fields: SearchView["searchFields"]) {
  return (fields || []).reduce((fields, _field) => {
    const field = {
      ..._field,
      type: _field.type?.toUpperCase?.() ?? "STRING",
    };

    if (field.type === "REFERENCE") {
      field.type = "MANY_TO_ONE";
    }

    if (field.type && ["INTEGER", "LONG", "DECIMAL"].includes(field.type)) {
      field.nullable = true;
    }

    return field.name ? { ...fields, [field.name]: field } : fields;
  }, {});
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
    []
  );
  const record = useRef({}).current;
  const { meta } = props;
  const { view } = meta;
  const { name, selects, limit = 80 } = view;
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
    [action]
  );

  const formMeta = useMemo(() => {
    const { view } = meta;
    const { title, name, searchFields } = view;
    const fields = prepareFields(searchFields);
    return {
      ...meta,
      view: {
        type: "form",
        items: [
          {
            colSpan: 12,
            name,
            type: "panel",
            title,
            items: searchFields?.map((field) => ({ ...field, type: "field" })),
          },
        ],
      },
      fields: fields as Record<string, Property>,
    } as unknown as ViewData<FormView>;
  }, [meta]);

  const gridView = useMemo(() => {
    const { hilites, buttons, resultFields } = view;
    return {
      type: "grid",
      hilites,
      items: [
        { title: i18n.get("Object"), name: "_modelTitle" },
        ...(resultFields || []),
        ...(buttons || []),
      ],
    } as GridView;
  }, [view]);

  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(formMeta, record);
  const setDirty = useSetAtom(useViewDirtyAtom());
  
  const onEdit = useAtomCallback(
    useCallback(
      (get, set, record: DataRecord, readonly: boolean = false) => {
        const { _id, _model, _modelTitle, _form, _grid } = record;
        if (!_model) return;
        const records = get(recordsAtom);
        const ids = records
          .filter((r) => r._model === _model)
          .map((r) => r._id)
          .join(",");

        openTab({
          title: _modelTitle!,
          model: _model,
          name: uniqueId("$act"),
          viewType: "form",
          views: [
            { type: "grid", name: _grid },
            { type: "form", name: _form },
          ],
          params: {
            forceReadonly: readonly,
          },
          context: {
            _showRecord: _id,
          },
          ...(ids && {
            domain: `self.id IN (${ids})`,
          }),
        });
      },
      [recordsAtom]
    )
  );

  const onView = useCallback(
    (record: DataRecord) => onEdit(record, true),
    [onEdit]
  );

  const onSearch = useAtomCallback(
    useCallback(
      async (get, set, options?: ActionView["params"]) => {
        const { record } = get(formAtom);
        const { selectValue } = get(searchObjectsAtom);

        if (!Object.values(record).some((x) => x)) {
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
        const records = recordList.map((record: DataRecord) => {
          const selectFields = selects?.find(
            (s) => s.model === record._model
          )?.fields;
          selectFields?.forEach((field) => {
            if (field.as && field.selectionList && record[field.as]) {
              record[field.as] = record[field.as]
                .split(/\s*,\s*/)
                .map(
                  (v: string) =>
                    field.selectionList?.find(
                      (item) => `${item.value}` === `${v}`
                    )?.title ?? v
                )
                .join(", ");
            }
          });
          return {
            ...record,
            _id: record.id,
            id: `${record._model}_${record.id}`,
          };
        });
        setRecords(records);
        setDirty(false);

        // check options to trigger on edit for initial search request only
        if (options?.showSingle && records.length === 1) {
          onEdit(records[0], !options?.forceEdit);
        }
      },
      [
        formAtom,
        searchObjectsAtom,
        setRecords,
        selects,
        name,
        limit,
        setDirty,
        onEdit,
      ]
    )
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
      [formAtom, setRecords]
    )
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
      [selected, searchObjectsAtom]
    )
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
      [formAtom, params, setRecords, onSearch]
    )
  );

  const gridActionExecutor = useGridActionExecutor(gridView, {
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
      <SearchObjects
        hasActions={!params?.hideActions}
        selects={selects}
        stateAtom={searchObjectsAtom}
        onGo={onGo}
        onSearch={onSearch}
        onClear={onClear}
      />
      <Box d="flex" flex={1}>
        <GridComponent
          showEditIcon
          readonly={false}
          records={records}
          view={gridView}
          state={state}
          setState={setState}
          actionExecutor={gridActionExecutor}
          onEdit={onEdit}
          onView={onView}
          onSearch={onSearch as any}
        />
      </Box>
    </Box>
  );
}
