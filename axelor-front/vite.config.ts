import legacy from "@vitejs/plugin-legacy";
import react from "@vitejs/plugin-react";
import path from "path";
import fs from "fs";
import crypto from "crypto";
import { defineConfig } from "vite";
import svgr from "vite-plugin-svgr";
import monacoPkg from "monaco-editor/package.json" with { type: "json" };

const monacoHash = crypto
  .createHash("sha256")
  .update(JSON.stringify(monacoPkg))
  .digest()
  .toString("base64url")
  .slice(0, 8);
const monacoPath = `assets/monaco-${monacoHash}`;
const monacoNodePath = "node_modules/monaco-editor/min/vs";

// https://vitejs.dev/config/
export default defineConfig({
  base: "./",
  plugins: [
    react(),
    svgr({
      svgrOptions: {
        icon: true,
      },
    }),
    legacy({
      modernPolyfills: true,
      renderLegacyChunks: false,
    }),
    {
      name: "monaco-hash",
      writeBundle() {
        const sourceDir = path.resolve(__dirname, monacoNodePath);
        const targetDir = `dist/${monacoPath}`;
        fs.symlinkSync(sourceDir, targetDir, "dir");
      },
    },
  ],
  define: {
    "import.meta.env.MONACO_PATH": JSON.stringify(
      process.env.NODE_ENV === "production" ? monacoPath : monacoNodePath,
    ),
  },
  optimizeDeps: {
    entries: ["src/**/*.{ts,js,tsx,jsx,css,scss,html}"],
  },
  resolve: {
    alias: [
      {
        find: /^~(.*)/,
        replacement: "$1",
      },
      {
        find: /^@\/(.*)/,
        replacement: path.join(__dirname, "src", "$1"),
      },
    ],
  },
  build: {
    target: ["es2022"],
  },
});
