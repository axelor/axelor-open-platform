import clsx from "clsx";
import { useAtomValue } from "jotai";

import { Box, InputLabel } from "@axelor/ui";

import { FieldProps, WidgetProps } from "./types";

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
  widgetAtom,
  titleActions,
  children,
}: FieldControlProps<any>) {
  const { uid } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;
  const canShowTitle = showTitle ?? schema.showTitle ?? true;
  return (
    <Box className={clsx(className, styles.container)}>
      {canShowTitle && (
        <Box className={styles.title}>
          <InputLabel htmlFor={uid} className={styles.label}>
            {title}
          </InputLabel>
          {titleActions && <Box className={styles.actions}>{titleActions}</Box>}
        </Box>
      )}
      <Box className={styles.content}>{children}</Box>
    </Box>
  );
}
