import { clsx } from "@axelor/ui";
import { useAtom, useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import {
  forwardRef,
  useDeferredValue,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { Box, ClickAwayListener, InputFeedback, InputLabel } from "@axelor/ui";

import { Tooltip } from "@/components/tooltip";
import { Icon } from "@/components/icon";
import { useAsync } from "@/hooks/use-async";
import { useHilites, useTemplate } from "@/hooks/use-parser";
import { useSession } from "@/hooks/use-session";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import {
  Hilite,
  Schema,
  Tooltip as TooltipType,
} from "@/services/client/meta.types";
import { session } from "@/services/client/session";
import { focusAtom } from "@/utils/atoms";
import format from "@/utils/format";

import { useFormScope } from "./scope";
import { FieldProps, FormAtom, ValueAtom, WidgetProps } from "./types";

import styles from "./form-field.module.css";
import { legacyClassNames } from "@/styles/legacy";
import { sanitize } from "@/utils/sanitize";

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

function useFieldClassNames(schema: Schema) {
  const hilites = schema.hilites as Hilite[];
  const [labelClassName, setLabelClassName] = useState<string>();
  const [contentClassName, setContentClassName] = useState<string>();

  const { recordHandler } = useFormScope();
  const getHilite = useHilites(hilites);

  useEffect(() => {
    if (!hilites?.length) return;

    recordHandler.subscribe((record) => {
      const { color, background, css } = getHilite(record)?.[0] ?? {};
      setLabelClassName(color ? legacyClassNames(`hilite-${color}-text`) : "");
      setContentClassName(
        css
          ? legacyClassNames(css, {
              "hilite-fill-body-bg": background,
            })
          : "",
      );
    });
  }, [hilites, recordHandler, getHilite]);

  return [labelClassName, contentClassName];
}

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
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [labelClassName, contentClassName] = useFieldClassNames(schema);
  const canShowTitle =
    showTitle ?? schema.showTitle ?? schema.widgetAttrs?.showTitle ?? true;

  const [focus, setFocus] = useAtom(
    useMemo(
      () =>
        focusAtom(
          widgetAtom,
          (state) => state.attrs.focus,
          (state, focus) => ({ ...state, attrs: { ...state.attrs, focus } }),
        ),
      [widgetAtom],
    ),
  );
  const focusInput = useDeferredValue(focus);

  useEffect(() => {
    if (focusInput) {
      const input = containerRef.current?.querySelector?.(
        "input,textarea",
      ) as HTMLInputElement;
      input && input?.select?.();
    }
  }, [focusInput]);

  function render() {
    const content = (
      <Box className={clsx(styles.content, contentClassName)}>{children}</Box>
    );
    return focus ? (
      <ClickAwayListener onClickAway={() => setFocus(undefined)}>
        {content}
      </ClickAwayListener>
    ) : (
      content
    );
  }

  return (
    <Box ref={containerRef} className={clsx(className, styles.container)}>
      {(canShowTitle || titleActions) && (
        <Box className={styles.title}>
          {canShowTitle && (
            <FieldLabel
              schema={schema}
              className={labelClassName}
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
      [widgetAtom],
    ),
  );
  return error && <InputFeedback invalid>{error}</InputFeedback>;
}

export const FieldLabelTitle = forwardRef<HTMLSpanElement, { title?: string }>(
  function FieldLabelTitle({ title }, ref) {
    const __html = useMemo(() => title && sanitize(title), [title]);
    return __html && <span ref={ref} dangerouslySetInnerHTML={{ __html }} />;
  },
);

export function FieldLabel({
  schema,
  icon,
  formAtom,
  widgetAtom,
  className,
}: WidgetProps & { className?: string; icon?: string }) {
  const { data: sessionInfo } = useSession();
  const { uid, help } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;

  const canShowHelp = !sessionInfo?.user?.noHelp && !!help;

  return (
    <HelpPopover schema={schema} formAtom={formAtom} widgetAtom={widgetAtom}>
      <InputLabel
        htmlFor={uid}
        className={clsx(className, styles.label, {
          [styles.help]: canShowHelp,
        })}
      >
        <span
          className={clsx(styles.labelText, {
            [styles.icon]: Boolean(icon),
          })}
        >
          {icon && <Icon icon={icon} />}
          <FieldLabelTitle title={title} />
        </span>
      </InputLabel>
    </HelpPopover>
  );
}

export function FieldDetails({
  data,
  fetch = true,
  model,
  record,
  parent,
}: {
  data: TooltipType;
  fetch?: boolean;
  model?: string;
  record?: DataRecord;
  parent?: FormAtom;
}) {
  const { depends, template } = data;
  const Template = useTemplate(template!, { parent });
  const { data: context } = useAsync(async () => {
    let values = { ...record };
    if (fetch && model && record?.id) {
      const ds = new DataStore(model);
      const newValues = await ds.read(+record.id, {
        fields: (depends || "")?.split?.(",").map((f) => f.trim()),
      });
      values = { ...values, ...newValues };
    }
    return { ...values };
  }, [fetch, model, record]);

  return (
    context && (
      <Box>
        <Template context={context} />
      </Box>
    )
  );
}

export function HelpPopover({
  schema,
  title,
  formAtom,
  widgetAtom,
  children,
  content,
}: WidgetProps & {
  title?: string;
  children: React.ReactElement;
  content?: () => JSX.Element | null;
}) {
  const { type, name, help } = schema;
  const { attrs } = useAtomValue(widgetAtom);

  const technical =
    session.info?.user?.technical &&
    (name || ["panel", "dashlet"].includes(type ?? ""));
  const canShowHelp = help || technical;

  if (canShowHelp) {
    return (
      <Tooltip
        title={title ?? attrs.title}
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
  const { name, serverType, type, target, help, widget, selection, action } =
    schema;
  const { model: formModel, original } = useAtomValue(formAtom);
  const { attrs } = useAtomValue(widgetAtom);
  const { domain } = attrs;

  const model = (type === "dashlet" && schema.model) || formModel;
  const technical =
    session.info?.user?.technical &&
    (name || ["panel", "dashlet"].includes(type!));
  const canShowHelp = !session.info?.user?.noHelp && !!help;

  const value = name && original ? original[name] : undefined;
  let text = format(value, { props: schema as any });
  const shouldDisplayValue =
    serverType &&
    !["TEXT", "BINARY"].includes(serverType) &&
    widget !== "password";

  if (serverType?.endsWith("_ONE") && value) {
    text = `(${value.id}, ${text})`;
  } else if (value && ["ONE_TO_MANY", "MANY_TO_MANY"].includes(serverType)) {
    const length = value.length;
    const items = value
      .slice(0, length > 5 ? 5 : length)
      .map((item: any) => item.id);
    if (length > 5) {
      items.push("...");
    }
    text = items.join(", ");
  } else if (value && (serverType === "ENUM" || selection)) {
    text = `${value} -> ${text}`;
  } else if (serverType === "STRING" && text) {
    if (text.length > 50) {
      text = text.slice(0, 50) + "...";
    }
  }

  return (
    <Box className={styles.tooltip}>
      {canShowHelp && <Box className={styles.help}>{help}</Box>}
      {canShowHelp && technical && <hr />}
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
            <code>{name ?? "undefined"}</code>
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
          {action && (
            <>
              <dt>{i18n.get("Action")}</dt>
              <dd>
                <code>{action}</code>
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

  const data = schema.tooltip as TooltipType;
  const { depends } = data;

  const formModel = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.model), [formAtom]),
  );
  const formRecord = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record), [formAtom]),
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
      {...(isRelational && {
        parent: formAtom,
      })}
    />
  );
}
