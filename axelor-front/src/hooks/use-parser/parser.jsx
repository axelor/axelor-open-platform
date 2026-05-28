/* eslint-disable no-new-func */
import * as Babel from "@babel/standalone";
import React from "react";

import { sanitize } from "@/utils/sanitize";

const blockedList = [
  "eval",
  "Function",
  "Reflect",
  "Proxy",
  "Object",
  "globalThis",
  "window",
  "self",
  "document",
  "__proto__",
  "prototype",
  "constructor",
];
const blockedProps = new Set(blockedList);

// Globals allowed as X.method() — only needed for names in blockedList
const allowedGlobals = new Map([
  ["Object", new Set(["entries", "keys", "values", "fromEntries"])],
]);

// Globals allowed as direct function calls (e.g. parseFloat("123"))
const allowedGlobalFunctions = new Set([
  "parseFloat",
  "parseInt",
  "isNaN",
  "isFinite",
  "Number",
  "String",
  "Boolean",
]);

const sanitizeURL = ["xlinkHref", "src", "href", "action", "formAction"];

function ScopeTransformer({ types: t, template }) {
  const helpers = [];

  function addHelper(file, name, tmpl, params) {
    if (file.declarations[name]) return file.declarations[name].id;
    const id = file.scope.generateUidIdentifier(name);
    const build = template(tmpl);
    const node = build({ ...params, name: id });
    file.declarations[name] = node;
    file.path.unshiftContainer("body", [node]);
    helpers.push(name);
    return id;
  }

  function validateFunction(file) {
    return addHelper(
      file,
      "checkFunction",
      `function %%name%%(obj, key) {
        const value = obj[key];
        if (value === Function
          || value === (async function(){}).constructor
          || value === (function*(){}).constructor
          || value === (async function*(){}).constructor
        ) throw new Error("Access to 'Function' is not allowed.");
        if (value === React.createElement) throw new Error("Access to 'React.createElement' is not allowed.");
        if (value === React.cloneElement) throw new Error("Access to 'React.cloneElement' is not allowed.");
        return typeof value === "function" ? value.bind(obj) : value;
      }`
    );
  }

  function validateOptionalFunction(file) {
    return addHelper(
      file,
      "checkOptionalFunction",
      `function %%name%%(obj, key) {
        if (obj == null) return undefined;
        return %%checkFunction%%(obj, key);
      }`,
      {
        checkFunction: validateFunction(file),
      }
    );
  }

  function validateURL(file) {
    return addHelper(
      file,
      "checkURL",
      `function %%name%%(value) {
        const isJavaScriptProtocol = /^[\\u0000-\\u001F ]*j[\\r\\n\\t]*a[\\r\\n\\t]*v[\\r\\n\\t]*a[\\r\\n\\t]*s[\\r\\n\\t]*c[\\r\\n\\t]*r[\\r\\n\\t]*i[\\r\\n\\t]*p[\\r\\n\\t]*t[\\r\\n\\t]*\\:/i;
        if (typeof value === 'string' && isJavaScriptProtocol.test(value)) throw new Error("javascript: URLs are not allowed.");
        return value;
      }`
    );
  }

  function validateProperty(file) {
    return addHelper(
      file,
      "checkProperty",
      `function %%name%%(key) {
        const blocked = ${JSON.stringify(Array.from(blockedProps))};
        if (typeof key === 'symbol') {
          throw new Error("Access to symbol properties is not allowed.");
        }
        const value = key == null ? key : String(key);
        if (typeof value === 'string' && blocked.includes(value)) {
          throw new Error("Access to '" + key + "' is not allowed.");
        }
        // Return the normalized key so coercion happens once and cannot mutate later.
        return value;
      }`
    );
  }

  function elementProxy(file) {
    return addHelper(
      file,
      "trapElement",
      `function %%name%%(element) {
        const allowed = [
          'dir', 'class', 'style', 'draggable', 'value', 'checked', 'selected', 'multiple', 'cols', 'placeholder',
          'clientTop', 'clientLeft', 'clientWidth', 'clientHeight',
          'offsetTop', 'offsetLeft', 'offsetWidth', 'offsetHeight',
          'scrollTop', 'scrollLeft', 'scrollWidth', 'scrollHeight',
          'focus', 'blur', 'click', 'select', 'setRangeText', 'setSelectionRange', 'scrollIntoView'
        ];
        return new Proxy(element || {}, {
          has(target, name) {
            return allowed.includes(name);
          },
          get(target, name) {
            const value = this.has(target, name) ? Reflect.get(target, name) : undefined;
            return typeof value === 'function' ? value.bind(target) : value;
          },
        });
      }`
    );
  }

  function trapEvent(file) {
    return addHelper(
      file,
      "trapEvent",
      `function %%name%%(handler) {
        const elements = ['target', 'currentTarget'];
        const blocked = ['nativeEvent'];
        return function (event) {
          const e = new Proxy(event, {
            has(target, name) {
              return !blocked.includes(name) && Reflect.has(target, name);
            },
            get(target, name) {
              if (blocked.includes(name)) return undefined;
              const value = Reflect.get(target, name);
              return elements.includes(name) ? %%proxy%%(value) : typeof value === 'function' ? value.bind(target) : value;
            },
          });
          return handler(e);
        }
      }`,
      {
        proxy: elementProxy(file),
      }
    );
  }

  function trapRef(file) {
    return addHelper(
      file,
      "trapRef",
      `function %%name%%(ref) {
        return typeof ref === 'function'
          ? element => ref(%%proxy%%(element))
          : element => {
              ref.current = element ? %%proxy%%(element) : null;
              return ref;
            };
      }`,
      {
        proxy: elementProxy(file),
      }
    );
  }

  function trapProps(file) {
    return addHelper(
      file,
      "trapProps",
      `function %%name%%(props) {
        if (props) {
          for (let name in props) {
            const value = props[name];
            if (name.startsWith('on')) props[name] = %%trapEvent%%(props[name]);
            if (name === 'ref') props[name] = %%trapRef%%(props[name]);
          }
        }
        return props;
      }`,
      {
        trapEvent: trapEvent(file),
        trapRef: trapRef(file),
      }
    );
  }

  function sanitize(file) {
    return addHelper(
      file,
      "sanitize",
      `function %%name%%(markup) {
        return markup && markup.__html
          ? { __html: %%sanitizeHTML%%(markup.__html) }
          : markup;
      }`,
      {
        sanitizeHTML: (usingSanitize =
          file.scope.generateUidIdentifier("sanitizeHTML")),
      }
    );
  }

  let ctx;
  let usingSanitize;

  const visited = new WeakSet();
  const isVisited = (v) => {
    if (visited.has(v)) return true;
    visited.add(v);
  };

  const jsxMemberObject = Symbol();
  const isReact = (node) => t.isIdentifier(node) && node.name === "React";

  function handleMemberExpression(path, state, isOptional) {
    const { node, scope } = path;
    if (isVisited(node)) return;
    const originalProperty = node.property;
    const makeMember = isOptional
      ? (obj, prop, computed) =>
          t.optionalMemberExpression(obj, prop, computed, true)
      : (obj, prop, computed) => t.memberExpression(obj, prop, computed);

    const isAllowedGlobalAccess = (() => {
      if (!t.isIdentifier(node.object) || node.computed) return false;
      if (!t.isIdentifier(node.property)) return false;
      const methods = allowedGlobals.get(node.object.name);
      if (methods === undefined) return false;
      return methods === null || methods.has(node.property.name);
    })();

    const obj =
      node.object.name &&
      !isReact(node.object) &&
      !isAllowedGlobalAccess &&
      !scope.hasBinding(node.object.name)
        ? makeMember(ctx, node.object, false)
        : node.object;

    const name =
      t.isTemplateLiteral(node.property) &&
      node.property.quasis.length === 1
        ? node.property.quasis[0].value.raw
        : node.property.name || node.property.value;

    if (blockedProps.has(name)) {
      throw path.buildCodeFrameError(
        `Access to '${name}' is not allowed.`
      );
    }

    if (node.computed) {
      node.property = t.callExpression(validateProperty(state.file), [
        originalProperty,
      ]);
    }

    const isCreateElement = () =>
      node.loc &&
      ["createElement", "cloneElement"].includes(name);
    const isConstructor = () => name === "constructor";
    const isStaticComputedProperty =
      t.isStringLiteral(originalProperty) ||
      t.isNumericLiteral(originalProperty) ||
      t.isBooleanLiteral(originalProperty) ||
      t.isNullLiteral(originalProperty) ||
      (t.isTemplateLiteral(originalProperty) &&
        originalProperty.expressions.length === 0);
    const isDynamicComputedProperty = node.computed && !isStaticComputedProperty;

    // don't allow access to 'constructor' and `React.createElement` methods
    if (
      !t.isAssignmentExpression(path.container) &&
      (isConstructor() ||
        isCreateElement() ||
        (t.isTemplateLiteral(originalProperty) &&
          originalProperty.expressions.length) ||
        isDynamicComputedProperty)
    ) {
      const validator = isOptional
        ? validateOptionalFunction(state.file)
        : validateFunction(state.file);
      const replacement = t.callExpression(validator, [
        obj,
        t.isIdentifier(node.property) && !node.computed
          ? t.stringLiteral(node.property.name)
          : node.property,
      ]);
      isVisited(replacement);
      path.replaceWith(replacement);
    } else if (
      (node.loc || node.object[jsxMemberObject]) &&
      obj !== node.object
    ) {
      const replacement = makeMember(obj, node.property, node.computed);
      isVisited(replacement);
      path.replaceWith(replacement);
    }
  }

  return {
    visitor: {
      Program: {
        enter(path, state) {
          ctx = state.file.scope.generateUidIdentifier("ctx");
          usingSanitize = false;
        },

        exit(path, state) {
          if (isVisited(this)) return;
          const { body } = path.node;
          const params = [ctx, t.identifier("React")];

          if (usingSanitize) {
            params.push(usingSanitize);
          }

          let lastIndex = -1;
          for (let i = body.length - 1; i >= 0; i--) {
            if (t.isEmptyStatement(body[i])) continue;
            if (t.isExpressionStatement(body[i])) {
              lastIndex = i;
              break;
            }
          }

          if (lastIndex > -1) {
            body[lastIndex] = t.returnStatement(body[lastIndex].expression);
          }

          path.stop();
          path.replaceWith(
            t.program([
              t.expressionStatement(
                t.arrowFunctionExpression(
                  params,
                  t.blockStatement(body, [
                    t.directive(t.directiveLiteral("use strict")),
                  ])
                )
              ),
            ])
          );
        },
      },
      Identifier(path) {
        const { node, parent, scope } = path;
        if (!node.loc) return;
        if (
          blockedList.includes(node.name) &&
          (parent.computed || parent.property !== node)
        ) {
          // Allow access to allowed global methods (e.g. Object.entries)
          const methods = allowedGlobals.get(node.name);
          const isAllowedAccess =
            methods !== undefined &&
            (t.isMemberExpression(parent) ||
              t.isOptionalMemberExpression(parent)) &&
            !parent.computed &&
            parent.object === node &&
            t.isIdentifier(parent.property) &&
            (methods === null || methods.has(parent.property.name));
          if (!isAllowedAccess) {
            throw path.buildCodeFrameError(
              `Access to '${node.name}' is not allowed.`
            );
          }
        }

        if (isReact(node)) {
          // do not allow re-declaring `React`
          if (
            t.isDeclaration(parent) ||
            (t.isVariableDeclarator(parent) && parent.init !== node)
          ) {
            throw path.buildCodeFrameError(`Declaring 'React' is not allowed.`);
          }
          return;
        }

        if (t.isObjectProperty(parent) && parent.key === node) return;
        if (t.isMemberExpression(parent) && !parent.computed) return;
        if (t.isOptionalMemberExpression(parent) && !parent.computed) return;
        if (scope.hasBinding(node.name)) return;

        if (
          t.isPrivateName(parent) ||
          t.isClassProperty(parent) ||
          t.isClassMethod(parent)
        ) {
          return;
        }

        // Don't rewrite allowed global functions (parseFloat, parseInt, etc.)
        if (allowedGlobalFunctions.has(node.name)) return;

        path.replaceWith(t.memberExpression(ctx, node));
      },
      ThisExpression(path) {
        throw path.buildCodeFrameError("Access to 'this' is not allowed.");
      },
      "ImportExpression|Import"(path) {
        throw path.buildCodeFrameError("Dynamic import() is not allowed.");
      },
      CallExpression(path) {
        if (path.node.callee.type === "Import") {
          throw path.buildCodeFrameError("Dynamic import() is not allowed.");
        }
      },
      ObjectProperty(path, state) {
        const { node } = path;
        if (!t.isObjectPattern(path.parent)) return;
        let key;
        if (t.isIdentifier(node.key) && !node.computed) {
          key = node.key.name;
        } else if (t.isStringLiteral(node.key)) {
          key = node.key.value;
        } else if (
          t.isTemplateLiteral(node.key) &&
          node.key.quasis.length === 1 &&
          node.key.expressions.length === 0
        ) {
          key = node.key.quasis[0].value.raw;
        }
        if (key && blockedProps.has(key)) {
          throw path.buildCodeFrameError(
            `Access to '${key}' is not allowed.`
          );
        }
        // Wrap dynamic computed keys with runtime validation
        if (node.computed && !key) {
          node.key = t.callExpression(validateProperty(state.file), [
            node.key,
          ]);
        }
      },
      JSXAttribute(path, state) {
        const { node } = path;
        const { name, value } = node;
        if (sanitizeURL.includes(name.name)) {
          node.value = t.callExpression(validateURL(state.file), [
            value.expression || value,
          ]);
        }
        if (name.name.startsWith("on")) {
          node.value = t.jsxExpressionContainer(
            t.callExpression(trapEvent(state.file), [value.expression])
          );
        }
        if (name.name === "ref") {
          node.value = t.jsxExpressionContainer(
            t.callExpression(trapRef(state.file), [value.expression])
          );
        }
        if (name.name === "dangerouslySetInnerHTML") {
          node.value = t.jsxExpressionContainer(
            t.callExpression(sanitize(state.file), [value.expression])
          );
        }
      },
      JSXSpreadAttribute(path, state) {
        const { node } = path;
        node.argument = t.callExpression(trapProps(state.file), [
          node.argument,
        ]);
      },
      JSXMemberExpression(path, state) {
        const { node } = path;
        node.object[jsxMemberObject] = true;
      },
      FunctionDeclaration(path, state) {
        if (
          helpers.some((name) => state.file.declarations[name] === path.node)
        ) {
          path.skip();
        }
      },
      OptionalMemberExpression(path, state) {
        handleMemberExpression(path, state, true);
      },
      MemberExpression(path, state) {
        handleMemberExpression(path, state, false);
      },
    },
  };
}

function SafeTransformer({ types: t }) {
  return {
    visitor: {
      "OptionalCallExpression|OptionalMemberExpression"(path) {
        // don't transform optional chaining
        path.skip();
      },
      MemberExpression(path) {
        const { node, parent, container } = path;

        // don't transform left-hand side
        if (t.isAssignmentExpression(parent) && parent.left === node) {
          path.skip();
          return;
        }

        if (t.isMemberExpression(container) || t.isCallExpression(container)) {
          container.type = `Optional${container.type}`;
        }

        node.optional = true;
        node.type = `Optional${node.type}`;
      },
    },
  };
}

Babel.registerPlugin("ScopeTransformer", ScopeTransformer);

const defaultOptions = {
  sourceType: "script",
  presets: [
    [
      "env",
      {
        targets: {
          chrome: "88",
          edge: "88",
          firefox: "91",
          safari: "16",
        },
        exclude: ["proposal-optional-chaining"],
      },
    ],
    "react",
  ],
  plugins: ["ScopeTransformer"],
};

export function parse(text, options = {}) {
  const opts = Object.assign({}, defaultOptions, options);
  const code = Babel.transform(text, opts).code;
  const func = new Function(`return ${code}`)();
  const res = (context) => func(context, React, sanitize);

  // store source and transform code for debug
  res.source = text;
  res.code = code;

  return res;
}

export function parseSafe(text, options = {}) {
  const safeOptions = {
    sourceType: "script",
    plugins: [SafeTransformer, "syntax-jsx"],
  };
  const code = Babel.transform(text, safeOptions).code;
  return parse(code, options);
}
