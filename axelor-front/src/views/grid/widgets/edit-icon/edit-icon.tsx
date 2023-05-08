import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { useGridScope } from "../../builder/scope";

export function EditIcon() {
  const { readonly } = useGridScope();
  const icon = readonly ? "description" : "edit";
  return (
    <Box h={100} d="flex" justifyContent="center" alignItems="center">
      <MaterialIcon icon={icon} fontSize={20} />
    </Box>
  );
}
