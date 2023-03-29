import { Box, Badge } from "@axelor/ui";
import clsx from "clsx";
import classes from "./chip.module.scss";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";


export function Chip({
  className,
  label,
  value,
  color: _color,
  style: styleProp = {},
  onDelete,
  deleteIconProps = {},
  ...rest
}) {
  const color = _color || "default";
  function renderLabel() {
    return (
      <>
        <span>{label}</span>
        {onDelete && (
          <Box
            d="flex"
            alignItems="center"
            justifyContent="center"
            className={classes.chipIcon}
            ms={1}
            onClick={onDelete}
          >
            <MaterialIcon icon="close" {...deleteIconProps} />
          </Box>
        )}
      </>
    );
  }

  const chipCss = classes[(color || value).trim()];
  if (!chipCss) return renderLabel();
  return (
    <Badge
      d="flex"
      className={clsx(classes.chip, classes.colorChip, className, chipCss)}
      {...rest}
      style={styleProp}
    >
      {renderLabel()}
    </Badge>
  );
}

