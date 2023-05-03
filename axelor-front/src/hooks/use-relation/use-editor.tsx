import { useAtomValue } from "jotai";
import { uniqueId } from "lodash";
import { useCallback } from "react";

import { Box, Button } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { showPopup } from "@/view-containers/view-popup";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";

import { initTab } from "../use-tabs";
import { FormView } from "@/services/client/meta.types";

export type EditorOptions = {
  model: string;
  title: string;
  record?: DataRecord | null;
  readonly?: boolean;
  view?: FormView;
  viewName?: string;
  context?: DataContext;
  onClose?: () => void;
  onSave?: (record: DataRecord) => Promise<DataRecord> | void;
  onSelect?: (record: DataRecord) => void;
};

export function useEditor() {
  return useCallback(async (options: EditorOptions) => {
    const {
      title,
      model,
      record,
      view,
      viewName,
      context,
      readonly: forceReadonly,
      onClose,
      onSave,
      onSelect,
    } = options;
    const tab = await initTab({
      name: uniqueId("$selector"),
      title,
      model,
      viewType: "form",
      views: [{ type: "form", name: viewName, ...view }],
      params: {
        popup: true,
        forceReadonly,
        "show-toolbar": false,
        "_popup-record": record,
      },
      context: {
        _showRecord: record?.id,
        ...context,
      },
    });

    if (!tab) return;

    await showPopup({
      tab,
      open: true,
      onClose: () => {
        onClose?.();
      },
      footer: (close) => (
        <Footer
          onSave={onSave}
          onClose={(result: boolean) => {
            close(result);
            onClose?.();
          }}
          onSelect={onSelect}
        />
      ),
      buttons: [],
    });
  }, []);
}

function Footer({
  onClose,
  onSave,
  onSelect,
}: {
  onClose: (result: boolean) => void;
  onSave?: EditorOptions["onSave"];
  onSelect?: EditorOptions["onSelect"];
}) {
  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);

  const handleClose = useCallback(() => {
    dialogs.confirmDirty(
      async () => handler.getState?.().dirty ?? false,
      async () => onClose(false)
    );
  }, [handler, onClose]);

  const handleConfirm = useCallback(async () => {
    if (handler.getState === undefined) return onClose(true);
    const state = handler.getState();
    const record = state.record;
    const canSave = state.dirty || !record.id;
    try {
      if (canSave) {
        if (onSave) {
          onSave(record);
        } else if (onSelect && handler.onSave) {
          const rec = await handler.onSave();
          onSelect(rec);
        }
      }
      onClose(true);
    } catch (e) {
      // TODO: show error
    }
  }, [handler, onClose, onSave, onSelect]);

  return (
    <Box d="flex" g={2}>
      <Button variant="secondary" onClick={handleClose}>
        {i18n.get("Cancel")}
      </Button>
      <Button variant="primary" onClick={handleConfirm}>
        {i18n.get("OK")}
      </Button>
    </Box>
  );
}
