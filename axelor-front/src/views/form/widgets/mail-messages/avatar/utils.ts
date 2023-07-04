import { request } from "@/services/client/client";
import { l10n } from "@/services/client/l10n";
import { session } from "@/services/client/session";
import { MessageAuthor } from "../message/types";

export function getName(user: MessageAuthor) {
  const nameField = session?.info?.user?.nameField || "name";
  return user && user[nameField];
}

export function getAvatarText(user: MessageAuthor) {
  const name = (user && user.name) || "";
  return name
    .split(" ")
    .slice(0, 2)
    .map((str: string) => str[0])
    .join("")
    .toLocaleUpperCase(l10n.getLocale() || undefined);
}

const userColors: Record<string, string> = {};
const usedColors: string[] = [];
const colorNames = [
  "blue",
  "green",
  "red",
  "orange",
  "yellow",
  "lime",
  "teal",
  "purple",
  "pink",
  "brown",
  "deeppurple",
  "indigo",
  "lightblue",
  "cyan",
  "lightgreen",
  "amber",
  "deeporange",
  "grey",
  "bluegrey",
  "black",
  "white",
  "olive",
  "violet",
];

export function getColor(user: MessageAuthor) {
  if (!user) return null;
  if (userColors[user.code]) {
    return userColors[user.code];
  }
  if (usedColors.length === colorNames.length) {
    usedColors.length = 0;
  }
  const color = colorNames.find((n) => !usedColors.includes(n));
  usedColors.push(color!);
  const bgColor = `bg-${color}`;
  userColors[user.code] = bgColor;
  return bgColor;
}

const allowedUrls = new Map<string, boolean>();
const allowedUrlsMaxSize = 1000;
const fetchingUrls: Record<string, Promise<any>> = {};

function trimMap(map: Map<String, Boolean>, maxSize: number) {
  if (map.size <= maxSize) return;
  const it = map.keys();
  const half = maxSize / 2;
  while (map.size > half) {
    map.delete(it.next().value);
  }
}

export function checkUrl(
  url: string,
  onAllowed: (url: string) => void,
  onForbidden: (url: string) => void
) {
  trimMap(allowedUrls, allowedUrlsMaxSize);

  onAllowed = onAllowed || (() => {});
  onForbidden = onForbidden || (() => {});

  if (!url) {
    onForbidden(url);
    return;
  }

  let perm = allowedUrls.get(url);
  if (perm !== undefined) {
    if (perm) {
      onAllowed(url);
    } else {
      onForbidden(url);
    }
    return;
  }

  let fetchingUrl = fetchingUrls[url];
  if (fetchingUrl) {
    fetchingUrl.then((data) => {
      if (data.status < 400) {
        onAllowed(url);
      } else {
        onForbidden(url);
      }
    });
    return;
  }

  fetchingUrls[url] = request({
    url,
    method: "HEAD",
  })
    .then((data) => {
      if (data.status < 400) {
        allowedUrls.set(url, true);
        onAllowed(url);
      } else {
        allowedUrls.set(url, false);
        onForbidden(url);
      }
      return data;
    })
    .catch((error) => {
      console.error(error);
    })
    .finally(() => {
      delete fetchingUrls[url];
    });
}
