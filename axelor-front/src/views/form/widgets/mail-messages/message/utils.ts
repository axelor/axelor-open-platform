import { session } from "@/services/client/session";
import { getName } from "../avatar/utils";
import { Message } from "./types";

export function getUser(message: Message) {
  const author = message.$author || message.$from;
  if (!author) return null;
  const key = session?.info?.user?.nameField || "name";
  const name =
    author.personal ||
    author[key] ||
    author.name ||
    author.fullName ||
    author.displayName;
  return { ...author, [key]: name };
}

export function getUserName(message: Message) {
  const user = getUser(message);
  return user && getName(user);
}
