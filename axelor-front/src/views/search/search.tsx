import { Box } from "@axelor/ui";
import { useCallback, useEffect, useMemo, useRef } from "react";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { atom, useAtom, useAtomValue } from "jotai";
import { generatePath, useLocation, useSearchParams } from "react-router-dom";
import isEqual from "lodash/isEqual";
import uniqueId from "lodash/uniqueId";
import cloneDeep from "lodash/cloneDeep";

import {
  ActionView,
  FormView,
  GridView,
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
import { useViewTab, useViewTabRefresh } from "@/view-containers/views/scope";
import { findActionView } from "@/services/client/meta-cache";
import { i18n } from "@/services/client/i18n";
import {
  isLegacyExpression,
  parseAngularExp,
  parseExpression,
} from "@/hooks/use-parser/utils";
import { DEFAULT_SEARCH_PAGE_SIZE } from "@/utils/app-settings.ts";
import {
  prepareSearchFields,
  prepareSearchFormMeta,
  processSearchField,
  searchData,
} from "./utils";
import { processView } from "@/services/client/meta-utils";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { createContextParams } from "@/views/form/builder/utils";

import styles from "./search.module.scss";

export function Search(props: ViewProps<SearchView>) {
  const recordsAtom = useMemo(() => atom<DataRecord[]>([]), []);
  const [records, setRecords] = useAtom(recordsAtom);
  const [state, setState, gridAtom] = useGridState();
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
  const { pathname } = useLocation();
  const { action, state: tabAtom } = useViewTab();
  const [searchParams] = useSearchParams();
  const currentSearchParams = useRef<Record<string, string>>({});

  const { params } = action;
  const isViewActive = useAtomValue(
    useMemo(
      () =>
        selectAtom(tabAtom, (tabState) => {
          const { action = "", mode = "" } = tabState.routes?.search ?? {};
          return (
            generatePath("/ds/:action/:mode", {
              action,
              mode,
            }) === pathname
          );
        }),
      [tabAtom, pathname],
    ),
  );
  const hasSearchParams = searchParams.size > 0;

  const getContext = useCallback<() => DataContext>(
    () => createContextParams(view, action) as DataContext,
    [view, action],
  );

  const { formMeta, formView } = useMemo(() => {
    const { searchForm } = meta;

    const model = searchForm?.model ?? "com.axelor.script.ScriptBindings";
    const searchFormMeta = prepareSearchFormMeta({
      ...meta,
      model,
    });

    const formView = cloneDeep(searchForm);
    if (formView) {
      const formViewMeta = {
        fields: searchFormMeta.fields,
        view: formView,
      } as ViewData<FormView>;
      processView(formViewMeta, formView);
      processSearchField(formView as unknown as SearchField);
    }

    return {
      formMeta: formView?.items
        ? {
            ...searchFormMeta,
            view: {
              ...searchFormMeta.view,
              items: formView?.items,
            },
          }
        : searchFormMeta,
      formView,
    };
  }, [meta]);

  const gridView = useMemo(() => {
    const { name, hilites, buttons, resultFields } = view;
    const fields = prepareSearchFields(
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

  const handleExecute = useCallback(
    (actionName: "onLoad" | "onNew") => {
      if (formView) {
        const action = formView[actionName];
        return action && actionExecutor.execute(action);
      }
    },
    [formView, actionExecutor],
  );

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
            ? (isLegacyExpression(viewTitle)
                ? parseAngularExp(viewTitle)
                : parseExpression(viewTitle))({
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
            __selected: selectValue?.length
              ? selectValue.map((x) => x.model)
              : null,
          },
          limit,
        });
        const records = recordList.map((record: DataRecord, ind: number) => {
          const selectFields = selects?.find(
            (s) => s.model === record._model,
          )?.fields;
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

        handleExecute("onLoad");
      },
      [
        formAtom,
        searchObjectsAtom,
        setRecords,
        selects,
        name,
        limit,
        onEdit,
        handleExecute,
      ],
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
        handleExecute("onNew");
      },
      [formAtom, setRecords, handleExecute],
    ),
  );

  const onGo = useAtomCallback(
    useCallback(
      async (get) => {
        const { action } = get(searchObjectsAtom);
        const { record: context } = get(formAtom);
        const actionName = action?.action;

        if (actionName) {
          const { rows, selectedRows } = get(gridAtom);
          const selected = rows?.[selectedRows?.[0] ?? -1]?.record;

          const _searchContext = (() => {
            const searchFields = Object.keys(context).reduce((obj, key) => {
              const value = context[key];
              return value === null ? obj : { ...obj, [key]: value };
            }, {});

            const modelByIds = (selectedRows ?? [])
              .map((ind) => rows[ind])
              .filter((row) => row.type === "row")
              .map((row) => row.record)
              .reduce(
                (modelIds, { _model, id }) => ({
                  ...modelIds,
                  [_model]: [...(modelIds[_model] ?? []), id],
                }),
                {} as Record<string, number[]>,
              );

            const _results = Object.keys(modelByIds).map((model) => ({
              model,
              ids: modelByIds[model],
            }));

            const {
              model: _model,
              view: { name: _viewName, type: _viewType },
            } = meta;

            return {
              ...searchFields,
              _results,
              _model,
              _action: actionName,
              _source: "go",
              _viewName,
              _viewType,
              _views: [{ name: _viewName, type: _viewType }],
            };
          })();

          const actionView = await findActionView(actionName, {
            _searchContext,
          });

          openTab({
            ...actionView,
            name: uniqueId("$act"),
            viewType: "form",
            ...(selected && {
              domain: `self.id IN (${selected.id})`,
            }),
            context: {
              ...actionView.context,
              _searchContext,
            },
          });
        }
      },
      [meta, gridAtom, searchObjectsAtom, formAtom],
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
    if (!isViewActive) return;

    const queryParams: Record<string, string> = {};
    for (const [key, value] of searchParams) {
      queryParams[key] = value;
    }

    if (isEqual(queryParams, currentSearchParams.current)) {
      return;
    }

    currentSearchParams.current = queryParams;

    const values = Object.keys(formMeta.fields || {}).reduce((values, name) => {
      const field = formMeta.fields?.[name];
      let value: any = queryParams[name];
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
  }, [
    isViewActive,
    searchParams,
    formMeta,
    setFormValues,
    currentSearchParams,
  ]);

  useAsyncEffect(async () => {
    // skip onNew when query params exist
    if (!isViewActive || hasSearchParams) return;
    // initial onNew
    handleExecute("onNew");
  }, [handleExecute]);

  // register tab:refresh
  useViewTabRefresh("search", onSearch);

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
          fields={formMeta.fields!}
          readonly={false}
          formAtom={formAtom}
          actionHandler={actionHandler}
          actionExecutor={actionExecutor}
          recordHandler={recordHandler}
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
          readonly={false}
          records={records}
          view={gridView}
          fields={meta.fields}
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
