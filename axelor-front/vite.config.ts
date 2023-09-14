import react from "@vitejs/plugin-react";
import path from "path";
import { defineConfig } from "vite";
import svgr from "vite-plugin-svgr";

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
  ],
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
    target: ["es2022", "chrome89", "edge89", "firefox89", "safari15"],
  },
});
