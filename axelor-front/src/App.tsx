import { ThemeProvider } from "@axelor/ui";
import { Routes } from "./routes";

function App() {
  return (
    <ThemeProvider>
      <Routes />
    </ThemeProvider>
  );
}

export default App;
