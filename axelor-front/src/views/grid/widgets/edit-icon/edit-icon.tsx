import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { GridColumnProps } from "@axelor/ui/src/grid/grid-column";

export function EditIcon(props: GridColumnProps) {
  const data = (props.data || {}) as any;
  const icon = data.readonly ? "description" : "edit";
  return (
    <Box h={100} d="flex" justifyContent="center" alignItems="center">
      <MaterialIcon icon={icon} fontSize={20} />
    </Box>
  );
}
