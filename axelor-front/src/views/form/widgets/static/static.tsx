import { useTemplate } from "@/hooks/use-parser";
import { useAtomValue } from "jotai";
import { FieldProps } from "../../builder";

export function Static({ schema, formAtom }: FieldProps<string>) {
  const { text } = schema;
  const { record } = useAtomValue(formAtom);
  const Template = useTemplate(text);
  return (
    <div>
      <Template context={record} />
    </div>
  );
}
