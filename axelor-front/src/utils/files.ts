import { alerts } from "@/components/alerts";
import { i18n } from "@/services/client/i18n";
import { session } from "@/services/client/session";

/**
 * Validates if the size of the given file is within the allowed maximum size.
 *
 * @param {File} file - The file to be validated.
 * @return {boolean} Returns true if the file size is within the maximum limit, otherwise false.
 */
export function validateFileSize(file: File): boolean {
  if (!file) return false;
  const maxSize = session.info?.data?.upload?.maxSize ?? 0;
  const uploadMaxSize = 1048576 * maxSize;

  if (maxSize > 0 && file.size > uploadMaxSize) {
    alerts.info({
      message: i18n.get(
        "You are not allowed to upload a file bigger than {0} MB.",
        maxSize,
      ),
    });
    return false;
  }
  return true;
}