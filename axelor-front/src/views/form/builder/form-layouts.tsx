import clsx from "clsx";
import { useMemo } from "react";

import { Schema } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";

import { FormWidget } from "./form-widget";
import { FormLayout } from "./types";

import { legacyClassNames } from "@/styles/legacy";
import styles from "./form-layouts.module.scss";

function computeCols(cols: number, colWidths: string = "") {
  const widths = colWidths
    .split(",")
    .map((w) => w.trim())
    .filter(Boolean)
    .map((w) => {
      if (w === "*") return "1fr";
      if (/^(\d+(.\d+)?)$/.test(w)) return `${w}px`;
      return w;
    });

  if (widths.length === 0) {
    return undefined;
  }

  if (widths.length < cols) {
    widths.push(`repeat(${cols - widths.length}, auto-fit)`);
  }

  return widths.join(" ");
}

function layoutClassName(item: Schema) {
  const css: string = item.css || "";
  const names = css
    .split(" ")
    .map((name) => name.trim())
    .filter(Boolean)
    .filter((name) => {
      if (/^span\d+/.test(name)) return false;
      if (/^btn-?/.test(name)) return false;
      return true;
    });
  return legacyClassNames(names);
}

export const GridLayout: FormLayout = ({
  schema,
  formAtom,
  className,
  readonly,
}) => {
  const { cols = 12, colWidths, itemSpan, gap, items = [] } = schema;
  const widths = useMemo(() => computeCols(cols, colWidths), [cols, colWidths]);
  const style = {
    "--grid-cols": widths ?? cols,
    "--grid-gap": gap ?? "1rem",
  } as React.CSSProperties;

  return (
    <div
      className={clsx(className, styles.grid, {
        [styles.table]: schema.layout === "table",
        [styles.stack]:
          toKebabCase(schema.widget || schema.type) === "panel-stack",
      })}
      data-cols={widths ? undefined : cols}
      style={style}
    >
      {items.map((item) => (
        <GridItem
          key={item.uid}
          schema={item}
          itemSpan={itemSpan}
          className={layoutClassName(item)}
        >
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
  itemSpan?: number;
}) {
  const { schema, className, itemSpan, children } = props;
  const { colSpan = itemSpan, rowSpan } = schema;
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
