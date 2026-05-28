import { useAtom } from "jotai";
import { useCallback, useId, useMemo, useState } from "react";

import { Box, Button } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import {
  ColorPicker as ColorPickerComponent,
  colorToHex,
  useColorPicker,
} from "@/components/color-picker";
import { i18n } from "@/services/client/i18n";
import { ColorResult } from "@uiw/color-convert";

import { FieldControl, FieldProps } from "../../builder";

import styles from "./color-picker.module.scss";

export function ColorPicker(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  const { lite, widgetAttrs } = schema;
  const { colorPickerShowAlpha = true } = widgetAttrs;

  const id = useId();
  const [value, setValue] = useAtom(valueAtom);
  // Color selected in the color picker popover
  const [color, setColor] = useState<ColorResult | null>(null);

  const onColorPickerClose = useCallback(() => {
    if (color?.hexa) {
      setValue(lite || !colorPickerShowAlpha ? color.hex : color.hexa, true);
    }
    setColor(null);
  }, [color, colorPickerShowAlpha, lite, setValue]);

  const { open: openPicker, pickerPopoverProps } = useColorPicker({
    onClose: onColorPickerClose,
  });

  const handleShowColorPicker = useCallback(
    (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
      setColor(null);
      openPicker(e.currentTarget);
    },
    [openPicker],
  );

  const handleResetColor = useCallback(() => {
    setColor(null);
    setValue("", true);
  }, [setValue]);

  /**
   * Current hex color code, either from the color picker or the record value
   */
  const currentHexaColor = useMemo(() => {
    return colorToHex(color?.hexa) ?? colorToHex(value) ?? undefined;
  }, [color, value]);

  return (
    <FieldControl {...props} inputId={id}>
      <Box
        id={id}
        className={styles.colorContainer}
        d="flex"
        flexDirection="row"
        alignItems="center"
        data-testid="input"
      >
        <Box
          rounded={1}
          overflow="hidden"
          w={100}
          h={100}
          {...(!value && { className: styles.checkerboard })}
        >
          <Box
            className={styles.colorPreview}
            style={{
              backgroundColor: currentHexaColor,
              cursor: readonly ? "default" : "pointer",
            }}
            role="button"
            tabIndex={-1}
            {...(!readonly && { onClick: handleShowColorPicker })}
            data-testid="color-preview"
          />
        </Box>
        {!readonly && value && (
          <Button
            className={styles.closeBtn}
            d="flex"
            p={1}
            border={false}
            onClick={handleResetColor}
            title={i18n.get("Remove color")}
            tabIndex={-1}
            data-testid="clear-button"
          >
            <MaterialIcon icon="close" />
          </Button>
        )}
      </Box>
      <ColorPickerComponent
        {...pickerPopoverProps}
        lite={lite}
        showAlpha={colorPickerShowAlpha}
        value={currentHexaColor}
        onChange={setColor}
      />
    </FieldControl>
  );
}
