import { request } from "./client";

let bundle: Record<string, string> = {};

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace i18n {
  export async function load() {
    // load the translation catalog
    bundle = await request({
      url: "js/messages.js",
    }).then((res) => res.json());
  }
  export function get(text: string, ...args: any[]): string {
    let message = bundle[text] || bundle[(text || "").trim()] || text;
    if (message && args.length) {
      for (let i = 0; i < args.length; i++) {
        let placeholder = new RegExp(`\\{${i}\\}`, "g");
        let value = args[i];
        message = message.replace(placeholder, value);
      }
    }
    return message;
  }
}
