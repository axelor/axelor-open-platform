import { ThemeProvider } from "@axelor/ui";
import { Routes } from "./routes";

import { useAppTheme } from "./hooks/use-app-theme";

import "./styles/global.scss";

function App() {
  const theme = useAppTheme();
  return (
    <ThemeProvider theme={theme}>
      <Routes />
    </ThemeProvider>
  );
}

export default App;
