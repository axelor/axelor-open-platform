import {
  Box,
  OverflowList,
  OverflowListButtonType,
  OverflowListItemProps,
} from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

import { Field } from "@/services/client/meta.types";
import { Chip } from "@/views/form/widgets";
import { useAppTheme } from "@/hooks/use-app-theme";

export function TagSelect(props: GridColumnProps) {
  const { record, data } = props;
  const { name, targetName = "" } = data as Field;
  const list = (record?.[name] || []) as OverflowListItemProps[];
  const theme = useAppTheme();
  return (
    <OverflowList
      d="flex"
      g={1}
      items={list}
      renderListItem={(item: OverflowListItemProps, index: number) => (
        <Box key={index}>
          <Chip title={item[targetName]} color={"indigo"} />
        </Box>
      )}
      renderOverflowItem={(
        item: OverflowListItemProps,
        index: number,
        closeDropdown?: () => void
      ) => {
        return (
          <Box key={index} onClick={() => closeDropdown && closeDropdown()}>
            <Chip title={item[targetName]} color={"indigo"} />
          </Box>
        );
      }}
      renderButton={(
        type: OverflowListButtonType,
        props: any,
        count?: number
      ) => {
        if (type === "dropdown") {
          return (
            <Box {...props}>
              <Chip
                title={`+${count ?? ""}`}
                color={theme === "dark" ? "gray" : "white"}
              />
            </Box>
          );
        }
        return null;
      }}
    />
  );
}
