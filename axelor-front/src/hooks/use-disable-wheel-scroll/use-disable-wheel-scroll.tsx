import { useEffect, useState } from "react";

/**
 * Custom React hook to disable mouse wheel scrolling on a specific `<input>` element
 * while it is focused. This is commonly used to prevent users from accidentally
 * changing the value of number input fields with the mouse wheel.
 *
 * ## Usage
 * You can use this hook in two ways:
 *
 * 1. **Pass an input element directly:**
 * ```tsx
 * const inputRef = useRef<HTMLInputElement>(null);
 * useDisableWheelScroll(inputRef.current);
 * ```
 *
 * 2. **Let the hook manage the input reference:**
 * ```tsx
 * const [inputElement, setInputElement] = useDisableWheelScroll();
 *
 * return <input ref={setInputElement} type="number" />;
 * ```
 *
 * @param givenInputElement Optional HTMLInputElement to disable scroll on.
 * @returns A tuple of `[inputElement, setInputElement]` for manual input ref management.
 */
export function useDisableWheelScroll(givenInputElement?: HTMLInputElement) {
  const [inputElement, setInputElement] = useState<HTMLInputElement | null>(
    null,
  );

  useEffect(() => {
    const input = givenInputElement ?? inputElement;
    if (!input) return;

    const onWheel = (e: WheelEvent) => {
      if (document.activeElement === input) {
        e.preventDefault();
      }
    };

    input.addEventListener("wheel", onWheel, { passive: false });

    return () => input.removeEventListener("wheel", onWheel);
  }, [inputElement, givenInputElement]);

  return [inputElement, setInputElement] as const;
}
