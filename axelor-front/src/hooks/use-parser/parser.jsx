/* eslint-disable no-new-func */
import React from "react";
import sanitizeHTML from "sanitize-html";
import * as Babel from "@babel/standalone";

const blacklist = ["eval", "Function", "Reflect", "Proxy"];
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
        if (value === Function) throw new Error("Access to 'Function' is not allowed.");
        if (value === React.createElement) throw new Error("Access to 'React.createElement' is not allowed.");
        if (value === React.createFactory) throw new Error("Access to 'React.createFactory' is not allowed.");
        if (value === React.cloneElement) throw new Error("Access to 'React.cloneElement' is not allowed.");
        return typeof value === "function" ? value.bind(obj) : value;
      }`
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

  function elementProxy(file) {
    return addHelper(
      file,
      "trapElement",
      `function %%name%%(element) {
        const whitelist = [
          'dir', 'class', 'style', 'draggable', 'value', 'checked', 'selected', 'multiple', 'cols', 'placeholder',
          'clientTop', 'clientLeft', 'clientWidth', 'clientHeight',
          'offsetTop', 'offsetLeft', 'offsetWidth', 'offsetHeight',
          'scrollTop', 'scrollLeft', 'scrollWidth', 'scrollHeight',
          'focus', 'blur', 'click', 'select', 'setRangeText', 'setSelectionRange', 'scrollIntoView'
        ];
        return new Proxy(element || {}, {
          has(target, name) {
            return whitelist.includes(name);
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
        const blacklist = ['nativeEvent'];
        return function (event) {
          const e = new Proxy(event, {
            has(target, name) {
              return !blacklist.includes(name) && Reflect.has(target, name);
            },
            get(target, name) {
              if (blacklist.includes(name)) return undefined;
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

  let isVisited = (() => {
    let visited = [];
    return (v) => {
      if (visited.includes(v)) return true;
      visited.push(v);
    };
  })();

  const jsxMemberObject = Symbol();
  const isReact = (node) => t.isIdentifier(node) && node.name === "React";

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
          blacklist.includes(node.name) &&
          (parent.computed || parent.property !== node)
        ) {
          throw path.buildCodeFrameError(
            `Access to '${node.name}' is not allowed.`
          );
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
        if (scope.hasBinding(node.name)) return;
        path.replaceWith(t.memberExpression(ctx, node));
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
      MemberExpression(path, state) {
        const { node, scope } = path;
        const obj =
          node.object.name &&
          !isReact(node.object) &&
          !scope.hasBinding(node.object.name)
            ? t.memberExpression(ctx, node.object)
            : node.object;

        const name =
          t.isTemplateLiteral(node.property) &&
          node.property.quasis.length === 1
            ? node.property.quasis[0].value.raw
            : node.property.name || node.property.value;

        const isCreateElement = () =>
          node.loc &&
          ["createElement", "createFactory", "cloneElement"].includes(name);
        const isConstructor = () => name === "constructor";

        // don't allow access to 'constructor' and `React.createElement` methods
        if (
          !t.isAssignmentExpression(path.container) &&
          (isConstructor() ||
            isCreateElement() ||
            (t.isTemplateLiteral(node.property) &&
              node.property.expressions.length) ||
            (node.computed &&
              (t.isIdentifier(node.property) || !t.isLiteral(node.property))))
        ) {
          path.replaceWith(
            t.callExpression(validateFunction(state.file), [
              obj,
              t.isIdentifier(node.property) && !node.computed
                ? t.stringLiteral(node.property.name)
                : node.property,
            ])
          );
        } else if (
          (node.loc || node.object[jsxMemberObject]) &&
          obj !== node.object
        ) {
          path.replaceWith(
            t.memberExpression(obj, node.property, node.computed)
          );
        }
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
        modules: false,
        shippedProposals: true,
      },
    ],
    "react",
  ],
  plugins: ["ScopeTransformer"],
};

function sanitize(text) {
  return sanitizeHTML(text, {
    allowedTags: [...sanitizeHTML.defaults.allowedTags, "img"],
    allowedAttributes: {
      ...sanitizeHTML.defaults.allowedAttributes,
      img: ["alt", "src", "srcset", "sizes"],
    },
  });
}

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
