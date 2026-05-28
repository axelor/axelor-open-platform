const isJavaScriptProtocol =
  // eslint-disable-next-line no-control-regex, no-useless-escape
  /^[\u0000-\u001F ]*j[\r\n\t]*a[\r\n\t]*v[\r\n\t]*a[\r\n\t]*s[\r\n\t]*c[\r\n\t]*r[\r\n\t]*i[\r\n\t]*p[\r\n\t]*t[\r\n\t]*\:/i;

export function isValidUrl(url?: string | null) {
  // DOMPurify sanitizes HTML content, but not standalone URLs.
  if (!url || isJavaScriptProtocol.test(url)) {
    return false;
  }

  try {
    new URL(url);
    return true;
  } catch (err) {
    return false;
  }
}
