import { GridLayout, WidgetProps } from "../../builder";
import { useMemo } from "react";

const WIDGET_COLSPAN: Record<string, number> = {
  "mail-messages": 9,
  "mail-followers": 3,
};

export function PanelMail(props: WidgetProps) {
  const { formAtom, schema } = props;
  const $schema = useMemo(() => {
    const { items } = schema;
    return {
      ...schema,
      items: items?.map((item) => ({
        ...item,
        colSpan:
          item.colSpan ||
          (items.length === 1 ? 12 : WIDGET_COLSPAN[item.type!]),
      })),
    };
  }, [schema]);

  return <GridLayout formAtom={formAtom} schema={$schema} />;
}
