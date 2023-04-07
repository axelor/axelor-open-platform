import { useAtomValue } from "jotai";
import { uniqueId } from "lodash";
import { useCallback } from "react";

import { Box, Button } from "@axelor/ui";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { showPopup } from "@/view-containers/view-popup";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";

import { useAsyncEffect } from "../use-async-effect";
import { initTab } from "../use-tabs";

export type EditorOptions = {
  model: string;
  title: string;
  record?: DataRecord | null;
  viewName?: string;
  context?: DataContext;
  onSelect?: (record: DataRecord) => void;
};

export function useEditor() {
  return useCallback(async (options: EditorOptions) => {
    const { title, model, record, viewName, context, onSelect } = options;
    const tab = await initTab({
      name: uniqueId("$selector"),
      title,
      model,
      viewType: "form",
      views: [{ type: "form", name: viewName }],
      params: {
        popup: true,
        "show-toolbar": false,
      },
      context,
    });

    if (!tab) return;

    const close = await showPopup({
      tab,
      open: true,
      onClose: () => {},
      handler: () => <Handler record={record} />,
      footer: () => <Footer onClose={() => close()} onSelect={onSelect} />,
      buttons: [],
    });
  }, []);
}

function Handler({ record }: { record?: DataRecord | null }) {
  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);

  const { onEdit, onRead } = handler;

  useAsyncEffect(async () => {
    if (!onRead || !onEdit) return;
    let rec = record ?? {};
    if (rec.id && +rec.id > 0) rec = await onRead(rec.id);
    onEdit(rec);
  }, [record]);

  return null;
}

function Footer({
  onClose,
  onSelect,
}: {
  onClose: () => void;
  onSelect?: (record: DataRecord) => void;
}) {
  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);

  const handleClose = useCallback(() => {
    onClose();
  }, [onClose]);

  const handleConfirm = useCallback(async () => {
    const { onSave } = handler;
    if (onSave) {
      const rec = await onSave();
      onSelect?.(rec);
    }
    onClose();
  }, [handler, onClose, onSelect]);

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
