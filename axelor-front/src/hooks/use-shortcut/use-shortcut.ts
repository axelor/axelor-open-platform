import { useCallback, useEffect } from "react";

import { useSelectViewState, useViewTab } from "@/view-containers/views/scope";
import { isInputFocused } from "@/views/form";
import { useTabs } from "../use-tabs";

export const isMac = /Mac OS/i.test(navigator.userAgent);

export type Options = {
  key: string;
  ctrlKey?: boolean;
  altKey?: boolean;
  shiftKey?: boolean;
  canHandle?: (e: KeyboardEvent) => boolean;
  action: (e: KeyboardEvent) => void;
};

let getKeys: (options: Options) => {
  ctrlKey?: boolean;
  altKey?: boolean;
  shiftKey?: boolean;
  metaKey?: boolean;
};
let inputSensitive: () => boolean;

if (isMac) {
  // Command (Meta) is used instead of Control.
  // Option (Alt) cannot be used alone for shortcuts,
  // as it might be used to type special characters on some layouts.
  // Ctrl -> ⌘
  // Alt -> ⌘ + ⌥
  getKeys = (options: Options) => {
    const { ctrlKey, shiftKey, altKey } = options;
    return {
      metaKey: ctrlKey ?? altKey,
      altKey,
      shiftKey,
    };
  };
  // Prevent conflict with Mac-specific navigation shortcuts.
  inputSensitive = isInputFocused;
} else {
  getKeys = (options: Options) => {
    const { ctrlKey, shiftKey, altKey } = options;
    return {
      ctrlKey,
      altKey,
      shiftKey,
    };
  };
  inputSensitive = () => false;
}

const compareKey = new Intl.Collator(undefined, { sensitivity: "base" })
  .compare;

export function useShortcut(options: Options) {
  const { key, canHandle, action } = options;
  const { ctrlKey, altKey, shiftKey, metaKey } = getKeys(options);
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (
        compareKey(e.key, key) === 0 &&
        (ctrlKey === undefined || e.ctrlKey === ctrlKey) &&
        (altKey === undefined || e.altKey === altKey) &&
        (shiftKey === undefined || e.shiftKey === shiftKey) &&
        (metaKey === undefined || e.metaKey === metaKey) &&
        (canHandle === undefined || canHandle(e))
      ) {
        e.stopPropagation();
        e.preventDefault();
        action(e);
      }
    },
    [key, ctrlKey, shiftKey, altKey, metaKey, canHandle, action]
  );

  useEffect(() => {
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [key, action, handleKeyDown]);
}

const defaultCanHandle = () => true;

export function useShortcuts({
  viewType,
  canHandle: canHandleProp = defaultCanHandle,
  onNew,
  onEdit,
  onSave,
  onCopy,
  onDelete,
  onRefresh,
  onFocus,
  onNext,
  onPrev,
}: {
  viewType: string;
  canHandle?: (e: KeyboardEvent) => boolean;
  onNew?: () => void;
  onEdit?: () => void;
  onSave?: () => void;
  onCopy?: () => void;
  onDelete?: () => void;
  onRefresh?: () => void;
  onFocus?: () => void;
  onPrev?: () => void;
  onNext?: () => void;
}) {
  const { active } = useTabs();
  const tab = useViewTab();
  const currentViewType = useSelectViewState(useCallback((x) => x.type, []));

  const canHandle = useCallback(
    (e: KeyboardEvent) =>
      active === tab && currentViewType === viewType && canHandleProp(e),
    [active, tab, currentViewType, viewType, canHandleProp]
  );

  const canHandleInputSensitive = useCallback(
    (e: KeyboardEvent) => canHandle(e) && !inputSensitive(),
    [canHandle]
  );

  useShortcut({
    key: "Insert",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onNew?.(), [onNew]),
  });

  useShortcut({
    key: "e",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onEdit?.(), [onEdit]),
  });

  useShortcut({
    key: "s",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onSave?.(), [onSave]),
  });

  useShortcut({
    key: "d",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onCopy?.(), [onCopy]),
  });

  useShortcut({
    key: "Delete",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onDelete?.(), [onDelete]),
  });

  useShortcut({
    key: "r",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onRefresh?.(), [onRefresh]),
  });

  useShortcut({
    key: "g",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onFocus?.(), [onFocus]),
  });

  useShortcut({
    key: "ArrowLeft",
    ctrlKey: true,
    canHandle: canHandleInputSensitive,
    action: useCallback(() => onPrev?.(), [onPrev]),
  });

  useShortcut({
    key: "ArrowRight",
    ctrlKey: true,
    canHandle: canHandleInputSensitive,
    action: useCallback(() => onNext?.(), [onNext]),
  });
}
