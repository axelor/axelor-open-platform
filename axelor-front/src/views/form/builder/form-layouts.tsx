import { Schema } from "@/services/client/meta.types";
import clsx from "clsx";
import { FormLayout } from "./types";

import styles from "./form-layouts.module.scss";
import { FormWidget } from "./form-widget";

export const GridLayout: FormLayout = ({ schema, formAtom, className }) => {
  const { cols, items = [] } = schema;
  return (
    <div className={clsx(className, styles.grid)} data-cols={cols}>
      {items.map((item) => (
        <GridItem key={item.uid} schema={item}>
          <FormWidget schema={item} formAtom={formAtom} />
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
