import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/src/grid/grid-column";
import { SingleSelect } from "../single-select";

export function MultiSelect(props: GridColumnProps) {
  const { value } = props;
  const values: string[] = `${value || ""}`.split(/\s*,\s*/);
  return (
    <Box d="flex">
      {values.map((value, ind) => (
        <Box as="span" key={ind} me={1}>
          <SingleSelect {...props} value={value} />
        </Box>
      ))}
    </Box>
  );
}
