import { isMac } from "@/hooks/use-shortcut";
import { i18n } from "@/services/client/i18n";
import styles from "./shortcuts.module.scss";

export function Shortcuts() {
  let ctrlOrMeta;
  let shift;

  if (isMac) {
    ctrlOrMeta = "⌘";
    shift = "⇧";
  } else {
    ctrlOrMeta = i18n.get("Ctrl.Key");
    shift = i18n.get("Shift.Key");
  }

  return (
    <table className={styles.shortcuts}>
      <tbody>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>{i18n.get("Insert.Key")}</kbd>
          </td>
          <td>{i18n.get("create new record")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>E</kbd>
          </td>
          <td>{i18n.get("edit selected record")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>S</kbd>
          </td>
          <td>{i18n.get("save current record")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>Y</kbd>
          </td>
          <td>{i18n.get("duplicate current record")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>D</kbd>
          </td>
          <td>{i18n.get("delete current/selected record(s)")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>R</kbd>
          </td>
          <td>{i18n.get("refresh current view")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>Q</kbd>
          </td>
          <td>{i18n.get("close the current view tab")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>{shift}</kbd> + <kbd>F</kbd>
          </td>
          <td>{i18n.get("search for records")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>{shift}</kbd> + <kbd>G</kbd>
          </td>
          <td>{i18n.get("focus first or selected item in view")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>J</kbd>
          </td>
          <td>{i18n.get("navigate to previous page/record")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>K</kbd>
          </td>
          <td>{i18n.get("navigate to next page/record")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>{ctrlOrMeta}</kbd> + <kbd>M</kbd>
          </td>
          <td>{i18n.get("focus left menu search box")}</td>
        </tr>
        <tr>
          <td className={styles.keys}>
            <kbd>F9</kbd>
          </td>
          <td>{i18n.get("toggle left menu")}</td>
        </tr>
      </tbody>
    </table>
  );
}
