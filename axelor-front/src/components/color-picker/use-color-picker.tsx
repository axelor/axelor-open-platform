import { useCallback, useState } from "react";

export type ColorPickerOptions = {
  /** Callback function invoked when the color picker is closed */
  onClose?: () => void;
};

/**
 * Hook for managing color picker popover state.
 *
 * Provides functions to open and close the color picker, along with
 * props to pass to the ColorPicker component.
 */
export function useColorPicker({ onClose }: ColorPickerOptions = {}) {
  const [target, setTarget] = useState<HTMLElement | null>(null);

  const open = useCallback((targetElement: HTMLElement) => {
    setTarget(targetElement);
  }, []);

  const close = useCallback(() => {
    setTarget(null);
    onClose?.();
  }, [onClose]);

  const isOpen = Boolean(target);

  return { open, pickerPopoverProps: { isOpen, target, onClose: close } };
}
