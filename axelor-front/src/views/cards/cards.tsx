import { useSetAtom } from "jotai";
import { useAtomCallback } from "jotai/utils";
import { useCallback, useEffect, useMemo } from "react";

import { Box } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { PageText } from "@/components/page-text";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useDataStore } from "@/hooks/use-data-store";
import { useTemplate } from "@/hooks/use-parser";
import { useViewPerms } from "@/hooks/use-perms";
import { useManyEditor } from "@/hooks/use-relation";
import { useSearchTranslate } from "@/hooks/use-search-translate";
import { useShortcuts } from "@/hooks/use-shortcut";
import { SearchOptions } from "@/services/client/data";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { CardsView } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { DEFAULT_PAGE_SIZE } from "@/utils/app-settings.ts";
import { AdvanceSearch } from "@/view-containers/advance-search";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import {
  useViewContext,
  useViewSwitch,
  useViewTab,
  useViewTabRefresh,
} from "@/view-containers/views/scope";

import { useActionExecutor, useAfterActions } from "../form/builder/scope";
import { getSortBy, parseOrderBy } from "../grid/builder/utils";
import { createContextParams } from "../form/builder/utils";
import { ViewProps } from "../types";
import { Card } from "./card";

import styles from "./cards.module.scss";

export function Cards(props: ViewProps<CardsView>) {
  const { meta, dataStore, searchAtom } = props;
  const { view, fields } = meta;
  const { action, dashlet, popup, popupOptions } = useViewTab();

  const switchTo = useViewSwitch();
  const { hasButton } = useViewPerms(meta);
  const getViewContext = useViewContext();

  const canNew = hasButton("new");
  const canEdit = hasButton("edit");
  const canDelete = hasButton("delete");

  const { onDelete: onDeleteAction } = view;
  const hasEditPopup = dashlet || view.editWindow === "popup";
  const hasAddPopup = hasEditPopup || view.editWindow === "popup-new";

  const showEditor = useManyEditor(action, dashlet);

  const getActionContext = useCallback(
    () => ({
      ...getViewContext(true),
      ...createContextParams(view, action),
    }),
    [getViewContext, view, action],
  );

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

  const orderBy = useMemo(() => parseOrderBy(view.orderBy), [view.orderBy]);
  const getSearchTranslate = useSearchTranslate(orderBy, fields);

  const doSearch = useAtomCallback(
    useCallback(
      (get, set, options: Partial<SearchOptions> = {}) => {
        const sortBy = getSortBy(orderBy);
        const names = Object.keys(fields ?? {});
        let { query: filter = {} } = searchAtom ? get(searchAtom) : {};

        if (dashlet) {
          const { _domainAction, ...formContext } = getViewContext() ?? {};
          const _domainContext = {
            ...filter._domainContext,
            ...formContext,
          };
          filter = {
            ...filter,
            _domainAction,
            _domainContext,
          };
        }

        const translate = getSearchTranslate(filter);

        return dataStore.search({
          translate,
          sortBy,
          filter,
          fields: names,
          ...options,
        });
      },
      [
        orderBy,
        searchAtom,
        fields,
        dashlet,
        dataStore,
        getSearchTranslate,
        getViewContext,
      ],
    ),
  );

  const onSearch = useAfterActions(doSearch);

  const onRefresh = useCallback(() => doSearch({}), [doSearch]);

  const actionExecutor = useActionExecutor(view, {
    getContext: getActionContext,
    onRefresh,
  });

  const getActionData = useAtomCallback(
    useCallback(
      (get) => {
        const { query: filter = {} } = searchAtom ? get(searchAtom) : {};
        return {
          ...dataStore.options?.filter,
          ...filter,
        };
      },
      [dataStore, searchAtom],
    ),
  );
  const records = useDataStore(dataStore, (ds) => ds.records);
  const Template = useTemplate(view.template!);

  useAsyncEffect(async () => {
    await onSearch();
  }, [onSearch]);

  const onDelete = useCallback(
    async (record: DataRecord) => {
      const confirmed = await dialogs.confirm({
        content: i18n.get(
          "Do you really want to delete the selected record(s)?",
        ),
        yesTitle: i18n.get("Delete"),
      });
      if (confirmed) {
        if (onDeleteAction) {
          await actionExecutor.execute(onDeleteAction, {
            context: record,
          });
        }
        try {
          await dataStore.delete([
            { id: record.id!, version: record.version! },
          ]);
        } catch {
          // Ignore
        }
      }
    },
    [onDeleteAction, actionExecutor, dataStore],
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
    [switchTo],
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
          onSearch: () => onSearch({}),
        });
    },
    [showEditor, view, action, onSearch],
  );

  const onNew = useCallback(() => {
    hasAddPopup ? onEditInPopup({}) : onEdit({});
  }, [hasAddPopup, onEdit, onEditInPopup]);

  const onView = useCallback(
    (record: DataRecord) => {
      hasEditPopup ? onEditInPopup(record, true) : onEdit(record, true);
    },
    [hasEditPopup, onEdit, onEditInPopup],
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
        searchAtom,
        dataStore,
        actionExecutor,
        view,
        onRefresh,
        ...(canNew && canEdit && {
          onAdd: () => onEditInPopup({}),
        }),
      });
    }
  }, [
    dashlet,
    view,
    searchAtom,
    dataStore,
    actionExecutor,
    onRefresh,
    setDashletHandlers,
    canNew,
    canEdit,
    onEditInPopup,
  ]);

  const showToolbar = popupOptions?.showToolbar !== false;

  const {
    offset = 0,
    limit = DEFAULT_PAGE_SIZE,
    totalCount = 0,
  } = dataStore.page;
  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;

  const handlePrev = useCallback(
    () => onSearch({ offset: offset - limit }),
    [limit, offset, onSearch],
  );
  const handleNext = useCallback(
    () => onSearch({ offset: offset + limit }),
    [limit, offset, onSearch],
  );

  useShortcuts({
    viewType: view.type,
    onNew: canNew ? onNew : undefined,
    onRefresh,
  });

  // register tab:refresh
  useViewTabRefresh("cards", onSearch);

  return (
    <Box
      className={legacyClassNames(styles.container, "cards-view", "row-fluid")}
    >
      {showToolbar && (
        <ViewToolBar
          meta={meta}
          dataStore={dataStore}
          getActionData={getActionData}
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
              view={view}
              onView={onView}
              Template={Template}
              width={width}
              minWidth={minWidth}
              onRefresh={onRefresh}
              {...(canEdit && {
                onEdit: hasEditPopup ? onEditInPopup : onEdit,
              })}
              {...(canDelete && { onDelete })}
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
