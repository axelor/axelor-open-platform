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
    include: [
      "@babel/standalone",
      "react/jsx-runtime",
      "react-dnd-html5-backend",
      "jotai-immer",
      "jotai-optics",
      "dayjs",
      "dayjs/plugin/customParseFormat",
      "dayjs/plugin/relativeTime",
      "date-fns",
      "date-fns/locale/en-AU",
      "date-fns/locale/en-CA",
      "date-fns/locale/en-GB",
      "date-fns/locale/en-IN",
      "date-fns/locale/en-US",
      "date-fns/locale/fr",
      "date-fns/locale/fr-CA",
      "date-fns/locale/ja",
      "date-fns/locale/ru",
      "date-fns/locale/zh-CN",
      "lodash",
      "lodash/uniq",
      "lodash/isNumber",
      "lodash/isNaN",
      "lodash/isObject",
      "lodash/get",
      "lodash/isEmpty",
      "lodash/isString",
      "react-datepicker",
      "lodash/padStart",
      "lodash/map",
      "lodash/filter",
      "monaco-editor",
      "immer",
      "parse5",
    ],
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
    target: ["chrome89", "edge89", "firefox89", "safari15", "es2022"],
  },
});
