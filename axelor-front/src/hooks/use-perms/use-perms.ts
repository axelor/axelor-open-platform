import { Perms, Schema } from "@/services/client/meta.types";
import { useCallback } from "react";

const defaultPerms: Perms = {};

const PERMS = {
  new: "create",
  edit: "write",
  save: "write",
  select: "read",
  view: "read",
  copy: "create",
  delete: "remove",
  remove: "remove",
  archive: "remove",
  export: "export",
  attach: "__",
} as const;

const ACTIONS = {
  new: "canNew",
  edit: "canEdit",
  save: "canSave",
  select: "canSelect",
  copy: "canCopy",
  delete: "canDelete",
  remove: "canRemove",
  archive: "canArchive",
  view: "canView",
  export: "__",
  attach: "canAttach",
} as const;

type PermissionName = keyof typeof PERMS | keyof typeof ACTIONS;

function attr(props: Schema, which: string) {
  return props[which] !== false;
}

function perm(perms: Perms, which: keyof Perms) {
  return perms?.[which] !== false;
}

function can(props: Schema, perms: Perms, what: PermissionName) {
  return attr(props, ACTIONS[what]) && perm(perms, PERMS[what] as keyof Perms);
}

/**
 * This hook can be used to check permissions
 * and use of a specific action button.
 *
 * @param state the current state of the view/field
 * @param perms the permissions state
 * @returns `{ hasPermission, hasButton }` to check permission and button
 *
 */
export function usePerms(state: Schema, perms = defaultPerms) {
  const hasPermission = useCallback(
    /**
     * Check whether the given operation is permitted.
     *
     * @param name operation name
     * @returns true if permitted false otherwise
     */
    (name: PermissionName) => {
      return can(state, perms, name);
    },
    [perms, state]
  );

  const hasButton = useCallback(
    /**
     * Check whether the given operation button is available.
     *
     * The button is not available is specified by the meta attrs
     * or not permitted for that operation.
     *
     * @param name operation name
     * @returns true if permitted false otherwise
     */
    (name: string) => {
      return can(state, perms, name as PermissionName) && state[name] !== false;
    },
    [perms, state]
  );

  return { hasPermission, hasButton } as const;
}
