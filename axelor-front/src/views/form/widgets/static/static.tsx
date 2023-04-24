import { useTemplate } from "@/hooks/use-parser";
import { useAtomValue } from "jotai";
import { useMemo } from "react";
import { FieldProps } from "../../builder";

export function Static({ formAtom, valueAtom }: FieldProps<string>) {
  const value = useAtomValue(valueAtom);
  const text = useMemo(() => value || "", [value]);
  const { record } = useAtomValue(formAtom);
  const Template = useTemplate(text);
  return (
    <div>
      <Template context={record} />
    </div>
  );
}
