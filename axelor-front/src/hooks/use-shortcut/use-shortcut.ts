import { useCallback, useEffect } from "react";

export const isMac = /Mac OS/i.test(navigator.userAgent);

export type Options = {
  key: string;
  altKey?: boolean;
  ctrlKey?: boolean;
  shiftKey?: boolean;
  canHandle?: (e: KeyboardEvent) => boolean;
  action: (e: KeyboardEvent) => void;
};

const ctrlOrMetaKey = isMac ? "metaKey" : "ctrlKey";

const compareKey = new Intl.Collator(undefined, { sensitivity: "base" })
  .compare;

export function useShortcut(options: Options) {
  const { key, altKey, ctrlKey, shiftKey, canHandle, action } = options;
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (
        compareKey(e.key, key) === 0 &&
        (altKey === undefined || e.altKey === altKey) &&
        (ctrlKey === undefined || e[ctrlOrMetaKey] === ctrlKey) &&
        (shiftKey === undefined || e.shiftKey === shiftKey) &&
        (canHandle === undefined || canHandle(e))
      ) {
        e.stopPropagation();
        e.preventDefault();
        action(e);
      }
    },
    [key, altKey, ctrlKey, shiftKey, canHandle, action]
  );

  useEffect(() => {
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [key, action, handleKeyDown]);
}
