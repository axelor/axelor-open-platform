import { ReactElement, useCallback } from "react";
import { useAtomCallback } from "jotai/utils";
import { Button, Box, CommandBar } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import clsx from "clsx";

import { DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView } from "@/services/client/meta.types";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { Form, useFormHandlers } from "../../form/builder";
import {
  Layout,
  showErrors,
  useFormAttachment,
  useGetErrors,
} from "../../form";
import { i18n } from "@/services/client/i18n";
import styles from "./details.module.scss";

export interface DetailsProps {
  meta: ViewData<FormView>;
  record: DataRecord;
  dirty?: boolean;
  overlay?: boolean;
  onNew?: () => void;
  onRefresh?: () => void;
  onCancel?: () => void;
  onSave?: (record: DataRecord) => Promise<any>;
}

export function Details({
  meta,
  record,
  dirty = false,
  overlay,
  onRefresh,
  onNew,
  onSave,
  onCancel,
}: DetailsProps) {
  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, record);

  const isNew = (record?.id ?? -1) < 0;
  const attachmentItem = useFormAttachment(formAtom);

  const getErrors = useGetErrors();

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

  useAsyncEffect(async () => {
    const { onLoad, onNew } = meta.view;
    if (record) {
      const action = (record?.id ?? 0) > 0 ? onLoad : onNew;
      action && (await actionExecutor.execute(action));
    }
  }, [record, meta.view, actionExecutor]);

  return (record && (
    <>
      <Box
        d="flex"
        flexDirection="column"
        flex={1}
        className={styles.container}
      >
        <Box d="flex" w={100} bg="body" borderBottom>
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
                hidden: overlay,
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
                ...attachmentItem,
                disabled: isNew,
              },
            ]}
            iconOnly
          />
          {overlay && (
            <Box
              flex={1}
              d="flex"
              alignItems="center"
              justifyContent="flex-end"
              px={2}
              className={styles.close}
            >
              <Button d="flex" p={0}>
                <MaterialIcon icon="close" onClick={() => onCancel?.()} />
              </Button>
            </Box>
          )}
        </Box>
        <Box
          d="flex"
          flex={1}
          className={clsx(styles["form-container"], {
            [styles.overlay]: overlay,
          })}
        >
          <Box d="flex" flex={1} m={3} bg="body" className={styles.form}>
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
      </Box>
    </>
  )) as ReactElement;
}
