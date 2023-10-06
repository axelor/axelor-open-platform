import { useEffect } from "react";

import { ThemeProvider } from "@axelor/ui";

import { useAppLang } from "./hooks/use-app-lang";
import { useAppThemeOption } from "./hooks/use-app-theme";
import { Routes } from "./routes";

import "./styles/global.scss";

function App() {
  const { theme, options } = useAppThemeOption();
  const { dir, lang } = useAppLang();

  useEffect(() => {
    document.documentElement.dir = lang;
    document.documentElement.dir = dir;
  }, [dir, lang]);

  return (
    <ThemeProvider dir={dir} theme={theme} options={options}>
      <Routes />
    </ThemeProvider>
  );
}

export default App;
