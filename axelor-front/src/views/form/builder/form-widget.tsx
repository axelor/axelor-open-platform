import { Schema } from "@/services/client/meta.types";
import { PrimitiveAtom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { useMemo } from "react";
import { useWidgetComp } from "./hooks";
import { FormState, WidgetProps } from "./types";
import { defaultAttrs } from "./utils";

export function FormWidget(props: {
  schema: Schema;
  formAtom: PrimitiveAtom<FormState>;
  readonly?: boolean;
}) {
  const { schema, formAtom, readonly } = props;

  const widgetAtom = useMemo(() => {
    const { uid } = schema;
    const attrs = defaultAttrs(schema);
    return focusAtom(formAtom, (o) =>
      o.prop("states").prop(uid).valueOr({ attrs })
    );
  }, [formAtom, schema]);

  const { attrs } = useAtomValue(widgetAtom);

  const widget = schema.widget!;
  const type = schema.type;

  const { state, data: Comp } = useWidgetComp(widget);

  if (state === "loading") {
    return <div>Loading...</div>;
  }

  const widgetProps = {
    schema,
    formAtom,
    widgetAtom,
    readonly: readonly ?? attrs.readonly,
  };

  if (Comp) {
    return type === "field" ? (
      <FormField component={Comp} {...widgetProps} />
    ) : (
      <Comp {...widgetProps} />
    );
  }
  return (
    <Unknown schema={schema} formAtom={formAtom} widgetAtom={widgetAtom} />
  );
}

function FormField(props: WidgetProps & { component: React.ElementType }) {
  const { schema, formAtom, component: Comp } = props;
  const name = schema.name!;

  const valueAtom = useMemo(
    () => focusAtom(formAtom, (o) => o.prop("record").prop(name)),
    [formAtom, name]
  );

  return <Comp {...props} valueAtom={valueAtom} />;
}

function Unknown(props: WidgetProps) {
  const { schema } = props;
  return <div>{schema.widget}</div>;
}
