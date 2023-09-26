import { uniqueId } from "lodash";
import { useCallback, useEffect, useMemo, useRef } from "react";

import { Box, Button, CommandBar, CommandItemProps } from "@axelor/ui";
import { GridState } from "@axelor/ui/grid";

import { PageText } from "@/components/page-text";
import { DataStore } from "@/services/client/data-store";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { showPopup } from "@/view-containers/view-popup";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { findView } from "@/services/client/meta-cache";

import { i18n } from "@/services/client/i18n";
import { useAtomValue } from "jotai";
import { useDataStore } from "../use-data-store";
import { initTab } from "../use-tabs";

export type SelectorOptions = {
  model: string;
  title?: string;
  multiple?: boolean;
  viewName?: string;
  orderBy?: string;
  domain?: string;
  context?: DataContext;
  limit?: number;
  onClose?: () => void;
  onCreate?: () => void;
  onSelect?: (records: DataRecord[]) => void;
};

export function useSelector() {
  return useCallback(async function showSelector(options: SelectorOptions) {
    const {
      title,
      model,
      viewName,
      orderBy,
      multiple,
      domain,
      context,
      limit,
      onClose,
      onCreate,
      onSelect,
    } = options;

    const { view } = await findView({
      type: "grid",
      name: viewName,
      model,
    });

    const tab = await initTab({
      name: uniqueId("$selector"),
      title: title || view?.title || "",
      model,
      viewType: "grid",
      views: [{ type: "grid", name: viewName }],
      params: {
        limit,
        popup: true,
        orderBy,
        "show-toolbar": false,
        "_popup-edit-icon": false,
        "_popup-multi-select": multiple,
      },
      domain,
      context,
    });

    if (!tab) return;

    await showPopup({
      tab,
      open: true,
      onClose: () => {
        onClose?.();
      },
      header: () => <Header />,
      footer: (close) => (
        <Footer
          multiple={multiple}
          close={close}
          onClose={onClose}
          onCreate={onCreate}
          onSelect={onSelect}
        />
      ),
      buttons: [],
    });
  }, []);
}

function Handler({
  multiple,
  onClose,
  onSelect,
}: {
  multiple?: boolean;
  onClose: (result: boolean) => void;
  onSelect?: (records: DataRecord[]) => void;
}) {
  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);

  const onSelectionChange = useCallback(
    (index: number, records: DataRecord[]) => {
      if (index === 0 && multiple) return;
      onSelect?.(records);
      onClose(true);
    },
    [multiple, onClose, onSelect],
  );

  useEffect(() => {
    const state = handler.data as GridState;
    const index = state?.selectedCell?.[1] ?? 0;
    const records =
      state?.selectedRows?.map((index) => state.rows[index].record) ?? [];
    if (records.length) {
      onSelectionChange(index, records);
    }
  }, [handler, onSelectionChange]);

  return null;
}

function Header() {
  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);
  if (handler.dataStore) {
    return <SelectorHeader dataStore={handler.dataStore} />;
  }
  return null;
}

function SelectorHeader({ dataStore }: { dataStore: DataStore }) {
  const page = useDataStore(dataStore, (state) => state.page);
  const { offset = 0, limit = 0, totalCount = 0 } = page;

  const onNext = useCallback(() => {
    const nextOffset = Math.min(offset + limit, totalCount);
    dataStore.search({ offset: nextOffset });
  }, [dataStore, limit, offset, totalCount]);

  const onPrev = useCallback(() => {
    const nextOffset = Math.max(offset - limit, 0);
    dataStore.search({ offset: nextOffset });
  }, [dataStore, limit, offset]);

  const commands = useMemo(() => {
    const items: CommandItemProps[] = [
      {
        key: "prev",
        iconOnly: true,
        iconProps: {
          icon: "navigate_before",
        },
        disabled: offset === 0,
        onClick: onPrev,
      },
      {
        key: "next",
        iconOnly: true,
        iconProps: {
          icon: "navigate_next",
        },
        disabled: offset + limit >= totalCount,
        onClick: onNext,
      },
    ];
    return items;
  }, [limit, offset, onNext, onPrev, totalCount]);

  return (
    <Box d="flex" alignItems="center" g={2}>
      <PageText dataStore={dataStore} />
      <CommandBar items={commands} />
    </Box>
  );
}

function Footer({
  multiple = false,
  close,
  onClose: _onClose,
  onCreate,
  onSelect,
}: {
  multiple?: boolean;
  close: (result: boolean) => void;
  onClose?: () => void;
  onCreate?: () => void;
  onSelect?: (records: DataRecord[]) => void;
}) {
  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);

  const onClose = useCallback(
    (result: boolean) => {
      close(result);
      _onClose?.();
    },
    [close, _onClose],
  );

  const handleCancel = useCallback(() => {
    onClose(false);
  }, [onClose]);

  const handleConfirm = useCallback(async () => {
    const state = handler.data as GridState;
    const records =
      state?.selectedRows?.map((index) => state.rows[index].record) ?? [];
    onSelect?.(records);
    onClose(true);
  }, [handler, onSelect, onClose]);

  return (
    <Box d="flex" g={2}>
      <Handler multiple={multiple} onClose={onClose} onSelect={onSelect} />
      <Box d="flex" {...(onCreate && { flex: 1 })}>
        {onCreate && (
          <Box d="flex" flex={1}>
            <Button
              variant="light"
              onClick={() => {
                onClose(false);
                onCreate();
              }}
            >
              {i18n.get("Create")}
            </Button>
          </Box>
        )}
      </Box>
      <Box d="flex" g={2}>
        <Button variant="secondary" onClick={handleCancel}>
          {i18n.get("Close")}
        </Button>
        <Button variant="primary" onClick={handleConfirm}>
          {i18n.get("OK")}
        </Button>
      </Box>
    </Box>
  );
}
