import clsx from "clsx";
import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { useMemo } from "react";

import { Box, InputFeedback, InputLabel } from "@axelor/ui";

import { Tooltip } from "@/components/tooltip";
import { useAsync } from "@/hooks/use-async";
import { useTemplate } from "@/hooks/use-parser";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { Schema, Tooltip as TooltipType } from "@/services/client/meta.types";
import { session } from "@/services/client/session";
import format from "@/utils/format";

import { useFormField, useFormScope } from "./scope";
import { FieldProps, ValueAtom, WidgetProps } from "./types";

import styles from "./form-field.module.css";

export type WidgetControlProps = WidgetProps & {
  className?: string;
  children: React.ReactNode;
};

export function WidgetControl({ className, children }: WidgetControlProps) {
  return <Box className={clsx(className, styles.container)}>{children}</Box>;
}

export type FieldControlProps<T> = FieldProps<T> & {
  className?: string;
  showTitle?: boolean;
  titleActions?: React.ReactNode;
  children: React.ReactNode;
};

export function FieldControl({
  schema,
  className,
  showTitle,
  formAtom,
  widgetAtom,
  valueAtom,
  titleActions,
  children,
}: FieldControlProps<any>) {
  const canShowTitle =
    showTitle ?? schema.showTitle ?? schema.widgetAttrs?.showTitle ?? true;

  function render() {
    return <Box className={styles.content}>{children}</Box>;
  }

  return (
    <Box className={clsx(className, styles.container)}>
      {(canShowTitle || titleActions) && (
        <Box className={styles.title}>
          {canShowTitle && (
            <FieldLabel
              schema={schema}
              formAtom={formAtom}
              widgetAtom={widgetAtom}
            />
          )}
          {titleActions && <Box className={styles.actions}>{titleActions}</Box>}
        </Box>
      )}
      {schema.tooltip && children ? (
        <Tooltip
          content={() => (
            <FieldTooltipContent schema={schema} valueAtom={valueAtom} />
          )}
        >
          {render()}
        </Tooltip>
      ) : (
        render()
      )}
      <FieldError widgetAtom={widgetAtom} />
    </Box>
  );
}

export function FieldError({ widgetAtom }: Pick<WidgetProps, "widgetAtom">) {
  const error = useAtomValue(
    useMemo(
      () => selectAtom(widgetAtom, (state) => state.errors?.error),
      [widgetAtom]
    )
  );
  return error && <InputFeedback invalid>{error}</InputFeedback>;
}

export function FieldLabel({
  schema,
  formAtom,
  widgetAtom,
  className,
}: WidgetProps & { className?: string }) {
  const { uid, help } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;

  return (
    <HelpPopover schema={schema} formAtom={formAtom} widgetAtom={widgetAtom}>
      <InputLabel
        htmlFor={uid}
        className={clsx(className, styles.label, { [styles.help]: help })}
      >
        <span className={styles.labelText}>{title}</span>
      </InputLabel>
    </HelpPopover>
  );
}

export function FieldDetails({
  fetch = true,
  model,
  record,
  data,
  $getField,
}: {
  fetch?: boolean;
  model?: string;
  record?: DataRecord;
  data: TooltipType;
  $getField?: (fieldName: string) => Schema;
}) {
  const { depends, template } = data;
  const Template = useTemplate(template!);
  const { data: context } = useAsync(async () => {
    let values = { ...record };
    if (fetch && model && record?.id) {
      const ds = new DataStore(model);
      const newValues = await ds.read(+record.id, {
        fields: (depends || "")?.split?.(",").map((f) => f.trim()),
      });
      values = { ...values, ...newValues };
    }
    return { ...values, record: values };
  }, [fetch, model, record]);

  return (
    context && (
      <Box>
        <Template
          context={context}
          options={{
            helpers: {
              $getField,
            },
          }}
        />
      </Box>
    )
  );
}

export function HelpPopover({
  schema,
  formAtom,
  widgetAtom,
  children,
  content,
}: WidgetProps & {
  children: React.ReactElement;
  content?: () => JSX.Element | null;
}) {
  const { name, help } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;

  const technical = session.info?.user?.technical && name;
  const canShowHelp = help || technical;

  if (canShowHelp) {
    return (
      <Tooltip
        title={title}
        content={
          content ??
          (() => (
            <HelpContent
              schema={schema}
              formAtom={formAtom}
              widgetAtom={widgetAtom}
            />
          ))
        }
      >
        {children}
      </Tooltip>
    );
  }

  return children;
}

function HelpContent(props: WidgetProps) {
  const { schema, formAtom, widgetAtom } = props;
  const { name, serverType, type, target, help, widget } = schema;
  const { model, original } = useAtomValue(formAtom);
  const { attrs } = useAtomValue(widgetAtom);
  const { domain } = attrs;

  const technical = session.info?.user?.technical && name;

  const value = name && original ? original[name] : undefined;
  let text = format(value, { props: schema as any });
  const shouldDisplayValue =
    serverType &&
    !["TEXT", "BINARY"].includes(serverType) &&
    widget !== "password";

  if (serverType?.endsWith("_ONE") && value) {
    text = `(${value.id}, ${text})`;
  }
  if (serverType === "STRING" && text) {
    if (text.length > 50) {
      text = text.slice(0, 50) + "...";
    }
  }
  if (value && ["ONE_TO_MANY", "MANY_TO_MANY"].includes(serverType)) {
    const length = value.length;
    const items = value
      .slice(0, length > 5 ? 5 : length)
      .map((item: any) => item.id);
    if (length > 5) {
      items.push("...");
    }
    text = items.join(", ");
  }

  return (
    <Box className={styles.tooltip}>
      {help && <Box className={styles.help}>{help}</Box>}
      {help && technical && <hr />}
      {technical && (
        <dl className={styles.details}>
          {model && (
            <>
              <dt>{i18n.get("Object")}</dt>
              <dd>
                <code>{model}</code>
              </dd>
            </>
          )}
          <dt>{i18n.get("Field name")}</dt>
          <dd>
            <code>{name}</code>
          </dd>
          <dt>{i18n.get("Field type")}</dt>
          <dd>
            <code>{serverType || type}</code>
          </dd>
          {target && (
            <>
              <dt>{i18n.get("Reference")}</dt>
              <dd>
                <code>{target}</code>
              </dd>
            </>
          )}
          {domain && (
            <>
              <dt>{i18n.get("Filter")}</dt>
              <dd>
                <code>{domain}</code>
              </dd>
            </>
          )}
          {shouldDisplayValue && (
            <>
              <dt>{i18n.get("Orig. value")}</dt>
              <dd>
                <code>{text}</code>
              </dd>
            </>
          )}
        </dl>
      )}
    </Box>
  );
}

function FieldTooltipContent({
  schema,
  valueAtom,
}: {
  schema: Schema;
  valueAtom: ValueAtom<any>;
}) {
  const { formAtom } = useFormScope();
  const value = useAtomValue(valueAtom);
  const $getField = useFormField(formAtom);

  const data = schema.tooltip as TooltipType;
  const { depends } = data;

  const formModel = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.model), [formAtom])
  );
  const formRecord = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record), [formAtom])
  );

  const isRelational = Boolean(schema.target);
  const record = isRelational ? value : formRecord;
  const model = isRelational ? schema.target : formModel;
  const shouldFetch = !isRelational || depends?.trim?.() !== schema.targetName;

  return (
    <FieldDetails
      fetch={shouldFetch}
      data={data}
      model={model}
      record={record}
      $getField={$getField}
    />
  );
}
