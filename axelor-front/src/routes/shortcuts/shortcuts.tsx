import { Fragment } from "react";

import { i18n } from "@/services/client/i18n";
import { device } from "@/utils/device";

import styles from "./shortcuts.module.scss";

const { isMac } = device;

const getKeys = isMac
  ? () => {
      return {
        ctrlKey: "⌘",
        altKey: "⌥",
        commandOptionKey: ["⌘", "⌥"],
      };
    }
  : () => {
      const ctrlKey = i18n.get("Ctrl.Key");
      const altKey = i18n.get("Alt.Key");
      const commandOptionKey = [altKey];

      return { ctrlKey, altKey, commandOptionKey };
    };

export function Shortcuts() {
  const { ctrlKey, altKey, commandOptionKey } = getKeys();

  // Conflicts are noted below for information:
  const shortcuts = [
    {
      // No known conflicts
      keys: [ctrlKey, i18n.get("Insert.Key")],
      description: i18n.get("create new record"),
    },
    {
      // Chrome: "Search from anywhere on the page"
      keys: [ctrlKey, "E"],
      description: i18n.get("edit selected record"),
    },
    {
      // Chrome:  "Open options to save the current page"
      keys: [ctrlKey, "S"],
      description: i18n.get("save current record"),
    },
    {
      // Chrome: "Save your current webpage as a bookmark"
      keys: [ctrlKey, "D"],
      description: i18n.get("duplicate current record"),
    },
    {
      // No known conflicts
      keys: [ctrlKey, i18n.get("Delete.Key")],
      description: i18n.get("delete current/selected record(s)"),
    },
    {
      // Chrome: "Reload the current page"
      keys: [ctrlKey, "R"],
      description: i18n.get("refresh current view"),
    },
    {
      // Mac: quit application
      keys: [ctrlKey, "Q"],
      description: i18n.get("close the current view tab"),
    },
    {
      // Chrome: "Open the Chrome menu"
      keys: [...commandOptionKey, "F"],
      description: i18n.get("search for records"),
    },
    {
      // No known conflicts
      keys: [...commandOptionKey, "G"],
      description: i18n.get("focus first or selected item in view"),
    },
    {
      // No known conflicts
      keys: [altKey, i18n.get("PageUp.Key")],
      description: i18n.get("navigate to previous page/record"),
    },
    {
      // No known conflicts
      keys: [altKey, i18n.get("PageDown.Key")],
      description: i18n.get("navigate to next page/record"),
    },
    {
      // Mac: minimize application
      keys: [ctrlKey, "M"],
      description: i18n.get("focus left menu search box"),
    },
    {
      // No known conflicts
      keys: ["F9"],
      description: i18n.get("toggle left menu"),
    },
  ];

  return (
    <table className={styles.shortcuts}>
      <tbody>
        {shortcuts.map((shortcut) => (
          <ShortcutRow key={`shortcut-row-${shortcut.keys}`} {...shortcut} />
        ))}
      </tbody>
    </table>
  );
}

function ShortcutRow({
  keys,
  description,
}: {
  keys: string[];
  description: string;
}) {
  return (
    <tr>
      <td className={styles.keys}>
        {keys.map((key, index) => (
          <Fragment key={`shortcut-key-${key}`}>
            <kbd>{key}</kbd>
            {index < keys.length - 1 && <> + </>}
          </Fragment>
        ))}
      </td>
      <td>{description}</td>
    </tr>
  );
}
