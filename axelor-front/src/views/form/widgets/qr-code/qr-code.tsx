import { QrCode as QrCodeUi } from "@axelor/ui";
import { useAtom } from "jotai";

import { FieldControl, FieldProps } from "../../builder";
import { String } from "../string";

export function QrCode(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  const { height } = schema;

  const [value] = useAtom(valueAtom);

  if (readonly && value)
    return (
      <FieldControl {...props}>
        <QrCodeUi value={value} height={height} />
      </FieldControl>
    );

  return <String {...props} />;
}
