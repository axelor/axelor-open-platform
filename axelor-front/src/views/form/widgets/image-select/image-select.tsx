import { Box } from "@axelor/ui";

import { Icon } from "@/components/icon";
import { Selection as SelectionType } from "@/services/client/meta.types";

import { FieldProps } from "../../builder";
import { Selection } from "../selection";

function Image({
  showLabel,
  option,
}: {
  showLabel?: boolean;
  option: SelectionType;
}) {
  const icon = option?.icon || option?.value;
  const text = option?.title;

  return (
    <Box d="flex" gap={6} alignItems="center">
      {icon && !icon.includes(".") ? (
        <Icon icon={icon} />
      ) : (
        <img
          style={
            showLabel === false
              ? { maxHeight: 18 }
              : { maxWidth: 18, height: "fit-content" }
          }
          src={icon}
          alt={text}
        />
      )}
      {showLabel !== false && text}
    </Box>
  );
}

export function ImageSelect(props: FieldProps<string | number | null>) {
  const { schema } = props;
  const { labels } = schema;
  return (
    <Selection
      {...props}
      renderValue={({ option }) => <Image option={option} showLabel={labels} />}
      renderOption={({ option }) => (
        <Image option={option} showLabel={labels} />
      )}
    />
  );
}
