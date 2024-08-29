export function isValidUrl(url?: string | null) {
  // DOMPurify sanitizes HTML content, but not standalone URLs.
  if (!url || url.trim().startsWith("javascript:")) {
    return false;
  }

  try {
    new URL(url);
    return true;
  } catch (err) {
    return false;
  }
}
