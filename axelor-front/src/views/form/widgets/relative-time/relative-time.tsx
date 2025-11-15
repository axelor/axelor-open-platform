import { useAtomValue } from "jotai";
import { useId } from "react";

import format from "@/utils/format";

import { FieldControl, FieldProps } from "../../builder";
import { ViewerInput } from "../string/viewer";

export function RelativeTime(props: FieldProps<string>) {
  const { schema, valueAtom } = props;

  const id = useId();
  const value = useAtomValue(valueAtom);
  const $value = (() => {
    try {
      return format(value, {
        props: schema as any,
      });
    } catch {
      return "";
    }
  })();
  return (
    <FieldControl {...props} inputId={id}>
      <ViewerInput id={id} name={schema.name} value={$value} />
    </FieldControl>
  );
}
