import { ThemeProvider } from "@axelor/ui";
import { Routes } from "./routes";

import "./styles/global.scss";

function App() {
  return (
    <ThemeProvider>
      <Routes />
    </ThemeProvider>
  );
}

export default App;
