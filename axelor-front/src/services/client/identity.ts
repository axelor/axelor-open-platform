import { request } from "./client";
import { MFAMethod } from "./mfa";

export interface IdentityCheckInfo {
  requiresPassword: boolean;
  requiresMfa: boolean;
  mfaMethods: MFAMethod[];
}

export interface IdentityVerifyPayload {
  password?: string;
  mfaCode?: string;
  mfaMethod?: string;
}

export async function getIdentityCheckInfo(): Promise<IdentityCheckInfo> {
  const resp = await request({
    url: "ws/auth/identity",
    method: "GET",
  });

  if (resp.ok) {
    return resp.json();
  }

  return Promise.reject(resp.status);
}

export async function verifyIdentity(
  payload: IdentityVerifyPayload,
): Promise<void> {
  const resp = await request({
    url: "ws/auth/identity",
    method: "POST",
    body: payload,
  });

  if (!resp.ok) {
    return Promise.reject(resp.status);
  }

  // RPC response: { status: 0 } on success, { status: -1, data: { message } } on failure
  const body = await resp.json();

  if (body.status === 0) {
    return;
  }

  const message = body.data?.message;
  return Promise.reject(message ?? body.status);
}
