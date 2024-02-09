import { session } from "@/services/client/session";

// Default number of records to display per page
export const DEFAULT_PAGE_SIZE = 40;
// Default max number of records per page
export const DEFAULT_MAX_PER_PAGE = 500;
// Default number of records to display per kanban column
export const DEFAULT_KANBAN_PAGE_SIZE = 20;
// Default number of records to fetch on search view
export const DEFAULT_SEARCH_PAGE_SIZE = 80;
// Default number of messages to display
export const DEFAULT_MESSAGE_PAGE_SIZE = 80;
// Default number of items to display on completion
export const DEFAULT_COMPLETION_PAGE_SIZE = 10;

/**
 * Checks if the application is running in a production mode
 */
export function isProduction(): boolean {
  return session.info?.application?.mode === "prod";
}

/**
 * Checks if the application is running in a development mode,
 * ie, not in production mode.
 */
export function isDevelopment(): boolean {
  return !isProduction();
}

/**
 * Get the default number of records to display per page
 */
export function getDefaultPageSize(): number {
  const defaultSize =
    session.info?.api?.pagination?.defaultPerPage ?? 0 > 0
      ? session.info?.api?.pagination?.defaultPerPage
      : DEFAULT_PAGE_SIZE;
  if (getDefaultMaxPerPage() > 0) {
    return Math.min(getDefaultMaxPerPage(), defaultSize!);
  }
  return defaultSize!;
}

/**
 * Get the max number of records to display per page.
 */
export function getDefaultMaxPerPage(): number {
  return session.info?.api?.pagination?.maxPerPage ?? DEFAULT_MAX_PER_PAGE;
}
