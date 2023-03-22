import clsx from "clsx";
import { useMemo, useRef } from "react";

import { useContainerQuery } from "@/hooks/use-container-query";
import { FormView } from "@/services/client/meta.types";
import { ViewToolBar } from "@/view-containers/view-toolbar";

import { ViewProps } from "../types";

import { Form as FormComponent, FormLayout, FormWidget } from "./builder";
import { fallbackFormAtom, fallbackWidgetAtom } from "./builder/atoms";

import styles from "./form.module.scss";

export function Form({ meta }: ViewProps<FormView>) {
  return (
    <div className={styles.formViewContainer}>
      <ViewToolBar
        meta={meta}
        actions={[
          {
            key: "new",
            text: "New",
            iconProps: {
              icon: "add",
            },
          },
          {
            key: "edit",
            text: "Edit",
            iconProps: {
              icon: "edit",
            },
          },
          {
            key: "save",
            text: "Save",
            iconProps: {
              icon: "save",
            },
          },
          {
            key: "delete",
            text: "Delete",
            iconProps: {
              icon: "delete",
            },
          },
        ]}
      />
      <div className={styles.formViewScroller}>
        <FormComponent
          className={styles.formView}
          schema={meta.view}
          fields={meta.fields!}
          layout={Layout}
          formAtom={fallbackFormAtom}
          widgetAtom={fallbackWidgetAtom}
        />
      </div>
    </div>
  );
}

const Layout: FormLayout = ({ schema, formAtom, className }) => {
  const ref = useRef<HTMLDivElement | null>(null);
  const isSmall = useContainerQuery(ref, "width < 768px");

  const { main, side, small } = useMemo(() => {
    const items = [...(schema.items ?? [])];
    const head = items.filter((x) => x.widget === "wkf-status");
    const side = items.filter((x) => x.sidebar);
    const mail = items.filter((x) => x.type === "panel-mail");
    const rest = items.filter(
      (x) => !head.includes(x) && !side.includes(x) && !mail.includes(x)
    );

    const sideTop = side.slice(0, 1);
    const sideAll = side.slice(1);

    const small = [...head, ...sideTop, ...rest, ...sideAll, ...mail];
    const main = [...head, ...rest, ...mail];

    return {
      main,
      side,
      small,
    };
  }, [schema]);

  const mainItems = isSmall ? small : main;
  const sideItems = isSmall ? [] : side;

  return (
    <div
      className={clsx(className, styles.formLayout, {
        [styles.small]: isSmall,
      })}
      ref={ref}
    >
      <div className={styles.main}>
        {mainItems.map((item) => (
          <FormWidget key={item.uid} schema={item} formAtom={formAtom} />
        ))}
      </div>
      {sideItems.length > 0 && (
        <div className={styles.side}>
          {side.map((item) => (
            <FormWidget key={item.uid} schema={item} formAtom={formAtom} />
          ))}
        </div>
      )}
    </div>
  );
};
