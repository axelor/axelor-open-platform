import { ThemeProvider } from "@axelor/ui";
import { useAppThemeOption } from "./hooks/use-app-theme";
import { Routes } from "./routes";

import "./styles/global.scss";

function App() {
  const { theme, loading, options } = useAppThemeOption();
  if (loading) return null;
  return (
    <ThemeProvider theme={theme} options={options}>
      <Routes />
    </ThemeProvider>
  );
}

export default App;
