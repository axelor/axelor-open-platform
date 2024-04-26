import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { useGridContext } from "../../builder/scope";

export function EditIcon() {
  const { readonly } = useGridContext();
  const icon = readonly ? "description" : "edit";
  return (
    <Box h={100} d="flex" justifyContent="center" alignItems="center">
      <MaterialIcon icon={icon} />
    </Box>
  );
}
