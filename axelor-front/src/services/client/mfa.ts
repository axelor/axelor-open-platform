import { request } from "./client";

export type MFAMethod = "EMAIL" | "TOTP" | "RECOVERY";

const storageKey = {
  mfaMethod: (username: string) => `${username}:mfa:method`,
  emailRetryAfter: (username: string) => `${username}:email:retryAfter`,
};

export async function sendEmailVerificationCode(username: string) {
  const resp = await request({
    url: `ws/public/mfa/email-code/send`,
    method: "POST",
    body: { username },
  });

  try {
    const data = await resp.json();
    return resp.ok ? Promise.resolve(data) : Promise.reject(data);
  } catch {
    // handle error
  }

  return Promise.reject(resp.status);
}

export const mfaSession = {
  MFAMethod: {
    get: (username: string): MFAMethod | null =>
      sessionStorage.getItem(
        storageKey.mfaMethod(username),
      ) as MFAMethod | null,
    set: (username: string, method: MFAMethod): void =>
      sessionStorage.setItem(storageKey.mfaMethod(username), method),
  },

  EmailRetryAfter: {
    get: (username: string): string | null =>
      sessionStorage.getItem(storageKey.emailRetryAfter(username)),
    set: (username: string, retryAfter: string): void =>
      sessionStorage.setItem(
        storageKey.emailRetryAfter(username),
        String(retryAfter),
      ),
  },

  reset: (username: string): void => {
    sessionStorage.removeItem(storageKey.mfaMethod(username));
    sessionStorage.removeItem(storageKey.emailRetryAfter(username));
  },
};
