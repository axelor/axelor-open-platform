import clsx from "clsx";
import { useAtomValue } from "jotai";

import { Box, InputLabel } from "@axelor/ui";

import { FieldProps, WidgetProps } from "./types";

import { Tooltip } from "@/components/tooltip";
import { i18n } from "@/services/client/i18n";
import { session } from "@/services/client/session";
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
  return (
    <Box className={clsx(className, styles.container)}>
      {canShowTitle && (
        <Box className={styles.title}>
          <FieldLabel
            schema={schema}
            formAtom={formAtom}
            widgetAtom={widgetAtom}
            valueAtom={valueAtom}
          />
          {titleActions && <Box className={styles.actions}>{titleActions}</Box>}
        </Box>
      )}
      <Box className={styles.content}>{children}</Box>
    </Box>
  );
}

export function FieldLabel({
  schema,
  formAtom,
  widgetAtom,
  valueAtom,
  className,
}: FieldProps<any> & { className?: string }) {
  const { uid, help } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;

  const technical = session.info?.user.technical;
  const canShowHelp = help || technical;

  if (canShowHelp) {
    return (
      <Tooltip
        title={title}
        content={() => (
          <HelpContent
            schema={schema}
            formAtom={formAtom}
            widgetAtom={widgetAtom}
            valueAtom={valueAtom}
          />
        )}
      >
        <InputLabel
          htmlFor={uid}
          className={clsx(className, styles.label, { [styles.help]: help })}
        >
          {title}
        </InputLabel>
      </Tooltip>
    );
  }

  return (
    <InputLabel htmlFor={uid} className={clsx(styles.label)}>
      {title}
    </InputLabel>
  );
}

function HelpContent(props: FieldProps<any>) {
  const { schema, formAtom, widgetAtom } = props;
  const { name, serverType, target, help } = schema;
  const { model, original } = useAtomValue(formAtom);
  const { attrs } = useAtomValue(widgetAtom);
  const { domain } = attrs;

  const technical = session.info?.user.technical;

  const value = original?.[name];
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
