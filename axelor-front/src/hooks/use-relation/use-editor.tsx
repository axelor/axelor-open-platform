import { useAtomValue } from "jotai";
import { uniqueId } from "lodash";
import { useCallback } from "react";

import { Box, Button } from "@axelor/ui";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { FormView, Schema } from "@/services/client/meta.types";
import { dialogs } from "@/components/dialogs";
import { i18n } from "@/services/client/i18n";
import { showPopup } from "@/view-containers/view-popup";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { useViewTab } from "@/view-containers/views/scope";
import { openTab_internal as openTab } from "@/hooks/use-tabs";
import { findView } from "@/services/client/meta-cache";
import { showErrors, useGetErrors } from "@/views/form";

import { initTab } from "../use-tabs";

export type EditorOptions = {
  model: string;
  title: string;
  record?: DataRecord | null;
  readonly?: boolean;
  maximize?: boolean;
  view?: FormView;
  viewName?: string;
  context?: DataContext;
  onClose?: () => void;
  onSave?: (record: DataRecord) => Promise<DataRecord> | void;
  onSelect?: (record: DataRecord) => void;
};

export function useEditorInTab(schema: Schema) {
  const {
    target,
    formView,
    gridView,
    widgetAttrs,
    widget,
    editWindow = widgetAttrs.editWindow || "popup",
  } = schema;
  const tab = useViewTab();

  const handleEdit = useCallback(
    async (record: DataRecord, readonly = false) => {
      const model = target;
      const { view } = await findView<FormView>({
        type: "form",
        name: formView,
        model,
      });
      return openTab({
        title: view?.title || "",
        name: uniqueId("$act"),
        model,
        viewType: "form",
        views: [
          { name: formView, type: "form" },
          {
            type: "grid",
            name: gridView,
          },
        ],
        params: {
          forceEdit: !readonly,
        },
        context: {
          _showRecord: record.id,
          __check_version: tab.action?.context?.__check_version,
        },
      });
    },
    [formView, gridView, target, tab.action]
  );

  if (!tab.popup && (editWindow === "blank" || widget === "ref-link")) {
    return handleEdit;
  }

  return null;
}

export function useEditor() {
  return useCallback(async (options: EditorOptions) => {
    const {
      title,
      model,
      record,
      view,
      viewName,
      context,
      readonly,
      maximize,
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
        forceEdit: !readonly,
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
      maximize,
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

  const getErrors = useGetErrors();

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
      const errors = getErrors(state);
      if (errors) {
        showErrors(errors);
        return;
      }

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
  }, [getErrors, handler, onClose, onSave, onSelect]);

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
