import {
  ChangeEvent,
  FocusEvent,
  InputHTMLAttributes,
  KeyboardEvent,
  Ref,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { Input, InputProps } from "@axelor/ui";

import { useMaskInput } from "./use-mask-input";
import { Mask, PLACEHOLDER } from "./types";

export { PLACEHOLDER } from "./types";
export type { Mask, MaskArray, MaskFunction } from "./types";

interface MaskedFieldProps {
  /**
   * The mask pattern that defines the input format. Each element is either:
   * - A **string** — rendered as a literal (fixed) character that the user
   *   cannot change (e.g. `"/"`, `"-"`, `"("`).
   * - A **RegExp** — represents a user-fillable slot; the typed character
   *   must match the regex to be accepted (e.g. `/\d/` for digits only).
   *
   * **Array form** — memoize the array to avoid re-processing on every render:
   * ```tsx
   * // US phone: (___) ___-____
   * const phoneMask = useMemo(
   *   () => ["(", /[1-9]/, /\d/, /\d/, ")", " ", /\d/, /\d/, /\d/, "-", /\d/, /\d/, /\d/, /\d/],
   *   [],
   * );
   * <MaskedInput mask={phoneMask} />
   * ```
   *
   * **Function form** — `(rawValue, config?) => MaskArray | false`.
   * Receives the current raw input and returns a mask array, allowing
   * the mask to change dynamically (e.g. varying length for credit card
   * numbers).
   *
   * **`false`** — disables masking entirely; the input behaves as a
   * plain text field.
   */
  mask: Mask;

  /**
   * Whether to display placeholder characters for unfilled positions.
   *
   * - `true` (default) — shows the full mask pattern as the user types
   *   (e.g. `(12_) ___-____`).
   * - `false` — only shows characters the user has actually entered,
   *   though mask literals are still added automatically as the user
   *   types past them (e.g. typing `12` yields `(12`).
   *
   * @default true
   */
  guide?: boolean;

  /**
   * The character used for unfilled positions in the mask.
   *
   * Must **not** be a character that appears as a literal in the mask
   * definition, otherwise the engine cannot distinguish placeholders from
   * fixed characters.
   *
   * @default "_"
   *
   * @example
   * // Invisible placeholders using Unicode en-space
   * placeholderChar={"\u2000"}
   */
  placeholderChar?: string;

  /**
   * Whether existing characters should stay in place when inserting or
   * deleting, rather than shifting left/right.
   *
   * Useful for fixed-format inputs like dates where each character slot
   * has a specific meaning.
   *
   * @default true
   */
  keepCharPositions?: boolean;

  /**
   * Controls whether the mask pattern is visible when the field is empty.
   *
   * - `true` — the empty placeholder is always shown (e.g. `___-____`).
   * - `false` — the placeholder is hidden when the field is empty.
   * - `undefined` (default) — the mask is shown on focus and hidden on blur.
   */
  showMask?: boolean;

  /** Ref forwarded to the underlying `<input>` element. */
  ref?: Ref<HTMLInputElement | null>;
}

type NativeInputProps = Omit<
  InputHTMLAttributes<HTMLInputElement>,
  keyof InputProps | "color"
>;

type MaskedInputProps = MaskedFieldProps & InputProps & NativeInputProps;

function moveCaretToStart(el: HTMLInputElement, placeholderChar: string) {
  const { value } = el;
  if (typeof el.selectionStart === "number") {
    const ind = value.indexOf(placeholderChar);
    if (ind > -1) {
      el.selectionStart = el.selectionEnd = ind;
    }
  }
}

/**
 * A controlled text input that enforces a character-by-character mask pattern.
 *
 * Wraps the `@axelor/ui` `<Input>` component and applies mask conforming,
 * caret management, and placeholder rendering on every keystroke.
 *
 * @example
 * ```tsx
 * // Date input: DD/MM/YYYY
 * const dateMask = useMemo(
 *   () => [/\d/, /\d/, "/", /\d/, /\d/, "/", /\d/, /\d/, /\d/, /\d/],
 *   [],
 * );
 * <MaskedInput mask={dateMask} value={date} onChange={handleChange} />
 * ```
 *
 * All standard `<input>` and `InputProps` attributes (e.g. `placeholder`,
 * `disabled`, `className`) are forwarded to the underlying element.
 */
export function MaskedInput(props: MaskedInputProps) {
  const {
    mask,
    guide = true,
    placeholderChar = PLACEHOLDER,
    keepCharPositions = true,
    showMask: showMaskProp,
    ref,
    ...inputProps
  } = props;

  const { value, onBlur, onChange, onFocus, onKeyDown, ...rest } = inputProps;

  const frameRef = useRef<number>(0);
  const [showMaskState, setShowMaskState] = useState(false);
  const isShowMaskControlled = showMaskProp !== undefined;
  const showMask = isShowMaskControlled ? !!showMaskProp : showMaskState;

  const {
    inputRef,
    maskedValue,
    onChange: handleMaskedChange,
  } = useMaskInput(value as string | number | null | undefined, {
    mask,
    guide,
    placeholderChar,
    keepCharPositions,
    showMask,
  });

  useEffect(() => {
    return () => cancelAnimationFrame(frameRef.current);
  }, []);

  const composedRef = useCallback(
    (node: HTMLInputElement | null) => {
      inputRef.current = node;
      if (typeof ref === "function") {
        ref(node);
      } else if (ref) {
        (ref as React.RefObject<HTMLInputElement | null>).current = node;
      }
    },
    [inputRef, ref],
  );

  function handleMaskedChangeWithEvent(event: ChangeEvent<HTMLInputElement>) {
    handleMaskedChange(event);
    onChange?.(event);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (["Delete", "Backspace"].includes(event.key)) {
      const el = event.target as HTMLInputElement;
      if (
        el &&
        el.selectionStart === 0 &&
        el.selectionEnd === (el.value || "").length
      ) {
        el.value = "";
        handleMaskedChangeWithEvent({
          ...event,
          target: el,
          currentTarget: el,
        } as unknown as ChangeEvent<HTMLInputElement>);
      }
    }

    onKeyDown?.(event);
  }

  function handleFocus(event: FocusEvent<HTMLInputElement>) {
    if (!isShowMaskControlled) {
      setShowMaskState(true);
    }
    const inputEl = event.currentTarget;
    frameRef.current = requestAnimationFrame(() =>
      moveCaretToStart(inputEl, placeholderChar),
    );
    onFocus?.(event);
  }

  function handleBlur(event: FocusEvent<HTMLInputElement>) {
    if (!isShowMaskControlled) {
      setShowMaskState(false);
    }
    onBlur?.(event);
  }

  return (
    <Input
      {...rest}
      ref={composedRef}
      value={maskedValue}
      onKeyDown={handleKeyDown}
      onFocus={handleFocus}
      onBlur={handleBlur}
      onChange={handleMaskedChangeWithEvent}
    />
  );
}
