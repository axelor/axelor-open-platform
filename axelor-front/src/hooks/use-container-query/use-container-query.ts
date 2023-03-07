import { MutableRefObject, useEffect, useRef, useState } from "react";

type Name = "width" | "height";
type Operator = "=" | ">" | "<";
type Value = `${number}px`;

export type ContainerQuery = `${Name} ${Operator} ${Value}`;

const parse = (query: string) => {
  const [name, operator, value] = query.trim().split(/\s+/g);
  return [name, operator, parseInt(value)] as const;
};

function compare(op: string, left: number, right: number) {
  if (op === ">") return left > right;
  if (op === "<") return left < right;
  return left === right;
}

function matches(rect: DOMRectReadOnly, query: string) {
  const [name, operator, value] = parse(query);
  const { width, height } = rect;
  if (name === "width") return width > 0 && compare(operator, width, value);
  if (name === "height") return width > 0 && compare(operator, height, value);
  return false;
}

export function useContainerQuery(
  ref: MutableRefObject<HTMLElement | null>,
  query: ContainerQuery
) {
  const [state, setState] = useState<boolean>(false);
  const matchRef = useRef<boolean>(false);

  useEffect(() => {
    const { current } = ref;
    if (current == null) return;

    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        if (entry.target === current) {
          const matched = matches(entry.contentRect, query);
          if (matched === matchRef.current) return;
          matchRef.current = matched;
          setState(matched);
        }
      }
    });

    observer.observe(current);

    return () => {
      observer.unobserve(current);
      observer.disconnect();
    };
  }, [ref, query]);

  return state;
}
