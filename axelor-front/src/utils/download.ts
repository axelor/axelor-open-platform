import { alerts } from "@/components/alerts";
import { i18n } from "@/services/client/i18n";
import { sanitizeFilename } from "@/utils/sanitize";

// Gets filename from Content-Disposition
function getFilename(disposition: string | null) {
  if (disposition === null) return null;
  let results = /filename\*=UTF-8''(.*)/i.exec(disposition);
  if (results) {
    return decodeURIComponent(results[1]);
  }
  results = /filename=(.*)/i.exec(disposition);
  if (results) {
    // remove trailing double quote
    return results[1].replace(/^"(.+(?="$))"$/, "$1");
  }
  return null;
}

/**
 * Triggers a browser file download for the given URL.
 *
 * The filename is sanitized before use to strip characters that are illegal
 * in filenames across platforms (e.g. `\ / : * ? " < > |`).
 */
export function saveAs(url: string, fileName: string) {
  const dotIndex = fileName.lastIndexOf(".");
  const ext = dotIndex > 0 ? fileName.slice(dotIndex) : "";
  const base = dotIndex > 0 ? fileName.slice(0, dotIndex) : fileName;
  const name = sanitizeFilename(base) + ext;
  const link = document.createElement("a");
  
  link.textContent = name || "File";
  link.download = name || "download";
  link.href = url;

  Object.assign(link.style, {
    position: "fixed",
    top: "-9999px",
    left: "-9999px",
  });

  document.body.appendChild(link);

  link.onclick = (e) => {
    setTimeout(() => {
      if (e.target) {
        document.body.removeChild(e.target as any);
      }
    }, 300);
  };

  setTimeout(() => link.click(), 100);

  const message = name
    ? i18n.get("Downloading {0}…", name)
    : i18n.get("Downloading file…");

  alerts.info({ message });
}

export async function download(url: string, fileName?: string) {
  if (fileName && !url.startsWith("data:")) {
    const qs = new URLSearchParams({ fileName }).toString();
    const sp = url.includes("?") ? "&" : "?";
    url = url + sp + qs;
  }

  const res = await fetch(url, {
    method: "HEAD",
  });

  if (res.status !== 200) {
    const message = fileName
      ? i18n.get("File {0} does not exist.", fileName)
      : i18n.get("File does not exist.");
    return alerts.error({ message });
  }

  if (res.ok) {
    const disposition = res.headers.get("Content-Disposition");
    const name = getFilename(disposition) || fileName || "";
    saveAs(url, name);
  }
}
