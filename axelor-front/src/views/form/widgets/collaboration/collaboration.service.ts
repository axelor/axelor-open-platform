import dayjs, { Dayjs } from "dayjs";
import { Draft, produce } from "immer";
import isEmpty from "lodash/isEmpty";
import omit from "lodash/omit";
import uniqBy from "lodash/uniqBy";

import { SocketChannel } from "@/services/client/socket";
import { Listener, Store, createStore } from "@/store";

export type RecordState = {
  model: string;
  recordId: number;
  recordVersion: number;
  dirty: boolean;
};

export type User = {
  id: number;
  code: string;
  name: string;
  $avatar?: string;
};

export type State = {
  joinDate?: Dayjs;
  version?: number;
  versionDate?: Dayjs;
  dirty?: boolean;
  dirtyDate?: Dayjs;
  leftDate?: Dayjs;
};

export type Room = {
  model: string;
  recordId: number;
  recordVersion: number;
  dirty: boolean;
  users: User[];
  states: Record<string, State>;
  $join?: boolean;
};

const roomStores: Record<string, Store<Room> | undefined> = {};
const roomJoinCounts: Record<string, number> = {};

type Command = "JOIN" | "LEFT" | "STATE";

type Message = {
  dirty?: boolean;
  version?: number;
};

type MessageData = {
  command?: Command;
  model: string;
  recordId: number;
  message?: Message;
  user: User;
  users?: User[];
  states?: Record<string, State>;
};

const CollaborationService = () => {
  // Rejoin
  function onopen() {
    for (const store of Object.values(roomStores)) {
      const room = store?.get() ?? ({} as Room);
      const { model, recordId, dirty } = room;
      let version = 0;
      const currentVersion = room.recordVersion;
      if (currentVersion > version) {
        version = currentVersion;
      }
      channel.send({
        command: "JOIN",
        model,
        recordId,
        message: { dirty, version },
      });
    }
  }

  const channel = new SocketChannel("collaboration", { onopen });
  let unsubscribeCallback: (() => void) | null = null;

  const collaborationCallback = (data: any) => {
    const {
      command,
      model,
      recordId,
      message = {},
      user,
      users,
      states,
    } = data as MessageData;
    const key = getKey(model, recordId);
    const roomStore = roomStores[key];

    if (!roomStore) {
      console.error(`No room with key ${key}`);
      return;
    }

    if (!user) {
      console.error(`Command ${command} received with no user.`);
      return;
    }

    const room = roomStore.get();

    if (users || states) {
      updateStore(roomStore, (draft) => {
        states && (draft.states = states);
        users && updateUsers(draft, [user, ...users]);
      });
    }

    switch (command) {
      case "JOIN":
        updateStore(roomStore, (draft) => {
          draft.states = draft.states || {};
          const newUserState = {
            ...omit(draft.states[user.code], "leftDate"),
            ...message,
            joinDate: dayjs(),
          };
          if (newUserState.dirty) {
            newUserState.dirtyDate = dayjs();
          }
          draft.states[user.code] = newUserState;
          updateUsers(draft, [user, ...(users ?? [])]);
        });
        break;
      case "LEFT":
        const state = room.states?.[user.code] || {};
        // Keep user if they saved the record.
        if ((state.version ?? 0) > room.recordVersion) {
          updateStore(roomStore, (draft) => {
            draft.states = draft.states || {};
            draft.states[user.code].leftDate = dayjs();
          });
        } else {
          updateStore(roomStore, (draft) => {
            draft.states = draft.states || {};
            draft.users = (draft.users || []).filter((u) => u.id !== user.id);
            delete draft.states[user.code];
          });
        }
        break;
      case "STATE":
        const newState = { ...message } as State;
        if (newState.version != null && newState.dirty == null) {
          newState.dirty = false;
        }
        if (newState.dirty) {
          newState.dirtyDate = dayjs();
        }
        if ((newState.version ?? 0) <= room.recordVersion) {
          delete newState.version;
          delete newState.versionDate;
        } else if (newState.version != null) {
          newState.versionDate = dayjs();
        }
        updateStore(roomStore, (draft) => {
          draft.states = draft.states || {};
          const state = draft.states[user.code] || {};

          if (state.joinDate == null) {
            state.joinDate = dayjs();
          }

          if (newState.version && newState.version === state.version) {
            delete newState.versionDate;
          }
          if (newState.dirty && newState.dirty === state.dirty) {
            delete newState.dirtyDate;
          }

          draft.states[user.code] = { ...state, ...newState };
          updateUsers(draft, [user, ...(users || [])]);
        });
        break;
    }
  };

  function updateUsers(draft: Draft<Room>, users: User[]) {
    const states = draft.states || {};
    const recordVersion = draft.recordVersion;
    draft.users = uniqBy([...users, ...(draft.users || [])], "id").sort(
      (userA, userB) => {
        const stateA = states[userA?.code] || {};
        const stateB = states[userB?.code] || {};

        const versionDiff =
          (stateB.version || recordVersion) - (stateA.version || recordVersion);
        if (versionDiff) return versionDiff;

        const versionDateDiff =
          dayjs(stateB.versionDate ?? 0).valueOf() -
          dayjs(stateA.versionDate ?? 0).valueOf();
        if (versionDateDiff) return versionDateDiff;

        const dirtyDiff =
          Number(stateB.dirty ?? false) - Number(stateA.dirty ?? false);
        if (dirtyDiff) return dirtyDiff;

        const dirtyDateDiff =
          dayjs(stateB.dirtyDate ?? 0).valueOf() -
          dayjs(stateA.dirtyDate ?? 0).valueOf();
        if (dirtyDateDiff) return dirtyDateDiff;

        const joinDateDiff =
          dayjs(stateB.joinDate ?? 0).valueOf() -
          dayjs(stateA.joinDate ?? 0).valueOf();
        if (joinDateDiff) return joinDateDiff;

        return 0;
      }
    );
  }

  function updateRoom(recordState: RecordState) {
    const { model, recordId, recordVersion, dirty } = recordState;
    const key = getKey(model, recordId);
    const roomStore = roomStores[key];

    if (!roomStore) {
      console.error(`No room with key ${key}`);
      return;
    }

    const room = roomStore.get();
    let command = (room.$join ? "JOIN" : null) as Command | null;
    const oldProps = command === "JOIN" ? null : room;

    if (oldProps) {
      const { dirty: _dirty, recordVersion: _recordVersion } = oldProps;
      if (_dirty === dirty && _recordVersion === recordVersion) {
        return;
      }
    }

    updateStore(roomStore, (draft) => {
      draft.dirty = dirty;
      delete draft.$join;

      if (draft.recordVersion !== recordVersion) {
        draft.recordVersion = recordVersion;

        // Remove left users if version is up-to-date
        const states = draft.states || {};
        const users = (draft.users || []).filter((user) => {
          const state = states[user?.code] || {};
          return !state.leftDate || (state.version ?? 0) > recordVersion;
        });
        if (users.length !== (draft.users || []).length) {
          draft.users = users;
        }
      }
    });

    command = command ?? "STATE";

    if (command) {
      const message = {} as Message;
      if (oldProps && oldProps.dirty !== dirty) {
        message.dirty = dirty;
      }
      const version = recordVersion;
      if (oldProps && version !== oldProps.recordVersion) {
        message.version = version;
      }
      channel.send({
        command,
        model,
        recordId,
        message: isEmpty(message) ? undefined : message,
      });
    }
  }

  const channelSubscribe = () => {
    if (unsubscribeCallback) return false;
    unsubscribeCallback = channel.subscribe(collaborationCallback);
    return true;
  };

  const channelUnsubscribe = () => {
    if (!unsubscribeCallback || !isEmpty(roomStores)) return false;
    unsubscribeCallback();
    unsubscribeCallback = null;
    return true;
  };

  function joinRoom(
    { model, recordId, recordVersion, dirty }: RecordState,
    listener: Listener<Room>
  ) {
    const key = getKey(model, recordId);
    let roomStore = roomStores[key];
    if (roomStore == null) {
      roomStore = roomStores[key] = createStore<Room>({
        model,
        recordId,
        recordVersion,
        dirty,
        users: [],
        states: {},
        $join: true,
      });
      roomJoinCounts[key] = 1;
    } else {
      ++roomJoinCounts[key];
      const room = roomStore.get();
      listener(room, room);
    }

    roomStore.subscribe(listener);
    channelSubscribe();

    return () => {
      if (--roomJoinCounts[key] <= 0) {
        channel.send({ command: "LEFT", model, recordId });
        delete roomStores[key];
        delete roomJoinCounts[key];
      }
      channelUnsubscribe();
    };
  }

  function updateStore(
    store: Store<Room>,
    recipe: (state: Draft<Room>) => void
  ) {
    store.set((prev) => produce(prev, recipe));
  }

  return {
    joinRoom,
    updateRoom,
  };
};

type CollaborationServiceType = {
  joinRoom: (recordState: RecordState, listener: Listener<Room>) => () => void;
  updateRoom: (recordState: RecordState) => void;
};

let serviceInstance: CollaborationServiceType | null = null;

export const getCollaborationService = () => {
  if (!serviceInstance) {
    serviceInstance = CollaborationService();
  }
  return serviceInstance;
};

export function getKey(model: string, recordId: number) {
  return `${model}:${recordId}`;
}
