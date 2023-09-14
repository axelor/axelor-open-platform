import { useAtomValue } from "jotai";
import { GridLayout, WidgetProps } from "../../builder";
import { useMemo } from "react";
import { focusAtom } from "jotai-optics";

const WIDGET_COLSPAN: Record<string, number> = {
  "mail-messages": 9,
  "mail-followers": 3,
};

export function PanelMail(props: WidgetProps) {
  const { formAtom, widgetAtom, schema } = props;
  const recordId = useAtomValue(
    useMemo(
      () => focusAtom(formAtom, (o) => o.prop("record").prop("id")),
      [formAtom]
    )
  );

  const model = useAtomValue(
    useMemo(() => focusAtom(formAtom, (o) => o.prop("model")), [formAtom])
  );

  const $schema = useMemo(() => {
    const { items } = schema;
    return {
      ...schema,
      items: items?.map((item) => ({
        ...item,
        model,
        modelId: recordId,
        colSpan:
          item.colSpan ||
          (items.length === 1 ? 12 : WIDGET_COLSPAN[item.type!]),
      })),
    };
  }, [schema, model, recordId]);

  return (
    ((recordId ?? 0) > 0 || model === "com.axelor.mail.db.MailMessage") && (
      <GridLayout
        formAtom={formAtom}
        parentAtom={widgetAtom}
        schema={$schema}
      />
    )
  );
}
