import { useSelectViewState, useViewTab } from "@/view-containers/views/scope";
import { useCallback, useEffect } from "react";
import { useTabs } from "../use-tabs";

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

const defaultCanHandle = () => true;

export function useShortcuts({
  viewType,
  canHandle: canHandleProp = defaultCanHandle,
  onNew,
  onEdit,
  onSave,
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
    shiftKey: true,
    canHandle,
    action: useCallback(() => onFocus?.(), [onFocus]),
  });

  useShortcut({
    key: "j",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onPrev?.(), [onPrev]),
  });

  useShortcut({
    key: "k",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onNext?.(), [onNext]),
  });
}
