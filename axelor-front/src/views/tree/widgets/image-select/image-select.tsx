import { WidgetProps } from "../../types";
import { Icon } from "@/components/icon";
import { Box } from "@axelor/ui";

export function ImageSelect({ field, record }: WidgetProps) {
  const { name, selectionList, labels } = field;
  const value = record[name];
  const selectValue = selectionList?.find(
    (item) => String(item.value) === String(value),
  );

  if (selectValue?.icon) {
    return (
      <>
        <Box gap={6} alignItems="center">
          {selectValue.icon && !selectValue.icon.includes(".") ? (
            <Icon icon={selectValue.icon} />
          ) : (
            <img
              style={
                labels === false
                  ? { maxHeight: 24 }
                  : { maxWidth: 18, height: "fit-content" }
              }
              src={selectValue.icon}
              alt={selectValue.title}
            />
          )}
          {labels !== false && <span>{selectValue.title}</span>}
        </Box>
      </>
    );
  }
  return value;
}
