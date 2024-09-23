import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { useGridContext } from "../../builder/scope";

export function DeleteIcon() {
  const { readonly, deleteIcon = true } = useGridContext();
  return (
    deleteIcon &&
    !readonly && (
      <Box h={100} d="flex" justifyContent="center" alignItems="center">
        <MaterialIcon icon={"delete"} />
      </Box>
    )
  );
}
