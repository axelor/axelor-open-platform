import react from "@vitejs/plugin-react";
import jotaiDebugLabel from "jotai/babel/plugin-debug-label";
import jotaiReactRefresh from "jotai/babel/plugin-react-refresh";
import path from "node:path";
import { ProxyOptions, loadEnv, mergeConfig } from "vite";
import { ViteUserConfig, defineConfig } from "vitest/config";
import viteConfig from "./vite.config";

const env = loadEnv("dev", process.cwd(), "");
let base = env.VITE_PROXY_CONTEXT ?? "/";

base = base.endsWith("/") ? base : `${base}/`;
const unslashedBase = base === "/" ? base : base.slice(0, -1);

const { plugins, ...conf } = viteConfig as ViteUserConfig;

// replace react plugin
plugins[0] = react({
  babel: {
    plugins: [jotaiDebugLabel, jotaiReactRefresh],
  },
});

const proxyAll: ProxyOptions = {
  target: env.VITE_PROXY_TARGET,
  changeOrigin: true,
  xfwd: true,
  bypass(req, res, options) {
    // Compare pathname without any query params
    const pathname = req.url.split("?")[0];
    if (
      pathname === base ||
      pathname === base + "index.html" ||
      pathname.startsWith(base + "src/") ||
      pathname.startsWith(base + "@fs/") ||
      pathname === base + "@react-refresh" ||
      pathname.startsWith(base + "@id/") ||
      pathname.startsWith(base + "@vite/") ||
      pathname.startsWith(base + "node_modules/") ||
      /\/theme\/([^.]+)\.json/.test(pathname)
    ) {
      return pathname;
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
    resolve: {
      ...conf?.resolve,
      alias: {
        ...conf?.resolve?.alias,
        react: path.resolve(__dirname, "./node_modules/react"),
        "react-dom": path.resolve(__dirname, "./node_modules/react-dom"),
      },
      dedupe: [...(conf?.resolve?.dedupe ?? []), "react", "react-dom"],
    },
    server: {
      proxy: {
        [`${base}websocket`]: proxyWs,
        [base]: proxyAll,
        [unslashedBase]: proxyAll,
      },
      fs: {
        // Allow serving files from one level up to the project root
        allow: ["..", "../../axelor-ui"],
      },
    },
  }),
);
