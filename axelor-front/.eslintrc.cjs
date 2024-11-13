module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    // https://github.com/eslint/eslint/blob/main/packages/js/src/configs/eslint-recommended.js
    "eslint:recommended",
    // https://typescript-eslint.io/users/configs#recommended
    "plugin:@typescript-eslint/recommended",
    // https://react.dev/reference/rules/rules-of-hooks
    "plugin:react-hooks/recommended",
  ],
  ignorePatterns: ["dist", ".eslintrc.cjs"],
  parser: "@typescript-eslint/parser",
  plugins: ["@typescript-eslint", "react-refresh", "react-hooks"],
  rules: {
    // Validate that components can safely be updated with fast refresh.
    "react-refresh/only-export-components": [
      "warn",
      { allowConstantExport: true },
    ],
    // Warn on `any` type
    "@typescript-eslint/no-explicit-any": "warn",
    // Only warn on unused variables, and ignore variables starting with `_`
    "@typescript-eslint/no-unused-vars": [
      "warn",
      {
        argsIgnorePattern: "^_",
        varsIgnorePattern: "^_",
      },
    ],
    // removed in v8 : https://typescript-eslint.io/blog/announcing-typescript-eslint-v8#replacement-of-ban-types
    "@typescript-eslint/ban-types": "warn",
    // Disallow variable declarations from shadowing variables declared in the outer scope
    // https://typescript-eslint.io/rules/no-shadow/
    "no-shadow": "off",
    "@typescript-eslint/no-shadow": "warn",
    // Require const declarations for variables that are never reassigned after declared
    "prefer-const": "warn",
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
  },
};
