import { useCallback, useEffect } from "react";

import { dialogsActive } from "@/components/dialogs";
import { useSelectViewState, useViewTab } from "@/view-containers/views/scope";
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

const alwaysTrue = () => true;

let getKeys: (options: Options) => {
  ctrlKey?: boolean;
  altKey?: boolean;
  shiftKey?: boolean;
  metaKey?: boolean;
};

if (isMac) {
  // Command (Meta) is used instead of Control.
  // Option (Alt) cannot be combined alone with alpha keys for shortcuts,
  // as it might be used to type special characters on some layouts.
  // Ctrl -> ⌘
  // Alt -> ⌘ + ⌥ (if alpha key)
  getKeys = (options: Options) => {
    const { key, ctrlKey, altKey, shiftKey } = options;
    return {
      metaKey: ctrlKey ?? (altKey && /^[a-z]$/i.test(key)),
      altKey,
      shiftKey,
    };
  };
  // Prevent conflict with Mac-specific navigation shortcuts.
} else {
  getKeys = (options: Options) => {
    const { ctrlKey, altKey, shiftKey } = options;
    return {
      ctrlKey,
      altKey,
      shiftKey,
    };
  };
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

export function useShortcuts({
  viewType,
  canHandle: canHandleProp = alwaysTrue,
  onNew,
  onEdit,
  onSave,
  onCopy,
  onDelete,
  onRefresh,
  onFocus,
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
}) {
  const { active } = useTabs();
  const tab = useViewTab();
  const currentViewType = useSelectViewState(useCallback((x) => x.type, []));

  const canHandle = useCallback(
    (e: KeyboardEvent) =>
      active === tab && currentViewType === viewType && canHandleProp(e),
    [active, tab, currentViewType, viewType, canHandleProp]
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
    action: useCallback(() => {
      if (!dialogsActive()) {
        onDelete?.();
      }
    }, [onDelete]),
  });

  useShortcut({
    key: "r",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => {
      if (!dialogsActive()) {
        onRefresh?.();
      }
    }, [onRefresh]),
  });

  useShortcut({
    key: "g",
    altKey: true,
    canHandle,
    action: useCallback(() => onFocus?.(), [onFocus]),
  });
}

export function useNavShortcuts({
  viewType,
  canHandle: canHandleProp = alwaysTrue,
  onNext,
  onPrev,
}: {
  viewType: string;
  canHandle?: (e: KeyboardEvent) => boolean;
  onPrev?: () => void;
  onNext?: () => void;
}) {
  const { active } = useTabs();
  const tab = useViewTab();
  const currentViewType = useSelectViewState(useCallback((x) => x.type, []));

  const canHandle = useCallback(
    (e: KeyboardEvent) =>
      !e.repeat &&
      active === tab &&
      currentViewType === viewType &&
      canHandleProp(e),
    [active, tab, currentViewType, viewType, canHandleProp]
  );

  useShortcut({
    key: "PageUp",
    altKey: true,
    canHandle,
    action: useCallback(() => {
      if (!dialogsActive()) {
        onPrev?.();
      }
    }, [onPrev]),
  });

  useShortcut({
    key: "PageDown",
    altKey: true,
    canHandle,
    action: useCallback(() => {
      if (!dialogsActive()) {
        onNext?.();
      }
    }, [onNext]),
  });
}
