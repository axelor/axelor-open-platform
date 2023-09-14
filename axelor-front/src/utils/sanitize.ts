import DOMPurify from "dompurify";
import { createElement, memo, useMemo } from "react";

export function sanitize(text: string) {
  return DOMPurify.sanitize(text);
}

export function unaccent(value: string) {
  if (typeof value === "string") {
    return value
      .normalize("NFKD")
      .replace(/\p{Diacritic}/gu, "")
      .replace(/./g, function (c) {
        return (
          {
            "’": "'",
            æ: "ae",
            Æ: "AE",
            œ: "oe",
            Œ: "OE",
            ð: "d",
            Ð: "D",
            ł: "l",
            Ł: "L",
            ø: "o",
            Ø: "O",
          }[c] || c
        );
      });
  }
  return value;
}

export const SanitizedContent = memo(function SanitizedContent({
  content,
}: {
  content: string;
}) {
  const __html = useMemo(() => sanitize(content), [content]);
  return createElement("div", {
    dangerouslySetInnerHTML: { __html },
  });
});
