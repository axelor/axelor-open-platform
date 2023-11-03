import react from "@vitejs/plugin-react";
import jotaiDebugLabel from "jotai/babel/plugin-debug-label";
import jotaiReactRefresh from "jotai/babel/plugin-react-refresh";
import { ProxyOptions, loadEnv, mergeConfig } from "vite";
import { UserConfig, defineConfig } from "vitest/config";
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

const proxyAll: ProxyOptions = {
  target: env.VITE_PROXY_TARGET,
  changeOrigin: true,
  bypass(req, res, options) {
    if (req.url === base || req.url === base + "index.html" || req.url.startsWith(base + "src/")
      || req.url.startsWith(base + "@fs/") || req.url === base + "@react-refresh"
      || req.url.startsWith(base + "@id/") || req.url.startsWith(base + "@vite/")
      || req.url.startsWith(base + "node_modules/") || /\/theme\/([^.]+)\.json/.test(req.url)) {
      return req.url;
    }
  },
};

const proxyWs: ProxyOptions = {
  target: env.VITE_PROXY_TARGET,
  changeOrigin: true,
  ws: true,
};

export default mergeConfig(
  conf,
  defineConfig({
    plugins,
    base,
    server: {
      proxy: {
        [`${base}websocket`]: proxyWs,
        [`${base}`]: proxyAll,
      },
      fs: {
        // Allow serving files from one level up to the project root
        allow: ["..", "../../axelor-ui"],
      },
    },
  })
);
