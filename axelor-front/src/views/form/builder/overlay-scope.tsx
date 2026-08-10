import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
} from "react";

import { FocusTrap, FocusTrapProps } from "@axelor/ui";

export type FormOverlayState = {
  onOpenOverlay?: () => void;
  onCloseOverlay?: () => void;
};

const FormOverlayContext = createContext<FormOverlayState>({});

export const FormOverlayProvider = FormOverlayContext.Provider;

export function useFormOverlay() {
  return useContext(FormOverlayContext);
}

export function FormOverlayFocusTrap({
  enabled = true,
  ...props
}: FocusTrapProps) {
  const [openOverlays, setOpenOverlays] = useState(0);

  const onOpenOverlay = useCallback(() => {
    setOpenOverlays((count) => count + 1);
  }, []);

  const onCloseOverlay = useCallback(() => {
    setOpenOverlays((count) => Math.max(0, count - 1));
  }, []);

  const overlayValue = useMemo(
    () => ({ onOpenOverlay, onCloseOverlay }),
    [onOpenOverlay, onCloseOverlay],
  );

  return (
    <FormOverlayProvider value={overlayValue}>
      <FocusTrap {...props} enabled={enabled && openOverlays === 0} />
    </FormOverlayProvider>
  );
}
