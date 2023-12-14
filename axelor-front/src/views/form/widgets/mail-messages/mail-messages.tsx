import { useAtom, useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useTabs } from "@/hooks/use-tabs";
import { useTags, useTagsMail } from "@/hooks/use-tags";
import {
  useViewAction,
  useViewTab,
  useViewTabRefresh,
} from "@/view-containers/views/scope";

import { focusAtom } from "@/utils/atoms";
import { FormAtom, WidgetProps } from "../../builder";
import { useAfterActions, useFormRefresh } from "../../builder/scope";
import { MessageBox } from "./message";
import { Message, MessageFetchOptions, MessageFlag } from "./message/types";
import { DataSource } from "./utils";

async function findMessages(
  id: number,
  model: string,
  { parent, folder, type, offset = 0, limit = 4 }: MessageFetchOptions,
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
  const [messages, setMessages] = useState<Message[]>([]);
  const [pagination, setPagination] = useState({
    hasNext: false,
    offset: 0,
    limit: schema.limit ?? 10,
    total: 0,
  });
  const { name } = useViewAction();
  const { model, modelId: recordId } = schema;
  const { offset, limit } = pagination;
  const { fetchTags } = useTags();

  const [filter, setFilter] = useState<string | undefined>(schema.filter);

  const fields = useAtomValue(
    useMemo(() => selectAtom(formAtom, (o) => o.fields), [formAtom]),
  );
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

      setMessages((messages) => {
        for (const flags of allFlags) {
          const message = findMessage(messages, flags.messageId);
          if (!message) {
            console.error(`Failed to find message ${flags.messageId}`);
            continue;
          }
          message.$flags = {
            isRead: true,
            ...flags,
          } as MessageFlag;
        }
        return [...messages];
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
        setMessages((msgs) => {
          const msgInd = msgs.findIndex((x) => `${x.id}` === `${parent}`);
          if (msgInd > -1) {
            msgs[msgInd] = { ...msgs[msgInd], $children: data };
          }
          return [...msgs];
        });
      } else {
        setMessages((msgs) =>
          reset
            ? data.map((msg: any) => ({
                ...(msgs.find((x) => x.id === msg.id) || {}),
                ...msg,
              }))
            : [...msgs, ...data],
        );
        setPagination((pagination) => ({
          ...pagination,
          ...options,
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
          setMessages((msgs) => [msg, ...msgs]);
          setPagination((pager) => ({
            ...pager,
            total: pager.total + 1,
            offset: pager.offset + 1,
          }));
        }
        return msg;
      }
    },
    [recordId, model],
  );

  const removeComment = useCallback(
    async (record: Message) => {
      const isRemoved = await DataSource.remove(record.id);
      const msgIndex = messages.findIndex(
        (x) => `${x.id}` === `${record?.parent?.id || record.id}`,
      );
      if (isRemoved && msgIndex > -1) {
        if (record?.parent?.id) {
          const $children = messages[msgIndex].$children || [];
          const $ind = $children.findIndex((x) => `${x.id}` === `${record.id}`);
          if ($ind > -1) {
            $children.splice($ind, 1);
            messages[msgIndex] = {
              ...messages[msgIndex],
              $children: $children,
            };
          }
        } else {
          messages.splice(msgIndex, 1);
          setPagination((pager) => ({
            ...pager,
            total: pager.total - 1,
            offset: pager.offset - 1,
          }));
        }
        setMessages([...messages]);
      }
      fetchTags();
    },
    [messages, fetchTags],
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
        setMessages((messages) => {
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
          return [...messages];
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

  // register form:refresh
  useFormRefresh(onRefresh);

  const { total } = pagination;

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
          fields={fields}
          data={messages}
          isMail={isMessageBox}
          filter={filter}
          onFilterChange={setFilter}
          onFetch={fetchAll}
          onComment={postComment}
          onCommentRemove={removeComment}
          onAction={handleFlagsAction}
          {...(pagination.hasNext ? { onLoad: loadMore } : {})}
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
