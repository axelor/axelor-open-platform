import { Box } from "@axelor/ui";
import { useRef } from "react";
import { ScopeProvider } from "bunshi/react";
import { useAtomValue } from "jotai";

import { FormView } from "@/services/client/meta.types";
import { MetaScope } from "@/view-containers/views/scope";
import { ViewData } from "@/services/client/meta";
import { Form, FormAtom, useFormHandlers } from "../form/builder";
import { DataRecord } from "@/services/client/data.types";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import styles from "./dashboard-search.module.scss";

function RefreshOnPanelChange({
  formAtom,
  onRefresh,
}: {
  formAtom: FormAtom;
  onRefresh: (values: DataRecord) => void;
}) {
  const { record } = useAtomValue(formAtom);

  useAsyncEffect(async () => {
    record && Object.keys(record).length > 0 && onRefresh(record);
  }, [record, onRefresh]);

  return null;
}

export function DashboardSearch({
  meta,
  onInit,
  onInitCompleted,
  onChange,
}: {
  meta: ViewData<FormView>;
  onChange: (values: DataRecord) => void;
  onInit?: string;
  onInitCompleted?: () => void;
}) {
  const formRef = useRef<HTMLDivElement>(null);

  const record = useRef<DataRecord>({}).current;
  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, record);

  useAsyncEffect(async () => {
    if (onInit) {
      await actionExecutor.execute(onInit);
      onInitCompleted?.();
    }
  }, [actionExecutor, onInit, onInitCompleted]);

  return (
    <ScopeProvider scope={MetaScope} value={meta}>
      <Box ref={formRef} d="flex" flex={1} className={styles.form}>
        <RefreshOnPanelChange formAtom={formAtom} onRefresh={onChange} />
        <Form
          schema={meta.view}
          fields={meta.fields!}
          readonly={false}
          formAtom={formAtom}
          actionHandler={actionHandler}
          actionExecutor={actionExecutor}
          recordHandler={recordHandler}
        />
      </Box>
    </ScopeProvider>
  );
}
