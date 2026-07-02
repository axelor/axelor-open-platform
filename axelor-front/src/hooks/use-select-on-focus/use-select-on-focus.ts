import { useCallback, useEffect, useRef } from "react";

export function useSelectOnFocus() {
  const frameRef = useRef(0);

  useEffect(() => {
    return () => cancelAnimationFrame(frameRef.current);
  }, []);

  // Deferred a frame: WebKit applies its native caret placement after our
  // listeners run, silently overriding a synchronous `.select()` call.
  const selectDeferred = useCallback((el: HTMLInputElement) => {
    cancelAnimationFrame(frameRef.current);
    frameRef.current = requestAnimationFrame(() => {
      if (document.activeElement === el) {
        el.select();
      }
    });
  }, []);

  const onFocus = useCallback(
    (e: React.FocusEvent<HTMLInputElement>) => {
      selectDeferred(e.target);
    },
    [selectDeferred],
  );

  return { onFocus };
}
