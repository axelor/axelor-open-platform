import { createElement, memo, useMemo } from "react";
import sanitizeHTML from "sanitize-html";

const allowedTags = [...sanitizeHTML.defaults.allowedTags, "img"];
const allowedAttributes = {
  ...sanitizeHTML.defaults.allowedAttributes,
  img: ["alt", "src", "srcset", "sizes"],
};

export function sanitize(text: string) {
  return sanitizeHTML(text, {
    allowedTags,
    allowedAttributes,
  });
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
