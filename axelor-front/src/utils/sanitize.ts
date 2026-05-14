import DOMPurify from "dompurify";
import { createElement, memo, useMemo } from "react";

export function sanitize(text: string) {
  return DOMPurify.sanitize(text, {
    FORBID_TAGS: ['style', 'form'],
  });
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

/**
 * Sanitize an arbitrary string for safe use as a download filename.
 *
 * Strips path separators, Windows-reserved and control characters,
 * neutralizes `..` traversal sequences, trims leading/trailing dots and
 * spaces (which Windows handles inconsistently) and caps the length.
 */
export function sanitizeFilename(
  value?: string | null,
  { fallback = "file", maxLength = 100 }: { fallback?: string; maxLength?: number } = {},
) {
  if (!value) return fallback;
  const sanitized = value
    // eslint-disable-next-line no-control-regex
    .replace(/[\\/:*?"<>|\x00-\x1F]/g, "_")
    .replace(/\.{2,}/g, "_")
    .replace(/^[.\s]+|[.\s]+$/g, "")
    .slice(0, maxLength);
  return sanitized || fallback;
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
