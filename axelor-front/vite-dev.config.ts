import react from "@vitejs/plugin-react";
import jotaiDebugLabel from "jotai/babel/plugin-debug-label";
import jotaiReactRefresh from "jotai/babel/plugin-react-refresh";
import { loadEnv, mergeConfig } from "vite";
import { defineConfig, UserConfig } from "vitest/config";
import viteConfig from "./vite.config";

let env = loadEnv("dev", process.cwd(), "");
let base = env.VITE_PROXY_CONTEXT ?? "/";

base = base.endsWith("/") ? base : `${base}/`;

const { plugins, ...conf } = viteConfig as UserConfig;

// replace react plugin
plugins[0] = react({
  babel: {
    plugins: [jotaiDebugLabel, jotaiReactRefresh],
  },
});

export default mergeConfig(
  conf,
  defineConfig({
    plugins,
    base,
    server: {
      proxy: ["callback", "login", "logout", "img", "ws", "js", "websocket"].reduce(
        (prev, path) => ({
          ...prev,
          [`${base}${path}`]: {
            target: env.VITE_PROXY_TARGET,
            changeOrigin: true,
            ws: path === "websocket",
          },
        }),
        {}
      ),
      fs: {
        // Allow serving files from one level up to the project root
        allow: ["..", "../../axelor-ui"],
      },
    },
  })
);
