import { useAtom } from "jotai";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { useAsync } from "@/hooks/use-async";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useTabs } from "@/hooks/use-tabs";
import { useTags, useTagsMail } from "@/hooks/use-tags";
import { findFields } from "@/services/client/meta-cache.ts";
import { DEFAULT_MESSAGE_PAGE_SIZE } from "@/utils/app-settings.ts";
import {
  useViewAction,
  useViewTab,
  useViewTabRefresh,
} from "@/view-containers/views/scope";
import { focusAtom } from "@/utils/atoms";
import { FormAtom, WidgetProps } from "../../builder";
import { useAfterActions } from "../../builder/scope";
import { getMailChannelService } from "./mail-channel.service";
import { MessageBox } from "./message";
import { Message, MessageFetchOptions, MessageFlag } from "./message/types";
import { DataSource } from "./utils";

async function findMessages(
  id: number,
  model: string,
  {
    parent,
    folder,
    type,
    offset = 0,
    limit = DEFAULT_MESSAGE_PAGE_SIZE,
  }: MessageFetchOptions,
) {
  const { total = 0, data = [] } = await (parent
    ? DataSource.replies(parent)
    : folder
      ? DataSource.folder(folder!, limit, offset)
      : DataSource.messages(id, model, { type, limit, offset }));
  return {
    pageInfo: {
      totalRecords: total,
      hasNextPage: offset + limit < total,
    },
    data,
  };
}

export function MailMessages({ formAtom, schema }: WidgetProps) {
  const [state, setState] = useState<{
    messages: Message[];
    hasNext: boolean;
    offset: number;
    limit: number;
    total: number;
  }>({
    messages: [],
    hasNext: false,
    offset: 0,
    limit: schema.limit ?? 10,
    total: 0,
  });
  const { name } = useViewAction();
  const { model, modelId: recordId } = schema;
  const { messages, offset, limit, hasNext, total } = state;
  const { fetchTags } = useTags();

  const [filter, setFilter] = useState<string | undefined>(schema.filter);

  const { data: meta } = useAsync(async () => await findFields(model), [model]);
  const isMessageBox = model === "com.axelor.mail.db.MailMessage";
  const folder = isMessageBox ? name.split(".").pop() : "";
  const hasMessages = isMessageBox || (recordId ?? 0) > 0;

  const flagAsRead = useCallback(
    async (messages: Message[]) => {
      const allFlags: MessageFlag[] = [];

      const pushUnread = (messages: Message[]) => {
        for (const message of messages) {
          const { $flags, $children } = message;
          const { isRead } = $flags ?? {};
          if (!isRead) {
            allFlags.push({
              isRead: true,
              messageId: message.id,
            } as MessageFlag);
          }
          if ($children?.length) {
            pushUnread($children);
          }
        }
      };

      pushUnread(messages);

      if (allFlags.length === 0) {
        return;
      }

      const allUpdatedFlags = await DataSource.flags(allFlags);

      if (!allUpdatedFlags?.length) {
        console.error(`Failed to flag ${allFlags.length} message(s) as read`);
        return;
      }

      // Merge flags with updated values
      for (let i = 0; i < allUpdatedFlags.length; ++i) {
        const flag = allFlags.find(
          (x) => x.messageId === allUpdatedFlags[i].messageId,
        );
        if (flag) {
          Object.assign(flag, allUpdatedFlags[i]);
        }
      }

      fetchTags();

      const findMessage = (
        messages: Message[],
        id?: number,
      ): Message | null => {
        for (const message of messages) {
          if (message.id === id) {
            return message;
          } else if (message.$children?.length) {
            const found = findMessage(message.$children, id);
            if (found) {
              return found;
            }
          }
        }
        return null;
      };

      setState((prev) => {
        const updated = [...prev.messages];
        for (const flags of allFlags) {
          const message = findMessage(updated, flags.messageId);
          if (!message) {
            console.error(`Failed to find message ${flags.messageId}`);
            continue;
          }
          message.$flags = {
            isRead: true,
            ...flags,
          } as MessageFlag;
        }
        return { ...prev, messages: updated };
      });
    },
    [fetchTags],
  );

  const fetchMessages = useAfterActions(findMessages);
  const fetchAll = useCallback(
    async (options?: MessageFetchOptions, reset = true) => {
      if (!hasMessages) return;
      const { parent } = options || {};

      const {
        data,
        pageInfo: { totalRecords, hasNextPage },
      } = await fetchMessages(recordId as number, model, {
        folder,
        type: filter,
        ...options,
      });

      if (parent) {
        setState((prev) => {
          const msgs = [...prev.messages];
          const msgInd = msgs.findIndex((x) => `${x.id}` === `${parent}`);
          if (msgInd > -1) {
            msgs[msgInd] = { ...msgs[msgInd], $children: data };
          }
          return { ...prev, messages: msgs };
        });
      } else {
        setState((prev) => ({
          ...prev,
          ...options,
          messages: reset
            ? data.map((msg: any) => ({
                ...(prev.messages.find((x) => x.id === msg.id) || {}),
                ...msg,
              }))
            : [...prev.messages, ...data],
          total: totalRecords,
          hasNext: hasNextPage,
        }));
        if (folder) {
          flagAsRead(data as Message[]);
        }
      }
    },
    [hasMessages, fetchMessages, recordId, model, folder, filter, flagAsRead],
  );

  const postComment = useCallback(
    async (_data: Message) => {
      const { relatedId, relatedModel, ...data } = _data;
      const result = await DataSource.add(
        relatedModel || model,
        relatedId || recordId!,
        {
          ...data,
          body: data.body?.replace(/\n/g, "<br>"),
        },
      );
      if (result) {
        const [msg] = result;
        if (!data.parent) {
          setState((prev) => {
            const filtered = prev.messages.filter((x) => x.id !== msg.id);
            const updated = [msg, ...filtered];
            const messageCount = updated.length - prev.messages.length;
            return {
              ...prev,
              messages: updated,
              ...(messageCount > 0 && {
                total: prev.total + messageCount,
                offset: prev.offset + messageCount,
              }),
            };
          });
        }
        return msg;
      }
    },
    [recordId, model],
  );

  const removeComment = useCallback(
    async (record: Message) => {
      const isRemoved = await DataSource.remove(record.id);
      if (!isRemoved) return;

      setState((prev) => {
        const msgs = [...prev.messages];
        const msgIndex = msgs.findIndex(
          (x) => `${x.id}` === `${record?.parent?.id || record.id}`,
        );
        if (msgIndex === -1) return prev;

        if (record?.parent?.id) {
          const $children = [...(msgs[msgIndex].$children || [])];
          const $ind = $children.findIndex((x) => `${x.id}` === `${record.id}`);
          if ($ind > -1) {
            $children.splice($ind, 1);
            msgs[msgIndex] = { ...msgs[msgIndex], $children };
          }
          return { ...prev, messages: msgs };
        } else {
          msgs.splice(msgIndex, 1);
          return {
            ...prev,
            messages: msgs,
            total: prev.total - 1,
            offset: prev.offset - 1,
          };
        }
      });
      fetchTags();
    },
    [fetchTags],
  );

  const handleFlagsAction = useCallback(
    async (msg: Message, attrs: Partial<MessageFlag>, reload = false) => {
      const { $flags } = msg;
      const allUpdatedFlags = await DataSource.flags([
        {
          ...attrs,
          messageId: msg.id,
        },
      ]);

      const updatedFlags = allUpdatedFlags?.[0];

      // handle error in update messages
      if (!updatedFlags || !updatedFlags.messageId) {
        console.error(`Failed to flag message ${msg.id}`);
        return;
      }

      if ($flags && $flags.isRead !== attrs.isRead) {
        fetchTags();
      }

      const flag = { ...attrs, ...updatedFlags };

      if (reload) {
        fetchAll({ offset: 0, limit }, true);
      } else {
        setState((prev) => {
          const messages = [...prev.messages];
          const msgIndex = messages.findIndex(
            (x) => `${x.id}` === `${msg?.parent?.id || msg.id}`,
          );
          messages[msgIndex] = {
            ...messages[msgIndex],
            ...(msg?.parent?.id
              ? {
                  ...(!flag.isRead && {
                    $flags: {
                      ...messages[msgIndex].$flags,
                      isRead: flag.isRead,
                    } as MessageFlag,
                  }),
                  $children: (messages[msgIndex].$children || []).map(($msg) =>
                    `${$msg.id}` === `${msg.id}`
                      ? {
                          ...$msg,
                          $flags: Object.assign({}, $msg.$flags, flag),
                        }
                      : $msg,
                  ),
                }
              : {
                  $flags: Object.assign({}, messages[msgIndex].$flags, flag),
                }),
          };
          return { ...prev, messages };
        });
      }

      return flag as MessageFlag;
    },
    [fetchAll, fetchTags, limit],
  );

  const onRefresh = useCallback(() => {
    fetchAll({ limit, offset: 0 });
  }, [fetchAll, limit]);

  const loadMore = useCallback(() => {
    fetchAll({ offset: offset + limit, limit }, false);
  }, [fetchAll, offset, limit]);

  useAsyncEffect(async () => {
    onRefresh();
  }, [onRefresh]);

  // Subscribe to mail channel (SUB on mount, UNS on unmount)
  useEffect(() => {
    const mailChannelService = getMailChannelService();
    return mailChannelService.subscribe();
  }, []);

  // Join/leave room on recordId change (JOIN/LEFT only)
  useEffect(() => {
    if (isMessageBox || !recordId || recordId <= 0) return;

    const mailChannelService = getMailChannelService();
    return mailChannelService.joinRoom(
      model,
      recordId,
      (messages) => {
        setState((prev) => {
          const existingIds = new Set(prev.messages.map((m) => m.id));
          const newMessages = messages.filter((m) => !existingIds.has(m.id));
          if (newMessages.length === 0) return prev;
          return {
            ...prev,
            messages: [...newMessages, ...prev.messages],
            total: prev.total + newMessages.length,
            offset: prev.offset + newMessages.length,
          };
        });
      },
      (deletedIds) => {
        const deletedSet = new Set(deletedIds);
        setState((prev) => {
          let deletedCount = 0;
          const messages = prev.messages
            .filter((m) => {
              if (deletedSet.has(m.id!)) {
                deletedCount++;
                return false;
              }
              return true;
            })
            .map((m) => {
              if (!m.$children?.length) return m;
              const $children = m.$children.filter(
                (c) => !deletedSet.has(c.id!),
              );
              return $children.length !== m.$children.length
                ? { ...m, $children }
                : m;
            });
          if (deletedCount === 0 && messages.length === prev.messages.length)
            return prev;
          return {
            ...prev,
            messages,
            total: prev.total - deletedCount,
            offset: Math.max(0, prev.offset - deletedCount),
          };
        });
      },
    );
  }, [model, recordId, isMessageBox]);

  // Stable string encoding only id/relatedModel/relatedId of top-level messages.
  // Doesn't change when $children are updated, so deletions don't cause re-joining.
  const messageBoxRoomKey = useMemo(() => {
    if (!isMessageBox) return "";
    return messages
      .filter((m) => m.relatedId && m.relatedModel)
      .map((m) => `${m.id}::${m.relatedModel}::${m.relatedId}`)
      .join("|");
  }, [isMessageBox, messages]);

  const messageBoxRooms = useMemo(() => {
    return messageBoxRoomKey
      .split("|")
      .filter(Boolean)
      .map((key) => {
        const [messageId, relatedModel, relatedId] = key.split("::");
        return {
          messageId: Number(messageId),
          relatedModel,
          relatedId: Number(relatedId),
        };
      });
  }, [messageBoxRoomKey]);

  // Join/leave rooms for MessageBox threads to handle DELETED events
  useEffect(() => {
    if (!messageBoxRooms.length) return;

    const mailChannelService = getMailChannelService();
    const cleanups = messageBoxRooms.map(
      ({ relatedId, relatedModel, messageId }) =>
        mailChannelService.joinRoom(
          relatedModel,
          relatedId,
          (newMessages) => {
            setState((prev) => {
              const msgs = [...prev.messages];
              const idx = msgs.findIndex((m) => m.id === messageId);
              if (idx === -1) return prev;
              const existingIds = new Set(
                (msgs[idx].$children || []).map((c) => c.id),
              );
              const toAdd = newMessages.filter((m) => !existingIds.has(m.id));
              if (!toAdd.length) return prev;
              msgs[idx] = {
                ...msgs[idx],
                $children: [
                  ...toAdd.map((msg) => ({ ...msg, $thread: true })),
                  ...(msgs[idx].$children || []),
                ],
                $numReplies: (msgs[idx].$numReplies ?? 0) + toAdd.length,
              };
              return { ...prev, messages: msgs };
            });
          },
          (deletedIds) => {
            const deletedSet = new Set(deletedIds);
            setState((prev) => {
              const msgs = [...prev.messages];
              const idx = msgs.findIndex((m) => m.id === messageId);
              if (idx === -1) return prev;
              const $children = (msgs[idx].$children || []).filter(
                (c) => !deletedSet.has(c.id!),
              );
              if ($children.length === msgs[idx].$children?.length) return prev;
              const deletedCount =
                (msgs[idx].$children?.length ?? 0) - $children.length;
              msgs[idx] = {
                ...msgs[idx],
                $children,
                $numReplies: Math.max(
                  0,
                  (msgs[idx].$numReplies ?? 0) - deletedCount,
                ),
              };
              return { ...prev, messages: msgs };
            });
          },
        ),
    );

    return () => cleanups.forEach((c) => c());
  }, [messageBoxRooms]);

  return (
    <>
      {isMessageBox && (
        <MessageBoxUpdates
          count={total}
          formAtom={formAtom}
          onRefresh={onRefresh}
        />
      )}
      {(!isMessageBox || total > 0) && (
        <MessageBox
          fields={meta?.fields}
          jsonFields={meta?.jsonFields}
          data={messages}
          isMail={isMessageBox}
          filter={filter}
          onFilterChange={setFilter}
          onFetch={fetchAll}
          onComment={postComment}
          onCommentRemove={removeComment}
          onAction={handleFlagsAction}
          {...(hasNext ? { onLoad: loadMore } : {})}
        />
      )}
    </>
  );
}

function MessageBoxUpdates({
  count,
  formAtom,
  onRefresh,
}: {
  count: number;
  formAtom: FormAtom;
  onRefresh: () => void;
}) {
  const [__empty, setEmpty] = useAtom(
    useMemo(
      () =>
        focusAtom(
          formAtom,
          ({ record }) => record.__empty,
          ({ record, ...rest }, __empty) => ({
            ...rest,
            record: { ...record, __empty },
          }),
        ),
      [formAtom],
    ),
  );
  const empty = count === 0;
  const shouldMarkEmpty = empty !== __empty;
  useEffect(() => {
    shouldMarkEmpty && setEmpty(empty);
  }, [shouldMarkEmpty, empty, setEmpty]);

  const tab = useViewTab();
  const { active } = useTabs();
  const { unread } = useTagsMail();
  const unreadRef = useRef(unread);

  useEffect(() => {
    unreadRef.current = unread;
  }, [unread]);

  useEffect(() => {
    if (active === tab && unreadRef.current) {
      const event = new CustomEvent("tab:refresh", { detail: { id: tab.id } });
      document.dispatchEvent(event);
    }
  }, [active, tab, onRefresh]);

  // register tab:refresh
  useViewTabRefresh("form", onRefresh);

  return null;
}
