import { clsx, Box, CommandBar, CommandItemProps } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { useAtomCallback } from "jotai/utils";
import { ReactElement, useCallback, useEffect, useRef } from "react";
import { ScopeProvider } from "bunshi/react";

import { useShortcuts } from "@/hooks/use-shortcut";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { ViewData } from "@/services/client/meta";
import { FormView } from "@/services/client/meta.types";
import { MetaScope, useViewDirtyAtom } from "@/view-containers/views/scope";
import { ToolbarActions } from "@/view-containers/view-toolbar";
import { resetFormDummyFieldsState } from "@/views/form/builder/utils";
import { SaveOptions } from "@/services/client/data";
import { useSingleClickHandler } from "@/hooks/use-button";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import {
  FormWidgetProviders,
  FormWidgetsHandler,
} from "@/views/form/builder/form-providers";

import {
  Layout,
  restoreSelectedStateWithSavedRecord,
  showErrors,
  useFormAttachment,
  useFormPerms,
  useGetErrors,
  useHandleFocus,
  usePrepareSaveRecord,
} from "../../form";
import { Form, FormState, useFormHandlers } from "../../form/builder";

import styles from "./details.module.scss";

export interface DetailsProps {
  meta: ViewData<FormView>;
  relatedViewType: string;
  record: DataRecord;
  dirty?: boolean;
  overlay?: boolean;
  onNew?: (options?: { showConfirm?: boolean }) => void;
  onRefresh?: (options?: {
    showConfirm?: boolean;
    select?: Record<string, any>;
  }) => void;
  onCancel?: () => void;
  onSave?: (
    record: DataRecord,
    options?: SaveOptions<DataRecord>,
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
  const { view } = meta;
  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, record);
  const { hasButton, attrs } = useFormPerms(meta.view, meta.perms, {
    recordHandler,
  });

  const widgetHandler = useRef<FormWidgetsHandler | null>(null);
  const resetStatesByName = useRef<FormState["statesByName"] | null>(null);
  const isSaveOnLoad = useRef(false);
  const dirtyAtom = useViewDirtyAtom();

  const { toolbar, menubar, onSave: onSaveAction } = view;
  const isNew = (record?.id ?? -1) < 0;
  const attachmentItem = useFormAttachment(formAtom);

  const getErrors = useGetErrors();
  const prepareRecordForSave = usePrepareSaveRecord(meta, formAtom);

  const setFormReady = useAtomCallback(
    useCallback(
      (get, set) => {
        set(formAtom, (state) => ({ ...state, ready: true }));
      },
      [formAtom],
    ),
  );

  const restoreFormState = useAtomCallback(
    useCallback(
      (get, set) => {
        const statesByName = resetStatesByName.current;
        if (statesByName) {
          resetStatesByName.current = null;
          set(formAtom, (prev) =>
            restoreSelectedStateWithSavedRecord(
              { ...prev, statesByName },
              prev.record,
            ),
          );
        }
      },
      [formAtom],
    ),
  );

  useEffect(() => {
    restoreFormState();
  }, [restoreFormState]);

  const doValidate = useAtomCallback(
    useCallback(
      async (get) => {
        const state = get(formAtom);
        const errors = getErrors(state);
        if (errors || widgetHandler?.current?.invalid?.()) {
          if (errors) {
            showErrors(errors);
          }
          return Promise.reject();
        }
      },
      [getErrors, formAtom],
    ),
  );

  const doSave = useAtomCallback(
    useCallback(
      async (
        get,
        set,
        options: { shouldSave?: boolean; callOnSave?: boolean } = {},
      ) => {
        await doValidate();

        const { shouldSave = true, callOnSave = true } = options;

        if (callOnSave && onSaveAction) {
          await actionExecutor.execute(onSaveAction);
        }

        const state = get(formAtom);

        if (!shouldSave) return state.record;

        const [savingRecord, restoreDummyValues] = prepareRecordForSave();

        resetStatesByName.current = resetFormDummyFieldsState(
          meta,
          get(formAtom).statesByName,
        );

        try {
          await onSave?.(
            savingRecord,
            { select: state.select },
            restoreDummyValues,
          );
          isSaveOnLoad.current = true;
        } catch (err) {
          resetStatesByName.current = null;
        }
      },
      [
        meta,
        formAtom,
        onSaveAction,
        actionExecutor,
        prepareRecordForSave,
        onSave,
        doValidate,
      ],
    ),
  );

  const handleSave = useCallback(async () => {
    await actionExecutor.waitFor();
    await widgetHandler.current?.commit?.();
    return doSave();
  }, [actionExecutor, doSave]);

  const handleSaveClick = useSingleClickHandler(handleSave);

  useAsyncEffect(async () => {
    const { onLoad: _onLoad, onNew: _onNew } = view;
    if (isSaveOnLoad.current) {
      isSaveOnLoad.current = false;
      return;
    }
    if (record) {
      const action = (record?.id ?? 0) > 0 ? _onLoad : _onNew;
      if (action) {
        await actionExecutor.execute(action);
      }
      setFormReady();
    }
  }, [record, view, actionExecutor]);

  const containerRef = useRef<HTMLDivElement>(null);

  const canNew = hasButton("new");
  const canSave = hasButton("save");
  const canEdit = hasButton("edit") && !attrs?.readonly;

  const doRefresh = useAtomCallback(
    useCallback(
      async (get, set, opts?: { showConfirm?: boolean }) => {
        const { select } = get(formAtom);
        await onRefresh?.({ ...opts, select });
      },
      [formAtom, onRefresh],
    ),
  );

  const handleRefresh = isNew ? onNew : doRefresh;

  actionHandler.setRefreshHandler(
    async () => await handleRefresh?.({ showConfirm: false }),
  );

  actionHandler.setSaveHandler(
    useAtomCallback(
      useCallback(
        async (get) => {
          const { record: rec, dirty: formDirty } = get(formAtom);
          const isDirty = get(dirtyAtom) || formDirty;
          const isNewRecord = (rec.id || 0) <= 0;
          await doSave({
            callOnSave: false,
            shouldSave: isDirty || isNewRecord,
          });
        },
        [formAtom, dirtyAtom, doSave],
      ),
    ),
  );
  actionHandler.setValidateHandler(doValidate);

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
                onClick: () => onNew?.(),
              },
              {
                key: "save",
                text: i18n.get("Save"),
                hidden: !canSave || !canEdit,
                iconProps: {
                  icon: "save",
                },
                onClick: handleSaveClick as CommandItemProps["onClick"],
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
                onClick: () => handleRefresh?.(),
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
              <FormWidgetProviders ref={widgetHandler}>
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
              </FormWidgetProviders>
            </ScopeProvider>
          </Box>
        </Box>
      </Box>
    </>
  )) as ReactElement;
}
