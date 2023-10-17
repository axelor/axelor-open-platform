import { FieldProps } from "../../builder";
import { Selection } from "../selection";
import { ImageSelectValue } from "./image-select-value";

export function ImageSelect(props: FieldProps<string | number | null>) {
  const { schema } = props;
  const { labels } = schema;
  return (
    <Selection
      {...props}
      autoComplete={false}
      renderValue={({ option }) => (
        <ImageSelectValue option={option} showLabel={labels} />
      )}
      renderOption={({ option }) => (
        <ImageSelectValue option={option} showLabel={labels} />
      )}
    />
  );
}
