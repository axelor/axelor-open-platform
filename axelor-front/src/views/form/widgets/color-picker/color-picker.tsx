import Block from "@uiw/react-color-block";
import Chrome from "@uiw/react-color-chrome";
import { useAtom } from "jotai";
import { useCallback, useMemo, useState } from "react";

import { Box, Button, ClickAwayListener, Popper } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { i18n } from "@/services/client/i18n";
import { FieldControl, FieldProps } from "../../builder";

import colors from "@/styles/legacy/_colors.module.scss";
import styles from "./color-picker.module.scss";

const DEFAULT_COLOR = { h: 0, s: 0, v: 0, a: 1 };

export function ColorPicker(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  const { lite, widgetAttrs } = schema;
  const { colorPickerShowAlpha = true } = widgetAttrs;

  const [value, setValue] = useAtom(valueAtom);
  const [color, setColor] = useState<any>({});

  const [showColorPicker, setShowColorPicker] = useState(false);
  const [element, setElement] = useState<EventTarget | null>(null);

  const isValueHex = useMemo(
    () => /^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/.test(value ?? ""),
    [value],
  );

  const handleShowColorPicker = useCallback(
    (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
      setColor({});
      setShowColorPicker(true);
      setElement(e.target);
    },
    [],
  );

  const handleCloseColorPicker = useCallback(() => {
    if (color?.hexa) {
      setValue(lite || !colorPickerShowAlpha ? color.hex : color.hexa, true);
    }

    setShowColorPicker(false);
  }, [color, colorPickerShowAlpha, lite, setValue]);

  const handleOnChange = useCallback((newColor: any) => {
    setColor(newColor);
  }, []);

  const handleResetColor = useCallback(() => {
    setColor({});
    setValue("", true);
  }, [setValue]);

  const colorPalette = useMemo(() => {
    return [
      "orange",
      "yellow",
      "lightgreen",
      "green",
      "cyan",
      "blue",
      "bluegrey",
      "red",
      "pink",
      "purple",
    ].map((colorName) => colors[colorName]);
  }, []);

  return (
    <FieldControl {...props}>
      <Box
        className={styles.colorContainer}
        d="flex"
        flexDirection="row"
        alignItems="center"
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
              backgroundColor: showColorPicker ? (color?.hexa ?? value) : value,
              cursor: readonly ? "default" : "pointer",
            }}
            {...(!readonly && { onClick: handleShowColorPicker })}
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
          >
            <MaterialIcon icon="close" />
          </Button>
        )}
      </Box>
      <Popper
        open={showColorPicker}
        shadow
        rounded
        target={element as HTMLElement}
        placement={lite ? "bottom" : "bottom-start"}
      >
        <ClickAwayListener onClickAway={handleCloseColorPicker}>
          <Box>
            {lite ? (
              <Block
                colors={colorPalette}
                color={color?.hsva ?? (isValueHex ? value : DEFAULT_COLOR)}
                onChange={handleOnChange}
              />
            ) : (
              <Chrome
                inputType={"hexa" as any}
                showAlpha={colorPickerShowAlpha}
                showEyeDropper={false}
                showColorPreview={false}
                color={color?.hsva ?? (isValueHex ? value : DEFAULT_COLOR)}
                onChange={handleOnChange}
              />
            )}
          </Box>
        </ClickAwayListener>
      </Popper>
    </FieldControl>
  );
}
