import { ThemeProvider } from "@axelor/ui";
import { useAppThemeOption } from "./hooks/use-app-theme";
import { Routes } from "./routes";

import "./styles/global.scss";

function App() {
  const { theme, options } = useAppThemeOption();
  return (
    <ThemeProvider theme={theme} options={options}>
      <Routes />
    </ThemeProvider>
  );
}

export default App;
