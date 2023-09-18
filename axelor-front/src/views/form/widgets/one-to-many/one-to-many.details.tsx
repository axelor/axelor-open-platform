import { ReactElement, useCallback, useEffect, useMemo } from "react";
import { Box, Button } from "@axelor/ui";
import { useAtomCallback } from "jotai/utils";
import { useSetAtom } from "jotai";
import { focusAtom } from "jotai-optics";

import { DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView } from "@/services/client/meta.types";
import { i18n } from "@/services/client/i18n";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { Form, FormAtom, useFormHandlers } from "../../builder";
import { Layout, showErrors, useGetErrors } from "../../form";
import styles from "./one-to-many.details.module.scss";

interface DetailsFormProps {
  meta: ViewData<FormView>;
  record: DataRecord | null;
  readonly?: boolean;
  formAtom?: FormAtom;
  onSave: (data: DataRecord) => void;
  onNew?: () => void;
  onClose?: () => void;
}

const defaultRecord: DataRecord = {};

export function DetailsForm({
  meta,
  formAtom: detailFormAtom,
  readonly,
  record,
  onNew,
  onClose,
  onSave,
}: DetailsFormProps) {
  const {
    formAtom: _formAtom,
    actionHandler,
    actionExecutor,
    recordHandler,
  } = useFormHandlers(meta, record ?? defaultRecord);

  const formAtom = detailFormAtom ?? _formAtom;
  const getErrors = useGetErrors();
  const isNew = (record?.id ?? 0) < 0 && !record?._dirty;

  const setReady = useSetAtom(
    useMemo(() => focusAtom(formAtom, (o) => o.prop("ready")), [formAtom]),
  );

  const handleSave = useAtomCallback(
    useCallback(
      async (get, set, saveAndNew?: boolean) => {
        const state = get(formAtom);
        const { record } = state;
        const errors = getErrors(state);
        if (errors) {
          showErrors(errors);
          return;
        }
        onSave(record);
        saveAndNew && onNew ? onNew() : onClose?.();
      },
      [formAtom, onSave, getErrors, onNew, onClose],
    ),
  );

  const resetFormAtom = useAtomCallback(
    useCallback(
      (get, set, formAtom: FormAtom) => {
        const { record } = get(formAtom);
        const state = get(_formAtom);
        state.record?.id === record.id &&
          set(_formAtom, {
            ...get(_formAtom),
            record: { ...state.record, ...record },
          });
      },
      [_formAtom],
    ),
  );

  useAsyncEffect(async () => {
    const { onLoad, onNew } = meta.view;
    if (record) {
      const action = (record?.id ?? 0) > 0 ? onLoad : onNew;
      action && (await actionExecutor.execute(action));
    }
  }, [record, meta.view, actionExecutor]);

  useEffect(() => {
    if (detailFormAtom) {
      return () => resetFormAtom(detailFormAtom);
    }
  }, [resetFormAtom, detailFormAtom]);

  useEffect(() => {
    setReady(true);
  }, [setReady]);

  return (
    record ? (
      <>
        <Box d="flex" flex={1} className={styles.container}>
          <Form
            schema={meta.view}
            fields={meta.fields!}
            readonly={readonly}
            formAtom={formAtom}
            actionHandler={actionHandler}
            actionExecutor={actionExecutor}
            recordHandler={recordHandler}
            layout={Layout}
            {...({} as any)}
          />
        </Box>
        <Box d="flex" gap={4} justifyContent="flex-end" mt={3}>
          {readonly ? (
            <Button size="sm" variant="light" onClick={() => onClose?.()}>
              {i18n.get("Close")}
            </Button>
          ) : (
            <>
              <Button size="sm" variant="danger" onClick={() => onClose?.()}>
                {i18n.get("Cancel")}
              </Button>
              <Button size="sm" variant="primary" onClick={() => handleSave()}>
                {isNew ? i18n.get("Add") : i18n.get("Update")}
              </Button>
              {isNew && onNew && (
                <Button
                  size="sm"
                  variant="primary"
                  onClick={() => handleSave(true)}
                >
                  {i18n.get("Add and new")}
                </Button>
              )}
            </>
          )}
        </Box>
      </>
    ) : (
      onNew &&
      !readonly && (
        <Box d="flex" justifyContent="flex-end">
          <Button size="sm" variant="primary" onClick={() => onNew()}>
            {i18n.get("New")}
          </Button>
        </Box>
      )
    )
  ) as ReactElement;
}
