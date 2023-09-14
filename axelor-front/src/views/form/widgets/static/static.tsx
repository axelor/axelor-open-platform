import { useTemplate } from "@/hooks/use-parser";
import { useAtomValue } from "jotai";
import { FieldProps } from "../../builder";
import { useFormField } from "../../builder/scope";

export function Static({ schema, formAtom }: FieldProps<string>) {
  const { text } = schema;
  const { record } = useAtomValue(formAtom);
  const Template = useTemplate(text);
  const $getField = useFormField(formAtom);
  return (
    <div>
      <Template
        context={record}
        options={{
          helpers: {
            $getField,
          },
        }}
      />
    </div>
  );
}
