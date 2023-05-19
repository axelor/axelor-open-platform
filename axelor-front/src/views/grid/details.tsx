import { ReactElement, useCallback, useMemo } from "react";
import { useAtomCallback } from "jotai/utils";
import { useAtom } from "jotai";
import { focusAtom } from "jotai-optics";
import { Box, CommandBar } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView } from "@/services/client/meta.types";
import { Form, useFormHandlers } from "../form/builder";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useDMSPopup } from "../dms/builder/hooks";
import { Layout, showErrors, useGetErrors } from "../form";
import { i18n } from "@/services/client/i18n";
import styles from "./details.module.scss";

export interface DetailsProps {
  meta: ViewData<FormView>;
  record: DataRecord;
  dirty?: boolean;
  onNew?: () => void;
  onRefresh?: () => void;
  onCancel?: () => void;
  onSave?: (record: DataRecord) => Promise<any>;
}

export function Details({
  meta,
  record,
  dirty = false,
  onRefresh,
  onNew,
  onSave,
  onCancel,
}: DetailsProps) {
  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, record);

  const isNew = (record?.id ?? -1) < 0;
  const [$attachments, setAttachmentCount] = useAtom(
    useMemo(
      () =>
        focusAtom(formAtom, (form) => form.prop("record").prop("$attachments")),
      [formAtom]
    )
  );

  const getErrors = useGetErrors();
  const showDMSPopup = useDMSPopup();

  const handleSave = useAtomCallback(
    useCallback(
      async (get) => {
        const state = get(formAtom);
        const { record } = state;
        const errors = getErrors(state);
        if (errors) {
          showErrors(errors);
          return;
        }
        onSave?.(record);
      },
      [formAtom, getErrors, onSave]
    )
  );

  const handleAttachment = useAtomCallback(
    useCallback(
      (get) => {
        const { record, model, fields } = get(formAtom);
        return showDMSPopup({
          record,
          model,
          fields,
          onCountChanged: (totalCount) => setAttachmentCount(totalCount),
        });
      },
      [formAtom, showDMSPopup, setAttachmentCount]
    )
  );

  useAsyncEffect(async () => {
    const { onLoad, onNew } = meta.view;
    if (record) {
      const action = (record?.id ?? 0) > 0 ? onLoad : onNew;
      action && (await actionExecutor.execute(action));
    }
  }, [record, meta.view, actionExecutor]);

  return (record && (
    <>
      <Box d="flex" flexDirection="column" flex={1} className={styles.container}>
        <Box flex={1} bg="body">
          <CommandBar
            items={[
              {
                key: "new",
                text: i18n.get("New"),
                iconProps: {
                  icon: "add",
                },
                onClick: onNew,
              },
              {
                key: "save",
                text: i18n.get("Save"),
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
                onClick: onCancel,
              },
              {
                key: "refresh",
                text: i18n.get("Refresh"),
                iconProps: {
                  icon: "refresh",
                },
                onClick: isNew ? onNew : onRefresh,
                disabled: !dirty,
              },
              {
                key: "attachment",
                text: i18n.get("Attachment"),
                icon: (props: any) => (
                  <Box as="span" d="flex" position="relative">
                    <MaterialIcon icon="attach_file" {...props} />
                    {$attachments ? (
                      <Box d="flex" as="small" alignItems="flex-end">
                        {$attachments}
                      </Box>
                    ) : null}
                  </Box>
                ),
                iconProps: {
                  icon: "attach_file",
                },
                disabled: isNew,
                onClick: handleAttachment,
              },
            ]}
            iconOnly
          />
        </Box>
        <Box d="flex" m={3} bg="body" className={styles.form}>
          <Form
            schema={meta.view}
            fields={meta.fields!}
            readonly={false}
            formAtom={formAtom}
            actionHandler={actionHandler}
            actionExecutor={actionExecutor}
            recordHandler={recordHandler}
            layout={Layout}
            {...({} as any)}
          />
        </Box>
      </Box>
    </>
  )) as ReactElement;
}
