import { QrCode as QrCodeUi } from "@axelor/ui";
import { useAtom } from "jotai";
import { useId } from "react";

import { FieldControl, FieldProps } from "../../builder";
import { String } from "../string";

export function QrCode(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  const { height } = schema;

  const id = useId();
  const [value] = useAtom(valueAtom);

  if (readonly && value)
    return (
      <FieldControl {...props} inputId={id}>
        <QrCodeUi id={id} value={value} height={height} data-testid="input" />
      </FieldControl>
    );

  return <String {...props} />;
}
