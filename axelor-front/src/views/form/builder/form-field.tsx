import clsx from "clsx";

import { Box } from "@axelor/ui";

import styles from "./form-field.module.css";

export function FieldContainer({
  className,
  children,
}: {
  children: React.ReactNode;
  className?: string;
  readonly?: boolean;
}) {
  return (
    <Box className={clsx(className, styles.fieldContainer)}>{children}</Box>
  );
}
