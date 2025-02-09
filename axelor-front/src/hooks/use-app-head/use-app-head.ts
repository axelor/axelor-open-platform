import { useEffect } from "react";

import { useAppSettings } from "@/hooks/use-app-settings";

export function useAppHead() {
  const { name, description, isReady } = useAppSettings();

  // Document title
  useEffect(() => {
    if (isReady) {
      setTitle(name, description);
    }
  }, [name, description, isReady]);

  // For favicon, use browser color scheme, not app theme mode.
  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    handleIcon(mediaQuery);
    mediaQuery.addEventListener("change", handleIcon);

    return () => mediaQuery.removeEventListener("change", handleIcon);
  }, []);
}

function setTitle(name: string, description: string) {
  document.title = `${name} – ${description}`;
}

function setIcon(icon: string | undefined) {
  const elem = document.querySelector(
    "head > link[rel='shortcut icon']",
  ) as HTMLLinkElement;

  if (icon && elem && elem.href !== icon) {
    elem.href = icon;
  }
}

const handleIcon = (e: MediaQueryListEvent | MediaQueryList) => {
  setIcon(`ws/public/app/icon${e.matches ? "?mode=dark" : ""}`);
};
