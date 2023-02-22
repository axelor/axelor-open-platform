import { loadEnv, mergeConfig } from "vite";
import { defineConfig } from "vitest/config";
import viteConfig from "./vite.config";

let env = loadEnv("dev", process.cwd(), "");
let base = env.VITE_PROXY_CONTEXT ?? "/";

base = base.endsWith("/") ? base : `${base}/`;

export default mergeConfig(
  viteConfig,
  defineConfig({
    base,
    server: {
      proxy: ["callback", "logout", "img", "ws", "js", "websocket"].reduce(
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
