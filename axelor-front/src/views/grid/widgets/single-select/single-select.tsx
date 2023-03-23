import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/src/grid/grid-column";
import classes from "./single-select.module.scss";
import { Field } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";

export function SingleSelect(props: GridColumnProps) {
  const { data, value } = props;
  const { selectionList } = data as Field;

  const option = (selectionList || []).find(
    (item) => String(item.value) === String(value)
  );

  return value && (
    <Box
      as="span"
      px={2}
      className={legacyClassNames(
        classes["single-select"],
        "label",
        "label-primary",
        classes[option?.color || "blue"]
      )}
    >
      <Box as="span" className={classes["tag-text"]}>
        {option?.title || value}
      </Box>
    </Box>
  );
}
