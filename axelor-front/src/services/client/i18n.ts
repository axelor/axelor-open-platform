import { request } from "./client";

// load the translation catalog
const bundle = await request({
  url: "js/messages.js",
}).then((res) => res.json());

export namespace i18n {
  export function get(text: string, ...args: any[]): string {
    let message = bundle[text] || bundle[(text || "").trim()] || text;
    if (message && args.length > 1) {
      for (let i = 0; i < args.length; i++) {
        let placeholder = new RegExp(`\\{${i}\\}`, "g");
        let value = args[i];
        message = message.replace(placeholder, value);
      }
    }
    return message;
  }
}
