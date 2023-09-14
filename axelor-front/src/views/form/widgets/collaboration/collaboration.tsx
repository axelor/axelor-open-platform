import dayjs, { Dayjs } from "dayjs";
import { useAtomValue } from "jotai";
import {
  SyntheticEvent,
  memo,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import { Block, Box, Menu, MenuItem, useClassNames } from "@axelor/ui";

import { useSession } from "@/hooks/use-session";
import { useNavTabsSize } from "@/layout/nav-tabs";
import { i18n } from "@/services/client/i18n";
import { useViewDirtyAtom } from "@/view-containers/views/scope";
import { FormAtom } from "../../builder";
import Avatar from "../mail-messages/avatar/avatar";

import { State, User, getCollaborationService } from "./collaboration.service";

import styles from "./collaboration.module.scss";

const _t = i18n.get;

export const Collaboration = memo(({ formAtom }: { formAtom: FormAtom }) => {
  const { data: sessionData } = useSession();
  const { enabled = true } = sessionData?.view?.collaboration ?? {};
  const { canViewCollaboration: canView = true } = sessionData?.user ?? {};

  const { model, record } = useAtomValue(formAtom);
  const { id: recordId, version: recordVersion } = record;

  return (
    enabled &&
    recordId != null &&
    recordVersion != null && (
      <CollaborationContainer
        model={model}
        recordId={recordId}
        recordVersion={recordVersion}
        canView={canView}
      />
    )
  );
});

const CollaborationContainer = memo(
  ({
    model,
    recordId,
    recordVersion,
    canView,
  }: {
    model: string;
    recordId: number;
    recordVersion: number;
    canView?: boolean;
  }) => {
    const classNames = useClassNames();

    const dirtyAtom = useViewDirtyAtom();
    const dirty = useAtomValue(dirtyAtom) ?? false;

    const collaborationService = useMemo(() => getCollaborationService(), []);

    const [users, setUsers] = useState<User[]>([]);
    const [states, setStates] = useState<Record<string, State>>({});

    // On join
    useEffect(() => {
      return collaborationService.joinRoom(
        { model, recordId, recordVersion: 0, dirty: false },
        (room) => {
          setUsers(room.users);
          setStates(room.states);
        }
      );
    }, [collaborationService, model, recordId]);

    // On update
    useEffect(() => {
      collaborationService.updateRoom({
        model,
        recordId,
        recordVersion,
        dirty,
      });
    }, [collaborationService, model, recordId, recordVersion, dirty]);

    if (!canView || users?.length <= 1) {
      return null;
    }

    return (
      <Box className={classNames(styles.viewCollaboration)}>
        <Users recordVersion={recordVersion} users={users} states={states} />
      </Box>
    );
  }
);

function Users({
  recordVersion,
  users,
  states,
}: {
  recordVersion: number;
  users: User[];
  states: Record<string, State>;
}) {
  const sessionUser = useSessionUser();
  const otherUsers = useMemo(
    () => users.filter((user) => user.id !== sessionUser?.id),
    [users, sessionUser]
  );

  const containerSize = useNavTabsSize();

  const avatarLimit = useMemo(() => {
    switch (containerSize) {
      case "xs":
        return 0;
      case "sm":
        return 1;
      case "md":
        return 2;
      case "lg":
        return 3;
      case "xl":
        return 4;
      case "xxl":
        return 5;
      default:
        return 0;
    }
  }, [containerSize]);

  const [inlineUsers, groupedUsers] = useMemo(() => {
    if (avatarLimit <= 0) {
      return [[], []];
    }
    if (otherUsers.length <= avatarLimit) {
      return [otherUsers, []];
    }
    const limit = avatarLimit - 1;
    return [otherUsers.slice(0, limit), otherUsers.slice(limit)];
  }, [otherUsers, avatarLimit]);

  const getClassName = useCallback(
    (state: State = {}) => {
      if (state.leftDate) {
        return "left";
      }
      if ((state.version ?? 0) > recordVersion && state.versionDate) {
        return "saved";
      }
      if (state.dirty) {
        return "dirty";
      }
      return "joined";
    },
    [recordVersion]
  );

  const getTitle = useCallback(
    (
      className: string,
      userName: string,
      state: State = {},
      _currentDate: string = ""
    ) => {
      if (className === "left") {
        return _t("{0} left {1}", userName, formatDate(state.versionDate));
      }
      if (className === "saved") {
        return _t("{0} saved {1}", userName, formatDate(state.versionDate));
      }
      if (className === "dirty") {
        return _t(
          "{0} is editing since {1}",
          userName,
          formatDate(state.dirtyDate)
        );
      }
      return _t("{0} joined {1}", userName, formatDate(state.joinDate));
    },
    []
  );

  return (
    <Box d="flex" flexDirection="row" gap="2">
      {inlineUsers.map((user) => (
        <SingleUser
          key={user.id}
          user={user}
          state={states[user.code]}
          getClassName={getClassName}
          getTitle={getTitle}
        />
      ))}
      {groupedUsers.length ? (
        <GroupedUsers
          users={groupedUsers}
          states={states}
          isAll={!inlineUsers.length}
          getClassName={getClassName}
          getTitle={getTitle}
        />
      ) : null}
    </Box>
  );
}

function SingleUser({
  user,
  state = {},
  getClassName,
  getTitle,
  withUserName = false,
}: {
  user: User;
  state: State;
  getClassName: (state: State) => string;
  getTitle: (
    className: string,
    userName: string,
    state: State,
    date: string
  ) => string;
  withUserName?: boolean;
}) {
  const classNames = useClassNames();
  const userName = useUserName(user);
  const className = useMemo(() => getClassName(state), [getClassName, state]);
  const [currentDate, setCurrentDate] = useState(getCurrentDate);
  const title = useMemo(
    () => getTitle(className, userName, state, currentDate),
    [getTitle, className, userName, state, currentDate]
  );

  const handleMouseEnter = useCallback(
    () => setCurrentDate(getCurrentDate()),
    [setCurrentDate]
  );

  if (withUserName) {
    return (
      <Box
        d="flex"
        alignItems={"center"}
        title={title}
        onMouseEnter={handleMouseEnter}
      >
        <Box d="flex" alignItems={"center"} pe={2}>
          <Box
            className={classNames(styles.avatarContainer, styles[className])}
          >
            <Avatar
              user={user}
              title={title}
              className={classNames(styles.avatar)}
            />
          </Box>
        </Box>
        <span>{userName}</span>
      </Box>
    );
  }

  return (
    <Box
      title={title}
      onMouseEnter={handleMouseEnter}
      className={classNames(styles.avatarContainer, styles[className])}
    >
      <Avatar user={user} title={title} className={classNames(styles.avatar)} />
    </Box>
  );
}

function GroupedUsers({
  users = [],
  states = {},
  getClassName,
  getTitle,
  isAll = false,
}: {
  users: User[];
  states: Record<string, State>;
  getClassName: (state: State) => string;
  getTitle: (
    className: string,
    userName: string,
    state: State,
    date: string
  ) => string;
  isAll?: boolean;
}) {
  const classNames = useClassNames();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const openMenu = useCallback(
    (event: SyntheticEvent) => setAnchorEl(event.currentTarget as HTMLElement),
    [setAnchorEl]
  );
  const closeMenu = useCallback(() => setAnchorEl(null), [setAnchorEl]);

  const firstUser = useMemo(() => users[0] || {}, [users]);
  const firstState = states[firstUser.code];
  const firstUserName = useUserName(firstUser);

  const className = useMemo(
    () => getClassName(firstState),
    [firstState, getClassName]
  );
  const [currentDate, setCurrentDate] = useState(getCurrentDate);
  const title = useMemo(
    () => getTitle(className, firstUserName, firstState, currentDate),
    [getTitle, className, firstUserName, firstState, currentDate]
  );
  const handleMouseEnter = useCallback(
    () => setCurrentDate(getCurrentDate()),
    [setCurrentDate]
  );
  const text = useMemo(
    () => `${isAll ? "" : "+"}${users.length}`,
    [isAll, users]
  );

  return (
    <Box>
      <Box
        title={title}
        onMouseEnter={handleMouseEnter}
        className={classNames(styles.avatarContainer, styles[className])}
      >
        <Block onClick={openMenu} className={classNames(styles.menu)}>
          <Avatar
            text={text}
            title={title}
            className={classNames(styles.avatar, styles.grouped)}
          />
        </Block>
      </Box>
      <Menu
        className={styles.viewCollaborationMenu}
        target={anchorEl}
        show={Boolean(anchorEl)}
        onHide={closeMenu}
      >
        {users.map((user) => {
          return (
            <MenuItem key={user.id} onClick={closeMenu}>
              <SingleUser
                key={user.id}
                user={user}
                state={states[user.code]}
                getClassName={getClassName}
                getTitle={getTitle}
                withUserName
              />
            </MenuItem>
          );
        })}
      </Menu>
    </Box>
  );
}

function useSessionUser() {
  const session = useSession();
  return useMemo(() => {
    const user = session.data?.user;
    if (!user) {
      return user;
    }
    const { id, login, name, nameField, image } = user;
    return {
      id,
      code: login,
      [nameField ?? "name"]: name,
      $avatar: image,
    } as User;
  }, [session]);
}

function useUserName(user: User) {
  const session = useSession();
  const nameField = useMemo(
    () => session.data?.user?.nameField ?? "name",
    [session]
  );

  return useMemo(
    () =>
      (
        user as User & {
          [nameField: string]: string;
        }
      )[nameField],
    [user, nameField]
  );
}

function formatDate(date: string | number | Dayjs | undefined) {
  return dayjs(date).fromNow();
}

function getCurrentDate() {
  return dayjs().format("YYYY-MM-DD HH:mm");
}
