import Color from "color";

import { Box, ClickAwayListener, Popper } from "@axelor/ui";
import type { ColorResult } from "@uiw/color-convert";
import Block from "@uiw/react-color-block";
import Chrome, { ChromeInputType } from "@uiw/react-color-chrome";

import colors from "@/styles/legacy/_colors.module.scss";

const DEFAULT_COLOR = { h: 0, s: 0, v: 0, a: 1 };

const DEFAULT_PALETTE = [
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

export type ColorPickerProps = {
  /** When true, displays a simple block color palette instead of the full Chrome picker */
  lite?: boolean;
  /**
   * Indicates whether the alpha transparency is displayed or enabled.
   */
  showAlpha?: boolean;
  /**
   * Color palette for lite mode.
   * Each item in the array should be a string that defines a color.
   * The colors can be specified in various formats, such as hexadecimal, RGB, or named colors.
   */
  palette?: string[];
  /**
   * The current color value in the hex format.
   */
  value: string | null | undefined;
  /** Callback function invoked when a color is selected */
  onChange: (color: ColorResult) => void;
};

export type ColorPickerPopoverProps = {
  /** Controls the visibility of the color picker popover */
  isOpen: boolean;
  /** The target element to anchor the popover to. If null/undefined, the picker won't render */
  target?: HTMLElement | null;
  /** Callback function invoked when the picker should close (e.g., click outside) */
  onClose: () => void;
};

/**
 * Converts a color value to its hexadecimal representation.
 * If the color value is invalid or not provided, it returns null.
 *
 * @param {string} [value] - The color value to convert. Can be a hex string, color name, or another CSS color format.
 * @return {string} The hexadecimal representation of the color, or null if the input is invalid or not provided.
 */
export function colorToHex(value?: string | null | undefined): string | null {
  if (!value) return null;
  // already hex format
  if (/^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/.test(value)) return value;
  try {
    // parse string (`red`, ...) or any other css color format
    const parsed = Color(value);
    return parsed.alpha() < 1 ? parsed.hexa() : parsed.hex();
  } catch {
    return null;
  }
}

/**
 * A color picker component that displays a popover with color selection options.
 * Supports two modes: a lite mode with a simple color palette and a full Chrome-style picker.
 * 
 * Typically used with the `useColorPicker` hook which manages the popover state.
 */
export function ColorPicker({
  isOpen,
  target,
  onClose,
  lite,
  showAlpha,
  palette = DEFAULT_PALETTE,
  value,
  onChange,
}: ColorPickerProps & ColorPickerPopoverProps) {
  return (
    <Popper
      open={isOpen}
      shadow
      rounded
      target={target as HTMLElement}
      placement={lite ? "bottom" : "bottom-start"}
      arrow={false}
    >
      <ClickAwayListener onClickAway={onClose}>
        <Box>
          {lite ? (
            <Block
              colors={palette}
              color={value ?? DEFAULT_COLOR}
              onChange={onChange}
            />
          ) : (
            <Chrome
              inputType={ChromeInputType.HEXA}
              showTriangle={false}
              showAlpha={showAlpha}
              showEyeDropper={false}
              showColorPreview={false}
              color={value ?? DEFAULT_COLOR}
              onChange={onChange}
            />
          )}
        </Box>
      </ClickAwayListener>
    </Popper>
  );
}
