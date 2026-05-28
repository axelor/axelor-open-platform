import {
  ChangeEvent,
  useCallback,
  useLayoutEffect,
  useRef,
  useState,
} from "react";

import {
  adjustCaretPosition,
  coerceValue,
  conformToMask,
  convertMaskToPlaceholder,
  resolveMask,
} from "./mask-engine";
import { Mask, PLACEHOLDER } from "./types";

export interface MaskInputConfig {
  mask: Mask;
  guide?: boolean;
  placeholderChar?: string;
  keepCharPositions?: boolean;
  showMask?: boolean;
}

const CARET_DIRECTION = "none";
const isAndroidDevice =
  typeof navigator !== "undefined" && /Android/i.test(navigator.userAgent);
const runInNextFrame =
  typeof requestAnimationFrame !== "undefined"
    ? requestAnimationFrame
    : (cb: () => void) => setTimeout(cb, 0);

function setCaretPosition(inputElement: HTMLInputElement, position: number) {
  if (
    typeof document === "undefined" ||
    document.activeElement !== inputElement ||
    typeof inputElement.setSelectionRange !== "function"
  ) {
    return;
  }

  const setPosition = () =>
    inputElement.setSelectionRange(position, position, CARET_DIRECTION);

  if (isAndroidDevice) {
    runInNextFrame(setPosition);
  } else {
    setPosition();
  }
}

export function useMaskInput(
  value: string | number | null | undefined,
  {
    mask,
    guide = true,
    placeholderChar = PLACEHOLDER,
    keepCharPositions,
    showMask,
  }: MaskInputConfig,
) {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [maskedValue, setMaskedValue] = useState("");
  const [updateKey, setUpdateKey] = useState(0);
  const caretRef = useRef<number>(0);

  const stateRef = useRef({
    previousConformedValue: "",
    previousPlaceholder: "",
    previousShowMask: showMask,
  });

  const update = useCallback(
    (rawValue?: string | number | null) => {
      const inputElement = inputRef.current;

      const valueToConform =
        rawValue === undefined ? (inputElement?.value ?? "") : rawValue;

      const state = stateRef.current;
      const showMaskChanged = showMask !== state.previousShowMask;
      if (valueToConform === state.previousConformedValue && !showMaskChanged)
        return;
      state.previousShowMask = showMask;

      const rawValueString = coerceValue(valueToConform);
      const selectionEnd = inputElement?.selectionEnd ?? rawValueString.length;
      const previousConformedValue = state.previousConformedValue;
      const previousPlaceholder = state.previousPlaceholder;

      const maskArray = resolveMask(mask, rawValueString, {
        currentCaretPosition: selectionEnd,
        previousConformedValue,
        placeholderChar,
      });

      if (maskArray === false) {
        state.previousConformedValue = rawValueString;
        setMaskedValue(rawValueString);
        return;
      }

      const placeholder = convertMaskToPlaceholder(maskArray, placeholderChar);

      const conformedValue = conformToMask(rawValueString, maskArray, {
        previousConformedValue,
        guide,
        placeholderChar,
        placeholder,
        currentCaretPosition: selectionEnd,
        keepCharPositions,
      });

      const caretPos = adjustCaretPosition({
        previousConformedValue,
        previousPlaceholder,
        conformedValue,
        placeholder,
        rawValue: rawValueString,
        currentCaretPosition: selectionEnd,
        placeholderChar,
      });

      const maskShown =
        conformedValue === placeholder && caretPos === 0
          ? showMask
            ? placeholder
            : ""
          : conformedValue;

      state.previousConformedValue = maskShown;
      state.previousPlaceholder = placeholder;

      setMaskedValue(maskShown);
      caretRef.current = caretPos;
      setUpdateKey((k) => k + 1);
    },
    [mask, guide, placeholderChar, keepCharPositions, showMask],
  );

  useLayoutEffect(() => {
    const inputElement = inputRef.current;
    if (inputElement) {
      setCaretPosition(inputElement, caretRef.current);
    }
  }, [updateKey]);

  useLayoutEffect(() => {
    update(value);
  }, [update, value]);

  const onChange = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      update(event.target.value);
      // Sync conformed value back to the event target so that parent
      // onChange handlers see the conformed value, not the raw browser value.
      event.target.value = stateRef.current.previousConformedValue;
    },
    [update],
  );

  return { inputRef, maskedValue, onChange };
}
