import eslint from "@eslint/js";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import globals from "globals";
import tseslint from "typescript-eslint";

export default tseslint.config(
  { ignores: ["dist"] },
  {
    extends: [
      // https://github.com/eslint/eslint/blob/main/packages/js/src/configs/eslint-recommended.js
      eslint.configs.recommended,
      // https://typescript-eslint.io/users/configs#recommended
      ...tseslint.configs.recommended,
    ],
    files: ["**/*.{ts,tsx}"],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
    },
    rules: {
      // https://react.dev/reference/rules/rules-of-hooks
      ...reactHooks.configs.recommended.rules,
      "react-hooks/react-compiler": "warn",
      // Validate that components can safely be updated with fast refresh.
      "react-refresh/only-export-components": [
        "warn",
        { allowConstantExport: true },
      ],
      // Require const declarations for variables that are never reassigned after declared
      "prefer-const": "warn",
      // Disallow variable declarations from shadowing variables declared in the outer scope
      // https://typescript-eslint.io/rules/no-shadow/
      "no-shadow": "off",
      "@typescript-eslint/no-shadow": "warn",
      // Disallow specified modules when loaded by import
      "no-restricted-imports": [
        "error",
        {
          paths: [
            {
              name: "lodash",
              message: "Import [module] from lodash/[module] instead",
            },
          ],
        },
      ],
      // Warn on `any` type
      "@typescript-eslint/no-explicit-any": "warn",
      // Only warn on unused expressions
      "@typescript-eslint/no-unused-expressions": "warn",
      // Only warn on unused variables, and ignore variables starting with `_`
      "@typescript-eslint/no-unused-vars": [
        "warn",
        {
          argsIgnorePattern: "^_",
          varsIgnorePattern: "^_",
        },
      ],
      "@typescript-eslint/no-restricted-types": "warn",
    },
  },
);
