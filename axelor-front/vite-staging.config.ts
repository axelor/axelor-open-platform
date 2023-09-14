import { mergeConfig } from "vite";
import { defineConfig } from "vitest/config";
import viteConfig from "./vite.config";

export default mergeConfig(
  viteConfig,
  defineConfig({
    resolve: {
      alias: [
        {
          find: /^(react-dom)$/,
          replacement: "react-dom/profiling",
        },
      ],
    },
    build: {
      sourcemap: true,
      minify: false,
    },
  })
);
