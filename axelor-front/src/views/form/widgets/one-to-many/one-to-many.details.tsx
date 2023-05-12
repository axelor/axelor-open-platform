import { ReactElement, useCallback, useEffect, useMemo, useState } from "react";
import { Box, Button } from "@axelor/ui";
import { useAtomCallback } from "jotai/utils";

import { useAsync } from "@/hooks/use-async";
import { DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { findView } from "@/services/client/meta-cache";
import { FormView } from "@/services/client/meta.types";
import { Form, useFormHandlers } from "../../builder";
import { Layout, showErrors, useGetErrors } from "../../form";
import { i18n } from "@/services/client/i18n";
import styles from "./one-to-many.details.module.scss";
import { nextId } from "../../builder/utils";

interface DetailsFormProps {
  meta: ViewData<FormView>;
  record: DataRecord;
  readonly?: boolean;
  onSave: (data: DataRecord) => void;
}

export function DetailsFormView({
  model,
  name,
  ...props
}: Omit<DetailsFormProps, "meta"> & {
  name?: string;
  model?: string;
}) {
  const { data: meta } = useAsync(
    async () =>
      await findView<FormView>({
        type: "form",
        name,
        model,
      }),
    [model, name]
  );

  return (meta?.view && <DetailsForm meta={meta} {...props} />) as ReactElement;
}

export function DetailsForm({
  meta,
  record,
  readonly,
  onSave,
}: DetailsFormProps) {
  const [recordId, setRecordId] = useState<null | number>();
  const $record = useMemo<DataRecord>(
    () => ({ id: recordId, ...(recordId === record?.id ? record : {}) }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [recordId]
  );
  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, $record);

  const getErrors = useGetErrors();
  const isNew = ($record.id ?? 0) < 0 && !$record?._dirty;

  const handleNew = useCallback(() => {
    setRecordId(nextId());
  }, []);

  const handleClose = useCallback(() => {
    setRecordId(null);
  }, []);

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
        saveAndNew ? handleNew() : handleClose();
      },
      [formAtom, onSave, getErrors, handleNew, handleClose]
    )
  );

  useEffect(() => {
    setRecordId(record?.id);
  }, [record?.id]);

  return (
    recordId ? (
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
            <Button size="sm" variant="danger" onClick={handleClose}>
              {i18n.get("Close")}
            </Button>
          ) : (
            <>
              <Button size="sm" variant="danger" onClick={handleClose}>
                {i18n.get("Cancel")}
              </Button>
              <Button size="sm" variant="primary" onClick={() => handleSave()}>
                {isNew ? i18n.get("Add") : i18n.get("Update")}
              </Button>
              {isNew && (
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
      !readonly && (
        <Box d="flex" justifyContent="flex-end">
          <Button size="sm" variant="primary" onClick={handleNew}>
            {i18n.get("New")}
          </Button>
        </Box>
      )
    )
  ) as ReactElement;
}
