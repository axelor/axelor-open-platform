import { useAtomValue } from "jotai";

import { moment } from "@/services/client/l10n";
import { FieldControl, FieldProps } from "../../builder";
import { ViewerInput } from "../string/viewer";

export function RelativeTime(props: FieldProps<string>) {
  const { schema, valueAtom } = props;
  const value = useAtomValue(valueAtom);
  const $value = (() => {
    try {
      return value ? moment(value).fromNow() : "";
    } catch {
      return "";
    }
  })();
  return (
    <FieldControl {...props}>
      <ViewerInput name={schema.name} value={$value} />
    </FieldControl>
  );
}
