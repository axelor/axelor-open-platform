import { useMemo } from "react";
import { FieldContainer, FieldProps } from "../../builder";
import { Selection } from "../selection";

const NAMES = [
  "BinaryLink",
  "BooleanRadio",
  "BooleanSelect",
  "BooleanSwitch",
  "CheckboxSelect",
  "CodeEditor",
  "Duration",
  "Email",
  "Html",
  "Image",
  "ImageLink",
  "ImageSelect",
  "InlineCheckbox",
  "Markdown",
  "MultiSelect",
  "NavSelect",
  "Password",
  "Progress",
  "RadioSelect",
  "RefLink",
  "RefSelect",
  "RefText",
  "RelativeTime",
  "SelectProgress",
  "SingleSelect",
  "SuggestBox",
  "TagSelect",
  "Toggle",
  "Url",
];

export function WidgetSelect(props: FieldProps<any>) {
  const schema = useMemo(() => {
    return {
      ...props.schema,
      selectionList: NAMES.map((name) => ({
        value: name,
        title: name,
      })),
    };
  }, [props.schema]);

  return (
    <FieldContainer>
      <Selection {...props} schema={schema} />
    </FieldContainer>
  );
}
