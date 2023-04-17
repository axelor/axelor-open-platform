import clsx from "clsx";

import { Schema } from "@/services/client/meta.types";

import { FormWidget } from "./form-widget";
import { FormLayout } from "./types";

import { useMemo } from "react";
import styles from "./form-layouts.module.scss";

function computeCols(cols: number, colWidths?: string | (string | number)[]) {
  const widths: string[] = [];
  if (typeof colWidths === "string") colWidths.split(",");
  if (Array.isArray(colWidths)) {
    for (const width of colWidths) {
      const w = String(width).trim();
      if (w === "*") {
        widths.push("1fr");
      } else if (/^(\d+(.\d+)?)$/.test(w)) {
        widths.push(`${w}px`);
      } else {
        widths.push(w);
      }
    }
  }

  if (widths.length === 0) {
    return undefined;
  }

  while (widths.length < cols) {
    widths.push("1fr");
  }

  return widths.join(" ");
}

export const GridLayout: FormLayout = ({
  schema,
  formAtom,
  className,
  readonly,
}) => {
  const { cols, colWidths, gap, items = [] } = schema;
  const widths = useMemo(() => computeCols(cols, colWidths), [cols, colWidths]);
  const style = {
    "--grid-cols": widths ?? cols,
    "--grid-gap": gap ?? "1rem",
  } as React.CSSProperties;

  return (
    <div
      className={clsx(className, styles.grid, {
        [styles.table]: schema.layout === "table",
      })}
      data-cols={widths ? undefined : cols}
      style={style}
    >
      {items.map((item) => (
        <GridItem key={item.uid} schema={item}>
          <FormWidget schema={item} formAtom={formAtom} readonly={readonly} />
        </GridItem>
      ))}
    </div>
  );
};

function GridItem(props: {
  schema: Schema;
  children: React.ReactNode;
  className?: string;
}) {
  const { schema, className, children } = props;
  const { colSpan, rowSpan } = schema;
  return (
    <div
      className={clsx(styles.gridItem, className)}
      data-colspan={colSpan}
      data-rowspan={rowSpan}
    >
      {children}
    </div>
  );
}
