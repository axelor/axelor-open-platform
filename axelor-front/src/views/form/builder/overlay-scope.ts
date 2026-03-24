import { createContext, useContext } from "react";

export type FormOverlayState = {
  onOpenOverlay?: () => void;
  onCloseOverlay?: () => void;
};

const FormOverlayContext = createContext<FormOverlayState>({});

export const FormOverlayProvider = FormOverlayContext.Provider;

export function useFormOverlay() {
  return useContext(FormOverlayContext);
}
