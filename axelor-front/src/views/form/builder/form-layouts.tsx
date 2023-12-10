import clsx from "clsx";
import { memo, useMemo } from "react";

import { Schema } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";

import { FormWidget } from "./form-widget";
import { FormLayout, WidgetProps } from "./types";

import styles from "./form-layouts.module.scss";

function computeLayout(schema: Schema) {
  const gap = typeof schema.gap === "number" ? `${schema.gap}px` : schema.gap;
  const gaps = gap?.trim().split(/\s+/g).filter(Boolean) ?? [];
  const rowGap = gaps[0] ?? "var(--ax-form-row-gap)";
  const columnGap = gaps[1] ?? gaps[0] ?? "var(--ax-form-column-gap)";
  const cols = parseInt(schema.cols) || 12;
  const itemSpan =
    parseInt(schema.itemSpan ?? schema.widgetAttrs?.itemSpan) || 6;
  const colWidths: string = schema.colWidths ?? "";
  const items = schema.items || [];
  const widths = colWidths
    .split(",")
    .map((w) => w.trim())
    .filter(Boolean)
    .map((w) => {
      if (w === "*") return "1fr";
      if (/^(\d+(.\d+)?)$/.test(w)) return `${w}px`;
      return w;
    });

  if (widths.length && widths.length < cols) {
    widths.push(`repeat(${cols - widths.length}, auto)`);
  }
  if (widths.length === 0) {
    widths.push(`repeat(${cols}, 1fr)`);
  }

  let last = 1;

  const numCols = widths.length > cols ? widths.length : cols;

  const template = widths.join(" ");
  const contents = items.map((item) => {
    const hasField =
      !["panel", "mail-messages", "mail-followers"].includes(item.type!) &&
      !item.editor;

    const colSpan = parseInt(item.colSpan) || itemSpan;
    let colStart = last > numCols ? 1 : last;
    let colEnd = colStart + colSpan;

    // if not enough columns, move to next row
    if (colEnd > numCols + 1) {
      colStart = 1;
      colEnd = Math.min(numCols, colSpan) + 1;
    }

    last = colEnd;

    return {
      className: colStart === 1 ? styles.first : undefined,
      style: {
        gridColumnStart: colStart,
        gridColumnEnd: colEnd,
        ...(hasField && { alignSelf: "end" }),
        ...(schema.$json && { gridColumn: `span ${colSpan}` }),
      },
      content: item,
    };
  });

  return {
    style: {
      "--g-row-gap": rowGap,
      "--g-col-gap": columnGap,
      display: "grid",
      gridTemplateColumns: template,
    },
    contents,
  };
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

export const StackLayout: FormLayout = memo(
  ({ schema, formAtom, parentAtom, className, readonly }) => {
    const { items = [], gap } = schema;
    return (items && (
      <div
        className={legacyClassNames(styles.stack, className, schema.css)}
        {...(gap && { style: { gap } })}
      >
        {items.map((item) => (
          <FormWidget
            key={item.uid}
            schema={item}
            formAtom={formAtom}
            parentAtom={parentAtom}
            readonly={readonly}
          />
        ))}
      </div>
    )) as JSX.Element;
  },
);

export const GridLayout: FormLayout = memo(
  ({ schema, formAtom, parentAtom, className, readonly }) => {
    const { style, contents } = useMemo(() => computeLayout(schema), [schema]);
    return (
      <div
        style={style}
        className={clsx(className, styles.grid, {
          [styles.table]: schema.layout === "table",
        })}
      >
        {contents.map(({ style, className, content }) => (
          <GridItem
            key={content.uid}
            style={style}
            className={className}
            schema={content}
            formAtom={formAtom}
            parentAtom={parentAtom}
            readonly={readonly}
          />
        ))}
      </div>
    );
  },
);

function GridItem(
  props: Omit<WidgetProps, "widgetAtom"> & {
    schema: Schema;
    style?: React.CSSProperties;
    className?: string;
  },
) {
  const { schema, formAtom, parentAtom, readonly, style } = props;
  const className = useMemo(() => layoutClassName(schema), [schema]);
  return (
    <div
      style={style}
      className={clsx(styles.gridItem, props.className, className)}
    >
      <FormWidget
        schema={schema}
        formAtom={formAtom}
        parentAtom={parentAtom}
        readonly={readonly}
      />
    </div>
  );
}
