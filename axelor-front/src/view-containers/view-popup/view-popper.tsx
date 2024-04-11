import {
  ClickAwayListener,
  ClickAwayListenerProps,
  Popper,
  PopperProps,
} from "@axelor/ui";
import { atom, getDefaultStore, useAtomValue, useSetAtom } from "jotai";
import { useCallback, useEffect, useId, useMemo } from "react";
import { dialogsActive } from "@/components/dialogs";

const popperAtom = atom<Record<string, boolean>>({});

export function handlePopper() {
  const poppers = getDefaultStore().get(popperAtom);

  // active popper ids
  const ids = Object.keys(poppers).filter((id) => poppers[id] === true);

  function setPoppers(active: boolean) {
    getDefaultStore().set(
      popperAtom,
      ids.reduce(
        (poppers, id) => ({ ...poppers, [id]: active }),
        getDefaultStore().get(popperAtom),
      ),
    );
  }

  // disable active poppers
  setPoppers(false);

  // callback to reset active poppers
  return () => {
    setPoppers(true);
  };
}

export function ViewPopper({
  children,
  onClose,
  ...popperProps
}: PopperProps & {
  children: ClickAwayListenerProps["children"];
  onClose: ClickAwayListenerProps["onClickAway"];
}) {
  const { open } = popperProps;

  const id = useId();
  const setPoppers = useSetAtom(popperAtom);
  const active = useAtomValue(
    useMemo(() => atom((get) => get(popperAtom)[id]), [id]),
  );

  useEffect(() => {
    if (open) {
      setPoppers((poppers) => ({ ...poppers, [id]: true }));
    } else {
      setPoppers((poppers) => {
        delete poppers[id];
        return { ...poppers };
      });
    }
  }, [id, open, setPoppers]);

  const handleClose = useCallback(
    (e: Event) => active && !dialogsActive() && onClose?.(e),
    [active, onClose],
  );

  return (
    <Popper bg="body" {...popperProps}>
      <ClickAwayListener onClickAway={handleClose}>
        {children}
      </ClickAwayListener>
    </Popper>
  );
}
