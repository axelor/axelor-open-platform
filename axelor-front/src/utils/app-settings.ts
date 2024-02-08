import { session } from "@/services/client/session";

export function isProduction(): boolean {
  return session.info?.application?.mode === 'prod';
}

export function isDevelopment(): boolean {
  return !isProduction();
}