import { legacyClassNames } from "@/styles/legacy";
import get from "lodash/get";
import isEmpty from "lodash/isEmpty";
import { parseFragment } from "parse5";
import React from "react";
import {
  ATTRIBUTES,
  HTML_ATTRIBUTES,
  HTML_REACT_ATTRIBUTES,
} from "./constants";
import {
  getStyleObject,
  isArray,
  isObject,
  parseAngularExp,
  parseExpression,
  parseTemplate,
  stringToObject,
} from "./utils";

const REACT_COMPONENTS = [];
const CUSTOM_COMPONENTS = {};

function reactComponent(element, _props = {}, _component, context, ref) {
  const { tagName } = element;
  const props = Object.assign({}, _props);
  const compName = _component || tagName.toLowerCase();
  const CustomComponent = CUSTOM_COMPONENTS[compName];

  if (CustomComponent) {
    return (
      <CustomComponent
        ref={ref}
        {...props}
        context={context}
        eval={(arg) => parseExpression(arg)(context)}
      />
    );
  }
  return React.createElement(compName, props);
}

function evaluateObject(val) {
  let classes = stringToObject(val);
  const parser = parseTemplate(JSON.stringify(classes));
  return (context) => {
    const result = parser(context);
    classes = JSON.parse(result);

    // custom object implementation
    if (!val.endsWith("}")) {
      const varStr = val.slice(val.lastIndexOf("}") + 1);
      // isArray
      if (varStr.startsWith("[") && varStr.endsWith("]")) {
        const varName = varStr.slice(1, -1);
        const varValue = get(context, varName);
        return classes[varValue];
      }
      return "";
    }

    let temp = {};
    for (const item in classes) {
      const name = item.replace("'", "").replace("'", "");
      temp[name] = classes[item] === "true" ? true : false;
    }
    return legacyClassNames(temp);
  };
}

function defaultTemplateEval(val) {
  const parser = parseTemplate(val);
  return (context) => {
    return parser(context);
  };
}

function defaultExpressionEval(val) {
  const parser = parseExpression(val);
  return (context) => {
    return parser(context);
  };
}

export const ATTR_EVALUATOR = {
  [ATTRIBUTES.if]: (val) => {
    const parser = parseExpression(val);
    return (context) => {
      return parser(context);
    };
  },
  [ATTRIBUTES.show]: (val) => {
    const parser = parseExpression(val);
    return (context) => {
      const show = parser(context);
      if (!show) return "hide";
      else return "";
    };
  },
  [ATTRIBUTES.click]: (val) => {
    return () => val;
  },
  [ATTRIBUTES.actionClick]: (val) => {
    return (context) => context.$action(val);
  },
  [ATTRIBUTES.bind]: (val) => {
    return (context) => {
      const value = get(context, val);
      return value;
    };
  },
  [ATTRIBUTES.class]: (val) => {
    const value = val && val.trim();
    if (isObject(value)) {
      return evaluateObject(value);
    } else if (isArray(value)) {
      const classes = value.replace("[", "").replace("]", "").split(",");
      const parsers = classes.map((c) => {
        if (isObject(c.trim())) {
          return evaluateObject(c.trim());
        }
        return () => c;
      });
      return (context) => {
        return parsers.map((parser) => parser(context));
      };
    }
    const parser = parseTemplate(val);
    return (context) => {
      return parser(context);
    };
  },
  [ATTRIBUTES.href]: defaultTemplateEval,
  [ATTRIBUTES.src]: defaultTemplateEval,
  [ATTRIBUTES.readonly]: defaultExpressionEval,
  [ATTRIBUTES.bindHTML]: defaultExpressionEval,
  [HTML_ATTRIBUTES.href]: defaultTemplateEval,
  [HTML_ATTRIBUTES.src]: defaultTemplateEval,
  [HTML_ATTRIBUTES.style]: defaultTemplateEval,
  [HTML_ATTRIBUTES.data]: defaultTemplateEval,
  [HTML_ATTRIBUTES.title]: (val) => {
    const parser = parseAngularExp(val);
    return (context) => {
      return parser(context);
    };
  },
  [ATTRIBUTES.translate]: (val) => {
    const parser = parseTemplate(val);
    return (context) => {
      const value = parser(context);
      return context.__t(`${value}`);
    };
  },
};

function process(root) {
  /**
   * @param {any} element
   * @returns {(props: {context: DataContext}) => JSX.Element | null}
   */
  function processElement(element) {
    if (element === undefined) return;
    const { value, tagName } = element;
    // check empty line
    if (!tagName && typeof value === "string" && !value.trim())
      return () => value;

    let { attrs = [], childNodes } = element;
    let props = {};
    let classes = [];

    //for  x-translate
    attrs = attrs.map((a) => {
      if (a.name === ATTRIBUTES.translate) {
        let value = childNodes.find((c) => c.value);
        childNodes = childNodes
          .map((c) => (c.value ? "" : c))
          .filter((c) => c !== "");
        return { name: a.name, value: value.value };
      }
      return { ...a, name: HTML_REACT_ATTRIBUTES[a.name] || a.name };
    });

    const remainingAttr = attrs.filter(
      ({ name, value }) => !ATTR_EVALUATOR[name]
    );
    remainingAttr.forEach(({ name, value }) => {
      if (name === HTML_ATTRIBUTES.class) {
        classes.push(value);
      } else {
        props[name] = value;
      }
    });

    const attrEvals = attrs
      .filter(({ name, value }) => ATTR_EVALUATOR[name])
      .map(({ name, value }) => ({
        attr: name,
        eval: ATTR_EVALUATOR[name](value),
      }));

    let renderProps = () => ({});

    if (childNodes && childNodes.length) {
      const childs = [];
      for (let i = 0; i < childNodes.length; i++) {
        const child = processElement(childNodes[i]);
        child && childs.push(child);
      }

      renderProps = (context) => ({
        children: childs.map((ChildComponent, i) => (
          <ChildComponent key={i} context={context} />
        )),
      });
    } else {
      const content = value || null;
      const parser = content ? parseAngularExp(content) : null;
      renderProps = (context) => (parser ? { children: parser(context) } : {});
    }

    const ReactComponent = (() => {
      const HTMLComponent = React.forwardRef(function HTMLComponent(
        { context },
        ref
      ) {
        try {
          let ngClasses = [];
          let showIf = true;

          attrEvals.forEach((attrEval) => {
            const { attr, eval: evaluate } = attrEval;
            let result = "";
            if (!isEmpty(context)) {
              result = evaluate(context);
            }
            if (attr === ATTRIBUTES.if && (showIf = result) === false) {
              return;
            } else if (attr === ATTRIBUTES.show) {
              ngClasses.push(result);
            } else if (attr === ATTRIBUTES.click) {
              props.onClick = () => result;
            } else if (attr === ATTRIBUTES.actionClick) {
              props.onClick = (e) => {
                e.preventDefault();
                result(
                  e,
                  props[ATTRIBUTES.actionContext] &&
                    get(context, props[ATTRIBUTES.actionContext])
                );
              };
            } else if (attr === ATTRIBUTES.bind) {
              if (tagName === "input") {
                props.defaultValue = result;
              } else {
                props.children = result;
              }
            } else if (
              attr === ATTRIBUTES.href ||
              attr === HTML_ATTRIBUTES.href
            ) {
              props.href = result;
            } else if (
              attr === ATTRIBUTES.src ||
              attr === HTML_ATTRIBUTES.src
            ) {
              props.src = result;
            } else if (attr === HTML_ATTRIBUTES.title) {
              props.title = result;
            } else if (attr === HTML_ATTRIBUTES.style) {
              props.style = getStyleObject(result);
            } else if (attr === ATTRIBUTES.readonly) {
              props.readOnly = result;
            } else if (attr === ATTRIBUTES.class) {
              ngClasses.push(result);
            } else if (attr === ATTRIBUTES.bindHTML) {
              props.dangerouslySetInnerHTML = { __html: result };
            } else if (attr === HTML_ATTRIBUTES.data) {
              props.data = result;
            } else if (attr === ATTRIBUTES.translate) {
              props.children = result;
            }
          });
          let allClasses = classes.concat(ngClasses);
          if (allClasses.length > 0) {
            props.className = legacyClassNames(
              allClasses.filter((c) => c !== "")
            );
          }
          return showIf
            ? reactComponent(
                element,
                { ...renderProps(context), ...props },
                (REACT_COMPONENTS.includes(tagName) || !tagName) &&
                  React.Fragment,
                context,
                ref
              )
            : null;
        } catch (err) {
          if (err instanceof TypeError) {
            return <span style={{ color: "red" }}>{err.message}</span>;
          }
          return null;
        }
      });
      return HTMLComponent;
    })();

    // for ng-repeat
    const index = attrs && attrs.findIndex((a) => a.name === ATTRIBUTES.repeat);
    if (index >= 0) {
      const statement = attrs[index].value;

      // check key-value for-each
      // ng-repeat="(key, value) in list"
      const objectLoop = statement.match(
        /\(([^\s]+),\s([^\s]+)\)\s+in\s+([^\s]+)/
      );
      if (objectLoop) {
        const [, itemKey, itemValue, objKey] = objectLoop;
        function List({ context }) {
          const obj = get(context, objKey) || {};
          return (
            <React.Fragment>
              {Object.keys(obj).map((key, i) => (
                <ReactComponent
                  key={key}
                  context={{
                    ...context,
                    [itemKey]: key,
                    [itemValue]: obj[key],
                    $index: i,
                  }}
                />
              ))}
            </React.Fragment>
          );
        }
        return List;
      }

      // for-each loop
      // ng-repeat="item in list track by id"
      const [, itemKey, itemsKey, , key] = attrs[index].value.match(
        /([^\s]+)\s+in\s+([^\s]+)(\s+track\s+by\s+([^\s]+))?/
      );
      function List({ context }) {
        const data = get(context, itemsKey) || [];
        return (
          <React.Fragment>
            {data.map((item, i) => (
              <ReactComponent
                key={key ? item[key] : i}
                context={{
                  ...context,
                  [itemKey]: item,
                  $index: i,
                }}
              />
            ))}
          </React.Fragment>
        );
      }
      return List;
    }
    return ReactComponent;
  }
  return processElement(root);
}

function generateTree(root) {
  function processElement(
    { value, tagName, attrs, childNodes = [] },
    isTranslate
  ) {
    if (value === "\n") return;
    if (value) return { value };
    return {
      tagName,
      attrs,
      childNodes: childNodes.map((c) => processElement(c)).filter((c) => c),
    };
  }
  return processElement(root);
}

function replaceTag(str) {
  let closingTag = "",
    tag = "";
  const arr = str.split(" ")[0].match(/<([^/>]+)\/>/g);
  if (arr) {
    closingTag = arr[0];
  } else {
    tag = str.replace(/\/>/g, ">");
    closingTag = str.split(" ")[0].replace(/</g, "</") + ">";
  }
  return tag + closingTag;
}

export function processTemplate(template) {
  const newTemplate = template.replace(/<([^/>]+)\/>/g, replaceTag);
  const { childNodes = [] } = parseFragment(newTemplate);
  const hasSingleChild = childNodes.length === 1;
  const isCustomNode =
    hasSingleChild && CUSTOM_COMPONENTS[childNodes[0].tagName];
  const tree = generateTree(
    isCustomNode
      ? childNodes[0]
      : {
          tagName: hasSingleChild ? "" : "span",
          attrs: [],
          childNodes,
        }
  );
  return process(tree);
}
