import { useContainerQuery } from "@/hooks/use-container-query";
import { FormView } from "@/services/client/meta.types";
import clsx from "clsx";
import { useMemo, useRef } from "react";
import { ViewProps } from "../types";
import { Form as FormComponent, FormLayout, FormWidget } from "./builder";

import styles from "./form.module.scss";

export function Form({ meta }: ViewProps<FormView>) {
  return (
    <div className={styles.formViewContainer}>
      <FormComponent className={styles.formView} meta={meta} layout={Layout} />
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
