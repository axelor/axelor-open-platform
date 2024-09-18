import { useMemo } from "react";
import { FieldProps } from "../../builder";
import { Selection } from "../selection";

const NAMES = [
  "BinaryLink",
  "BooleanRadio",
  "BooleanSelect",
  "BooleanSwitch",
  "CheckboxSelect",
  "CodeEditor",
  "ColorPicker",
  "Drawing",
  "Duration",
  "Email",
  "EvalRefSelect",
  "Expandable",
  "Html",
  "Image",
  "ImageLink",
  "ImageSelect",
  "InlineCheckbox",
  "MasterDetail",
  "MultiSelect",
  "NavSelect",
  "Password",
  "Phone",
  "Progress",
  "RadioSelect",
  "Rating",
  "RefLink",
  "RefSelect",
  "RefText",
  "RelativeTime",
  "SelectProgress",
  "SingleSelect",
  "Slider",
  "Stepper",
  "SuggestBox",
  "SwitchSelect",
  "TagSelect",
  "Text",
  "Toggle",
  "TreeGrid",
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

  return <Selection {...props} schema={schema} />;
}
