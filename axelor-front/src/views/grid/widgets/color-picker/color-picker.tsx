import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

import styles from "../../../form/widgets/color-picker/color-picker.module.scss";

export function ColorPicker(props: GridColumnProps) {
  const { rawValue: value } = props;

  return (
    value && (
      <Box
        rounded={1}
        className={styles.colorPreview}
        style={{ backgroundColor: value }}
      />
    )
  );
}
