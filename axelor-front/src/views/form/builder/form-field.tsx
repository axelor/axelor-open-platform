import clsx from "clsx";
import { useMemo } from "react";
import { selectAtom } from "jotai/utils";
import { useAtomValue } from "jotai";

import { Box, InputLabel } from "@axelor/ui";

import { DataStore } from "@/services/client/data-store";
import { Schema, Tooltip as TooltipType } from "@/services/client/meta.types";
import { FieldProps, ValueAtom, WidgetProps } from "./types";

import { Tooltip } from "@/components/tooltip";
import { i18n } from "@/services/client/i18n";
import { session } from "@/services/client/session";
import { useTemplate } from "@/hooks/use-parser";
import { useAsync } from "@/hooks/use-async";
import { useFormScope } from "./scope";
import format from "@/utils/format";

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
  const canShowTitle = showTitle ?? schema.showTitle ?? true;

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
    </Box>
  );
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

  const technical = session.info?.user.technical && name;
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
  const { name, serverType, target, help } = schema;
  const { model, original } = useAtomValue(formAtom);
  const { attrs } = useAtomValue(widgetAtom);
  const { domain } = attrs;

  const technical = session.info?.user.technical && name;

  const value = name && original ? original[name] : undefined;
  let text = format(value, { props: schema as any });

  if (serverType && serverType.endsWith("_ONE") && value) {
    text = `(${value.id}, ${text})`;
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
            <code>{serverType}</code>
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
          <dt>{i18n.get("Orig. value")}</dt>
          <dd>
            <code>{text}</code>
          </dd>
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

  const data = schema.tooltip as TooltipType;
  const { depends, template } = data;
  const Template = useTemplate(template!);

  const formModel = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.model), [formAtom])
  );
  const formRecord = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record), [formAtom])
  );

  const isRelational = Boolean(schema.target);
  const record = isRelational ? value : formRecord;
  const model = isRelational ? schema.target : formModel;

  const { data: context } = useAsync(async () => {
    const shouldFetch =
      !isRelational || depends?.trim?.() !== schema.targetName;
    const recordId = record?.id;

    let values = { ...record };

    if (shouldFetch && model && recordId && recordId > 0) {
      const ds = new DataStore(model);
      const newValues = await ds.read(recordId, {
        fields: (depends || "")?.split?.(",").map((f) => f.trim()),
      });
      values = { ...values, ...newValues };
    }

    return { ...values, record: values };
  }, [isRelational, schema, model, record]);

  return (
    context && (
      <Box>
        <Template context={context} />
      </Box>
    )
  );
}
