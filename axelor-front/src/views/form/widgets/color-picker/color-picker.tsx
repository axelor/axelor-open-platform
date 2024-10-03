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

export function ColorPicker(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  const { lite } = schema;

  const [value, setValue] = useAtom(valueAtom);
  const [hsvaValue, setHsvaValue] = useState(null);

  const [showColorPicker, setShowColorPicker] = useState(false);
  const [element, setElement] = useState<EventTarget | null>(null);

  const handleShowColorPicker = useCallback(
    (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
      setShowColorPicker(true);
      setElement(e.target);
    },
    [],
  );

  const handleCloseColorPicker = useCallback(() => {
    setShowColorPicker(false);
  }, []);

  const handleOnChange = useCallback(
    (color: any) => {
      setHsvaValue(color.hsva);
      setValue(color.hex, true);
    },
    [setValue],
  );

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
    ].map((color) => colors[color]);
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
          className={styles.colorPreview}
          style={{
            backgroundColor: value as any,
            cursor: readonly ? "default" : "pointer",
          }}
          {...(!readonly && { onClick: handleShowColorPicker })}
        >
          <Box
            rounded={1}
            w={100}
            h={100}
            className={styles.checkerboard}
            opacity={!value ? 100 : 0}
          />
        </Box>
        {!readonly && value && (
          <Button
            className={styles.closeBtn}
            d="flex"
            p={1}
            border={false}
            onClick={() => {
              setValue("", true);
            }}
            title={i18n.get("Remove color")}
          >
            <MaterialIcon icon="close" />
          </Button>
        )}
      </Box>
      {showColorPicker && (
        <Popper
          open
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
                  color={hsvaValue ?? value ?? ""}
                  onChange={handleOnChange}
                />
              ) : (
                <Chrome
                  inputType={"hexa" as any}
                  showEyeDropper={false}
                  showColorPreview={false}
                  color={hsvaValue ?? value ?? ""}
                  onChange={handleOnChange}
                />
              )}
            </Box>
          </ClickAwayListener>
        </Popper>
      )}
    </FieldControl>
  );
}
