import { useSetAtom } from "jotai";
import { useAtomCallback } from "jotai/utils";
import { useCallback, useEffect, useMemo } from "react";

import { Box } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { PageText } from "@/components/page-text";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useDataStore } from "@/hooks/use-data-store";
import { useTemplate } from "@/hooks/use-parser";
import { usePerms } from "@/hooks/use-perms";
import { useEditor } from "@/hooks/use-relation";
import { useShortcuts } from "@/hooks/use-shortcut";
import { SearchOptions } from "@/services/client/data";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { CardsView } from "@/services/client/meta.types";
import { AdvanceSearch } from "@/view-containers/advance-search";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import { useViewSwitch, useViewTab, useViewTabRefresh } from "@/view-containers/views/scope";

import { usePrepareContext } from "../form/builder";
import { useFormScope } from "../form/builder/scope";
import { useGridActionExecutor } from "../grid/builder/utils";
import { ViewProps } from "../types";
import { Card } from "./card";

import { legacyClassNames } from "@/styles/legacy";
import styles from "./cards.module.scss";

export function Cards(props: ViewProps<CardsView>) {
  const { meta, dataStore, searchAtom } = props;
  const { view, fields } = meta;
  const { action, dashlet, popup, popupOptions } = useViewTab();
  const showEditor = useEditor();

  const switchTo = useViewSwitch();
  const { hasButton } = usePerms(meta.view, meta.perms);
  const { formAtom } = useFormScope();
  const getFormContext = usePrepareContext(formAtom);
  const hasEditPopup = dashlet || view.editWindow === "popup";
  const hasAddPopup = hasEditPopup || view.editWindow === "popup-new";

  const getContext = useCallback(
    () => ({
      ...action.context,
      _model: action.model,
    }),
    [action.context, action.model]
  );

  const getActionContext = useCallback(() => {
    return {
      ...getContext(),
      _viewName: action.name,
      _viewType: action.viewType,
      _views: action.views,
    };
  }, [action.name, action.viewType, action.views, getContext]);

  const actionExecutor = useGridActionExecutor(view, {
    getContext: getActionContext,
    onRefresh: () => onSearch({}),
  });

  const { width, minWidth } = useMemo(() => {
    const width = view.width || "calc(100% / 3)";
    const widths = width
      .split(":")
      .map((x) => x.trim())
      .filter(Boolean);
    return {
      width: widths[0],
      minWidth: widths[1],
    };
  }, [view.width]);

  const onSearch = useAtomCallback(
    useCallback(
      (get, set, options: Partial<SearchOptions> = {}) => {
        const { query: filter = {} } = searchAtom ? get(searchAtom) : {};
        const names = Object.keys(fields ?? {});

        if (dashlet) {
          const { _domainAction, ...formContext } = getFormContext() ?? {};
          const { _domainContext } = filter;
          filter._domainContext = {
            ..._domainContext,
            ...formContext,
          };
          filter._domainAction = _domainAction;
        }
        return dataStore.search({
          filter,
          fields: names,
          ...options,
        });
      },
      [dataStore, fields, dashlet, searchAtom, getFormContext]
    )
  );

  const records = useDataStore(dataStore, (ds) => ds.records);
  const Template = useTemplate(view.template || "");

  useAsyncEffect(async () => {
    await onSearch();
  }, [onSearch]);

  const onDelete = useCallback(
    async (record: DataRecord) => {
      const confirmed = await dialogs.confirm({
        content: i18n.get(
          "Do you really want to delete the selected record(s)?"
        ),
        yesTitle: i18n.get("Delete"),
      });
      if (confirmed) {
        try {
          await dataStore.delete([
            { id: record.id!, version: record.version! },
          ]);
        } catch { }
      }
    },
    [dataStore]
  );

  const onEdit = useCallback(
    (record: DataRecord, readonly = false) => {
      const recordId = record.id || 0;
      const id = recordId > 0 ? String(recordId) : "";
      switchTo("form", {
        route: { id },
        props: { readonly },
      });
    },
    [switchTo]
  );

  const onEditInPopup = useCallback(
    (record: DataRecord, readonly = false) => {
      const viewName = action.views?.find((v) => v.type === "form")?.name;
      const { title, model } = view;
      model &&
        showEditor({
          title: title ?? "",
          model,
          viewName,
          record,
          readonly,
          onSelect: () => onSearch({}),
        });
    },
    [showEditor, view, action, onSearch]
  );

  const onNew = useCallback(() => {
    hasAddPopup ? onEditInPopup({}) : onEdit({});
  }, [hasAddPopup, onEdit, onEditInPopup]);

  const onView = useCallback(
    (record: DataRecord) => {
      hasEditPopup ? onEditInPopup(record, true) : onEdit(record, true);
    },
    [hasEditPopup, onEdit, onEditInPopup]
  );

  const setPopupHandlers = useSetAtom(usePopupHandlerAtom());
  const setDashletHandlers = useSetAtom(useDashletHandlerAtom());

  useEffect(() => {
    if (popup) {
      setPopupHandlers({
        dataStore: dataStore,
        onSearch,
      });
    }
  }, [onSearch, popup, dataStore, setPopupHandlers]);

  useEffect(() => {
    if (dashlet) {
      setDashletHandlers({
        dataStore,
        view,
        onRefresh: () => onSearch({}),
      });
    }
  }, [dashlet, view, dataStore, onSearch, setDashletHandlers]);

  const showToolbar = popupOptions?.showToolbar !== false;

  const { offset = 0, limit = 40, totalCount = 0 } = dataStore.page;
  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;

  const canNew = hasButton("new");

  const handlePrev = useCallback(
    () => onSearch({ offset: offset - limit }),
    [limit, offset, onSearch]
  );
  const handleNext = useCallback(
    () => onSearch({ offset: offset + limit }),
    [limit, offset, onSearch]
  );

  useShortcuts({
    viewType: view.type,
    onNew: canNew ? onNew : undefined,
    onRefresh: onSearch,
  });

  // register tab:refresh
  useViewTabRefresh("cards", onSearch);

  return (
    <Box className={legacyClassNames(styles.container, "cards-view", "row-fluid")}>
      {showToolbar && (
        <ViewToolBar
          meta={meta}
          actionExecutor={actionExecutor}
          actions={[
            {
              key: "new",
              text: i18n.get("New"),
              hidden: !canNew,
              iconProps: {
                icon: "add",
              },
              onClick: onNew,
            },
            {
              key: "refresh",
              text: i18n.get("Refresh"),
              iconProps: {
                icon: "refresh",
              },
              onClick: () => onSearch(),
            },
          ]}
          pagination={{
            canPrev,
            canNext,
            onPrev: handlePrev,
            onNext: handleNext,
            text: () => <PageText dataStore={dataStore} />,
          }}
        >
          {searchAtom && (
            <AdvanceSearch
              stateAtom={searchAtom}
              dataStore={dataStore}
              items={view.items}
              customSearch={view.customSearch}
              freeSearch={view.freeSearch}
              onSearch={onSearch}
            />
          )}
        </ViewToolBar>
      )}
      {records.length > 0 && (
        <Box className={styles.cards}>
          {records.map((record) => (
            <Card
              key={record.id}
              record={record}
              fields={fields}
              onView={onView}
              Template={Template}
              width={width}
              minWidth={minWidth}
              getContext={getContext}
              {...(hasButton("edit") && { onEdit })}
              {...(hasButton("edit") && {
                onEdit: hasEditPopup ? onEditInPopup : onEdit,
              })}
              {...(hasButton("delete") && { onDelete })}
            />
          ))}
        </Box>
      )}
      {records.length === 0 && (
        <Box d="flex" flex={1} justifyContent="center" alignItems="center">
          <Box style={{ fontSize: "0.9rem" }}>
            {i18n.get("No records found.")}
          </Box>
        </Box>
      )}
    </Box>
  );
}
