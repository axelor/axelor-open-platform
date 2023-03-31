import DOMPurify from "dompurify";
import { createElement, memo, useMemo } from "react";

export function sanitize(text: string) {
  return DOMPurify.sanitize(text);
}

export const SenitizedContent = memo(function SenitizedContent({
  content,
}: {
  content: string;
}) {
  const __html = useMemo(() => sanitize(content), [content]);
  return createElement("div", {
    dangerouslySetInnerHTML: { __html },
  });
});
