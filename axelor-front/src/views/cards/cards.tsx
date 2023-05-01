import { useCallback, useEffect } from "react";
import { useSetAtom } from "jotai";
import { Box } from "@axelor/ui";
import { useAtomCallback } from "jotai/utils";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { usePerms } from "@/hooks/use-perms";
import { useTemplate } from "@/hooks/use-parser";
import { legacyClassNames } from "@/styles/legacy";
import { CardsView } from "@/services/client/meta.types";
import { useDataStore } from "@/hooks/use-data-store";
import { useViewSwitch, useViewTab } from "@/view-containers/views/scope";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import { i18n } from "@/services/client/i18n";
import { SearchOptions } from "@/services/client/data";
import { PageText } from "@/components/page-text";
import { DataRecord } from "@/services/client/data.types";
import { dialogs } from "@/components/dialogs";
import { AdvanceSearch } from "@/view-containers/advance-search";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { ViewProps } from "../types";
import { Card } from "./card";
import classes from "./cards.module.scss";

export function Cards(props: ViewProps<CardsView>) {
  const { meta, dataStore, searchAtom, domains } = props;
  const { view, fields } = meta;
  const { dashlet, popup, popupOptions } = useViewTab();
  const { hasButton } = usePerms(meta.view, meta.perms);
  const switchTo = useViewSwitch();

  const onSearch = useAtomCallback(
    useCallback(
      (get, set, options: Partial<SearchOptions> = {}) => {
        const { query = {} } = searchAtom ? get(searchAtom) : {};
        const { archived: _archived } = query;
        return dataStore.search({
          filter: { ...query, _archived },
          ...options,
        });
      },
      [dataStore, searchAtom]
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
        } catch {}
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

  const onNew = useCallback(() => {
    onEdit({});
  }, [onEdit]);

  const onView = useCallback(
    (record: DataRecord) => {
      onEdit(record, true);
    },
    [onEdit]
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

  return (
    <Box className={legacyClassNames(classes.cards, "cards-view", "row-fluid")}>
      {showToolbar && (
        <ViewToolBar
          meta={meta}
          actions={[
            {
              key: "new",
              text: i18n.get("New"),
              hidden: !hasButton("new"),
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
            onPrev: () => onSearch({ offset: offset - limit }),
            onNext: () => onSearch({ offset: offset + limit }),
            text: () => <PageText dataStore={dataStore} />,
          }}
        >
          {searchAtom && (
            <AdvanceSearch
              stateAtom={searchAtom}
              dataStore={dataStore}
              items={view.items}
              fields={fields}
              domains={domains}
              onSearch={onSearch}
            />
          )}
        </ViewToolBar>
      )}
      <Box p={2} py={3} d="flex" flexWrap="wrap" overflow="auto">
        {records.map((record) => (
          <Card
            key={record.id}
            model={view.model}
            record={record}
            fields={fields}
            onView={onView}
            Template={Template}
            {...(hasButton("edit") && { onEdit })}
            {...(hasButton("delete") && { onDelete })}
          />
        ))}
      </Box>
    </Box>
  );
}
