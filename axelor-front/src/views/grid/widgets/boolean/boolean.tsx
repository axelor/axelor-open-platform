import { Box, Input } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";
import classes from "./boolean.module.scss";

export function Boolean(props: GridColumnProps) {
  const { value } = props;
  return (
    <Box d="flex" justifyContent="center">
      <Input
        type="checkbox"
        className={classes.checkbox}
        checked={String(value) === 'true'}
        onChange={() => {}}
      />
    </Box>
  );
}
