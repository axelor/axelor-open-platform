import { useAtom, useAtomValue } from "jotai";
import { useId } from "react";

import { Switch } from "@axelor/ui";

import { FieldControl, FieldProps } from "../../builder";

export function BooleanSwitch(props: FieldProps<boolean>) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const [value = false, setValue] = useAtom(valueAtom);
  const {
    attrs: { focus },
  } = useAtomValue(widgetAtom);

  const id = useId();

  return (
    <FieldControl {...props} inputId={id}>
      <Switch
        key={focus ? "focused" : "normal"}
        data-input
        data-testid="input"
        autoFocus={focus}
        id={id}
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
