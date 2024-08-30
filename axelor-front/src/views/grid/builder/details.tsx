import { clsx, Box, CommandBar } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { useAtomCallback } from "jotai/utils";
import { ReactElement, useCallback, useEffect, useRef } from "react";
import { ScopeProvider } from "bunshi/react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { usePerms } from "@/hooks/use-perms";
import { useShortcuts } from "@/hooks/use-shortcut";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { ViewData } from "@/services/client/meta";
import { FormView } from "@/services/client/meta.types";
import { MetaScope } from "@/view-containers/views/scope";
import { ToolbarActions } from "@/view-containers/view-toolbar";
import {
  Layout,
  showErrors,
  useFormAttachment,
  useGetErrors,
  useHandleFocus,
  usePrepareSaveRecord,
} from "../../form";
import { Form, FormState, useFormHandlers } from "../../form/builder";
import { resetFormDummyFieldsState } from "@/views/form/builder/utils";

import styles from "./details.module.scss";

export interface DetailsProps {
  meta: ViewData<FormView>;
  relatedViewType: string;
  record: DataRecord;
  dirty?: boolean;
  overlay?: boolean;
  onNew?: () => void;
  onRefresh?: () => void;
  onCancel?: () => void;
  onSave?: (
    record: DataRecord,
    restoreDummyValues?: (saved: DataRecord, fetched: DataRecord) => DataRecord,
  ) => Promise<void>;
}

export function Details({
  meta,
  relatedViewType,
  record,
  dirty = false,
  overlay,
  onRefresh,
  onNew,
  onSave,
  onCancel,
}: DetailsProps) {
  const { view, perms } = meta;
  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, record);

  const { hasButton } = usePerms(view, perms);
  const resetStatesByName = useRef<FormState["statesByName"] | null>(null);

  const { toolbar, menubar, onSave: onSaveAction } = view;
  const isNew = (record?.id ?? -1) < 0;
  const attachmentItem = useFormAttachment(formAtom);

  const getErrors = useGetErrors();
  const prepareRecordForSave = usePrepareSaveRecord(meta, formAtom);

  const restoreFormState = useAtomCallback(
    useCallback(
      (get, set) => {
        const statesByName = resetStatesByName.current;
        if (statesByName) {
          resetStatesByName.current = null;
          set(formAtom, (prev) => ({ ...prev, statesByName }));
        }
      },
      [formAtom],
    ),
  );

  useEffect(() => {
    restoreFormState();
  }, [restoreFormState]);

  const handleSave = useAtomCallback(
    useCallback(
      async (get) => {
        const state = get(formAtom);
        const errors = getErrors(state);
        if (errors) {
          showErrors(errors);
          return;
        }

        if (onSaveAction) {
          await actionExecutor.execute(onSaveAction);
        }

        const [savingRecord, restoreDummyValues] = prepareRecordForSave();

        resetStatesByName.current = resetFormDummyFieldsState(
          meta,
          get(formAtom).statesByName,
        );

        try {
          await onSave?.(savingRecord, restoreDummyValues);
        } catch (err) {
          resetStatesByName.current = null;
        }
      },
      [
        meta,
        formAtom,
        onSaveAction,
        actionExecutor,
        getErrors,
        onSave,
        prepareRecordForSave,
      ],
    ),
  );

  useAsyncEffect(async () => {
    const { onLoad: _onLoad, onNew: _onNew } = view;
    if (record) {
      const action = (record?.id ?? 0) > 0 ? _onLoad : _onNew;
      action && (await actionExecutor.execute(action));
    }
  }, [record, view, actionExecutor]);

  const containerRef = useRef<HTMLDivElement>(null);

  const canNew = hasButton("new");
  const canSave = hasButton("save");
  const canEdit = hasButton("edit");

  const handleRefresh = isNew ? onNew : onRefresh;

  useEffect(() => {
    actionHandler.setRefreshHandler(async () => handleRefresh?.());
    actionHandler.setSaveHandler(handleSave);
  }, [actionHandler, handleSave, handleRefresh]);

  useShortcuts({
    viewType: relatedViewType,
    onNew: canNew ? onNew : undefined,
    onSave: canSave ? handleSave : undefined,
    onRefresh: dirty ? handleRefresh : undefined,
    onFocus: useHandleFocus(containerRef),
  });

  return (record && (
    <>
      <Box
        d="flex"
        flexDirection="column"
        flex={1}
        border
        borderBottom={false}
        className={styles.container}
      >
        <Box d="flex" w={100} bg="body" borderBottom>
          <CommandBar
            items={[
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
                key: "save",
                text: i18n.get("Save"),
                hidden: !canSave || !canEdit,
                iconProps: {
                  icon: "save",
                },
                onClick: handleSave,
              },
              {
                key: "back",
                text: i18n.get("Back"),
                iconProps: {
                  icon: "undo",
                },
                hidden: overlay,
                onClick: onCancel,
              },
              {
                key: "refresh",
                text: i18n.get("Refresh"),
                iconProps: {
                  icon: "refresh",
                },
                onClick: handleRefresh,
                disabled: !dirty,
              },
              {
                ...attachmentItem,
                disabled: isNew,
              },
            ]}
            iconOnly
          />
          <Box d="flex" className={styles.actions}>
            {(toolbar || menubar) && (
              <ToolbarActions
                buttons={toolbar}
                menus={menubar}
                actionExecutor={actionExecutor}
              />
            )}
          </Box>
          <Box
            flex={1}
            d="flex"
            alignItems="center"
            justifyContent="flex-end"
            className={styles.close}
          >
            <Box d="flex" p={2} onClick={() => onCancel?.()}>
              <MaterialIcon icon="close" />
            </Box>
          </Box>
        </Box>
        <Box
          d="flex"
          flex={1}
          className={clsx(styles["form-container"], {
            [styles.overlay]: overlay,
          })}
        >
          <Box
            d="flex"
            flex={1}
            m={3}
            bg="body"
            className={styles.form}
            ref={containerRef}
          >
            <ScopeProvider scope={MetaScope} value={meta}>
              <Form
                schema={meta.view}
                fields={meta.fields!}
                readonly={!canEdit}
                formAtom={formAtom}
                actionHandler={actionHandler}
                actionExecutor={actionExecutor}
                recordHandler={recordHandler}
                layout={Layout}
              />
            </ScopeProvider>
          </Box>
        </Box>
      </Box>
    </>
  )) as ReactElement;
}
