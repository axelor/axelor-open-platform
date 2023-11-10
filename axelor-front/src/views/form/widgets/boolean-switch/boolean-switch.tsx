import { useAtom, useAtomValue } from "jotai";

import { Switch } from "@axelor/ui";

import { FieldControl, FieldProps } from "../../builder";

export function BooleanSwitch(props: FieldProps<boolean>) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { uid } = schema;
  const [value = false, setValue] = useAtom(valueAtom);
  const {
    attrs: { focus },
  } = useAtomValue(widgetAtom);

  return (
    <FieldControl {...props}>
      <Switch
        {...(focus && { key: "focused" })}
        data-input
        autoFocus={focus}
        id={uid}
        invalid={invalid}
        checked={value ?? false}
        readOnly={readonly}
        disabled={readonly}
        onChange={() => setValue(!value, true)}
        value=""
      />
    </FieldControl>
  );
}
