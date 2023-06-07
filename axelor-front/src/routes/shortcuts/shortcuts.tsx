import { Fragment } from "react";

import { isMac } from "@/hooks/use-shortcut";
import { i18n } from "@/services/client/i18n";

import styles from "./shortcuts.module.scss";

export function Shortcuts() {
  const ctrlKey = isMac ? "⌘" : i18n.get("Ctrl.Key");

  const shortcuts = [
    {
      keys: [ctrlKey, i18n.get("Insert.Key")],
      description: i18n.get("create new record"),
    },
    {
      keys: [ctrlKey, "E"],
      description: i18n.get("edit selected record"),
    },
    {
      keys: [ctrlKey, "S"],
      description: i18n.get("save current record"),
    },
    {
      keys: [ctrlKey, "D"],
      description: i18n.get("duplicate current record"),
    },
    {
      keys: [ctrlKey, i18n.get("Delete.Key")],
      description: i18n.get("delete current/selected record(s)"),
    },
    {
      keys: [ctrlKey, "R"],
      description: i18n.get("refresh current view"),
    },
    {
      keys: [ctrlKey, "Q"],
      description: i18n.get("close the current view tab"),
    },
    {
      keys: [ctrlKey, "F"],
      description: i18n.get("search for records"),
      activeWhenNotInput: true,
    },
    {
      keys: [ctrlKey, "G"],
      description: i18n.get("focus first or selected item in view"),
    },
    {
      keys: [ctrlKey, "←"],
      description: i18n.get("navigate to previous page/record"),
      activeWhenNotInput: true,
    },
    {
      keys: [ctrlKey, "→"],
      description: i18n.get("navigate to next page/record"),
      activeWhenNotInput: true,
    },
    {
      keys: [ctrlKey, "M"],
      description: i18n.get("focus left menu search box"),
    },
    {
      keys: ["F9"],
      description: i18n.get("toggle left menu"),
    },
  ];

  const activeWhenNotInputText = i18n.get(
    "active when a text input is not focused"
  );

  return (
    <table className={styles.shortcuts}>
      <tbody>
        {shortcuts.map((shortcut) => (
          <ShortcutRow key={`shortcut-row-${shortcut.keys}`} {...shortcut} />
        ))}
        <tr className={styles.notes}>
          <td colSpan={2}>1. {activeWhenNotInputText}</td>
        </tr>
      </tbody>
    </table>
  );
}

function ShortcutRow({
  keys,
  description,
  activeWhenNotInput,
}: {
  keys: string[];
  description: string;
  activeWhenNotInput?: boolean;
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
        {activeWhenNotInput && <sup>&nbsp;1</sup>}
      </td>
      <td>{description}</td>
    </tr>
  );
}
