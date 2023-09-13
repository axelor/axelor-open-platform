import { Box } from "@axelor/ui";

import { Icon } from "@/components/icon";
import { Selection as SelectionType } from "@/services/client/meta.types";

export function ImageSelectValue({
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
