import { createElement, useCallback, useMemo } from "react";

import { DataContext } from "@/services/client/data.types";
import { EvalContextOptions, createEvalContext } from "./eval-context";
import { processTemplate } from "./template-utils";
import { parseAngularExp, parseExpression } from "./utils";

const isSimple = (expression: string) => {
  return !expression.includes("{{") && !expression.includes("}}");
};

export function useExpression(expression: string) {
  return useCallback(
    (context: DataContext, options?: EvalContextOptions) => {
      const func = isSimple(expression)
        ? parseExpression(expression)
        : parseAngularExp(expression);
      const evalContext = createEvalContext(context, options);
      return func(evalContext);
    },
    [expression]
  );
}

export function useTemplate(template: string) {
  return useMemo(() => {
    const Comp = processTemplate(template);
    return (props: { context: DataContext; options?: EvalContextOptions }) => {
      const context = createEvalContext(props.context, props.options);
      return createElement(Comp, { context });
    };
  }, [template]);
}
