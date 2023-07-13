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
};

const proxyWs: ProxyOptions = {
  ...proxyAll,
  ws: true,
};

const proxyJs: ProxyOptions = {
  ...proxyAll,
  bypass(req, res, options) {
    if (/\/theme\/([^.]+)\.json/.test(req.url)) {
      return req.url;
    }
  },
};

export default mergeConfig(
  conf,
  defineConfig({
    plugins,
    base,
    server: {
      proxy: {
        [`${base}callback`]: proxyAll,
        [`${base}login`]: proxyAll,
        [`${base}logout`]: proxyAll,
        [`${base}img`]: proxyAll,
        [`${base}ws`]: proxyWs,
        [`${base}js`]: proxyJs,
        [`${base}websocket`]: proxyAll,
        [`${base}wkf-editor`]: proxyAll,
        [`${base}studio`]: proxyAll,
        [`${base}baml-editor`]: proxyAll,
      },
      fs: {
        // Allow serving files from one level up to the project root
        allow: ["..", "../../axelor-ui"],
      },
    },
  })
);
