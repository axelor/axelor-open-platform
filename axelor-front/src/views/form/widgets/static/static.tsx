import { TemplateRenderer } from "@/hooks/use-parser";
import { useAtomValue } from "jotai";
import { FieldProps } from "../../builder";

export function Static({ schema, formAtom }: FieldProps<string>) {
  const { text } = schema;
  const { record } = useAtomValue(formAtom);
  return (
    <div>
      <TemplateRenderer template={text} context={record} />
    </div>
  );
}
