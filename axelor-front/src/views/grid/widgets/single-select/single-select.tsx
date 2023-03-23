import clsx from "clsx";
import { Box, Badge } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/src/grid/grid-column";
import { Field } from "@/services/client/meta.types";
import classes from "./single-select.module.scss";

export function SingleSelect(props: GridColumnProps) {
  const { data, value } = props;
  const { selectionList } = data as Field;

  const option = (selectionList || []).find(
    (item) => String(item.value) === String(value)
  );

  return (
    value && (
      <Badge
        px={2}
        className={clsx(
          classes["single-select"],
          classes[(option?.color || "blue").trim()]
        )}
      >
        <Box as="span" className={classes["tag-text"]}>
          {option?.title || value}
        </Box>
      </Badge>
    )
  );
}
