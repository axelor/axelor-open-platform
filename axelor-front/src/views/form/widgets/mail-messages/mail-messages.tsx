import { WidgetProps } from "../../builder";
import { useAtomValue } from "jotai";
import { useCallback, useMemo, useState } from "react";
import { DataSource } from "./utils";
import { focusAtom } from "jotai-optics";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { MessageBox } from "./message";
import { Message, MessageFetchOptions, MessageFlag } from "./message/types";

function getMessages(
  id: number,
  model: string,
  { parent, folder, offset = 0, limit = 4 }: MessageFetchOptions
) {
  return (
    parent
      ? DataSource.replies(parent)
      : folder
      ? DataSource.folder(folder!, limit, offset)
      : DataSource.messages(id, model, limit, offset)
  ).then(({ total = 0, data = [] }) => ({
    pageInfo: {
      totalRecords: total,
      hasNextPage: offset + limit < total,
    },
    data,
  }));
}

export function MailMessages({ formAtom, schema }: WidgetProps) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [pagination, setPagination] = useState({
    hasNext: false,
    offset: 0,
    limit: 4,
    total: 0,
  });
  const { offset, limit } = pagination;
  const recordId = useAtomValue(
    useMemo(
      () => focusAtom(formAtom, (o) => o.prop("record").prop("id")),
      [formAtom]
    )
  );
  const fields = useAtomValue(
    useMemo(() => focusAtom(formAtom, (o) => o.prop("fields")), [formAtom])
  );
  const model = useAtomValue(
    useMemo(() => focusAtom(formAtom, (o) => o.prop("model")), [formAtom])
  );
  const msgFolder = "";
  const hasMessages = true;

  const fetchAll = useCallback(
    async (options?: MessageFetchOptions, reset = true) => {
      if (!hasMessages) return;
      const { parent } = options || {};
      const {
        data,
        pageInfo: { totalRecords, hasNextPage },
      } = await getMessages(recordId as number, model, {
        folder: msgFolder,
        ...options,
      });
      if (parent) {
        setMessages((msgs) => {
          const msgInd = msgs.findIndex((x) => `${x.id}` === `${parent}`);
          if (msgInd > -1) {
            msgs[msgInd] = { ...msgs[msgInd], $data: data };
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
            : [...msgs, ...data]
        );
        setPagination((pagination) => ({
          ...pagination,
          ...options,
          total: totalRecords,
          hasNext: hasNextPage,
        }));
      }
    },
    [hasMessages, recordId, model, msgFolder]
  );

  const postComment = useCallback(
    async (_data: Message) => {
      const { relatedId, relatedModel, ...data } = _data;
      const result = await DataSource.add(
        relatedModel || model,
        relatedId || recordId!,
        data
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
    [recordId, model]
  );

  const removeComment = useCallback(
    async (record: Message) => {
      const isRemoved = await DataSource.remove(record.id);
      const msgIndex = messages.findIndex(
        (x) => `${x.id}` === `${record?.parent?.id || record.id}`
      );
      if (isRemoved && msgIndex > -1) {
        if (record?.parent?.id) {
          const $data = messages[msgIndex].$data || [];
          const $ind = $data.findIndex((x) => `${x.id}` === `${record.id}`);
          if ($ind > -1) {
            $data.splice($ind, 1);
            messages[msgIndex] = { ...messages[msgIndex], $data };
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
      // TODO: refresh tags
    },
    [messages]
  );

  const handleFlagsAction = useCallback(
    async (msg: Message, attrs: Partial<MessageFlag>, reload = false) => {
      const { $flags } = msg;
      const isUpdated = await DataSource.flags([
        {
          ...attrs,
          id: attrs.id!,
          version: undefined,
          messageId: msg.id,
        },
      ]);

      // handle error in update messages
      if (!isUpdated) {
        return;
      }

      if ($flags && $flags.isRead !== attrs.isRead) {
        // TODO: refresh tags
      }

      const flag = { ...attrs, version: undefined };
      // version is outdated after flags update

      if (reload) {
        fetchAll({ offset: 0, limit }, true);
      } else {
        setMessages((messages) => {
          const msgIndex = messages.findIndex(
            (x) => `${x.id}` === `${msg?.parent?.id || msg.id}`
          );
          messages[msgIndex] = {
            ...messages[msgIndex],
            ...(msg?.parent?.id
              ? {
                  $data: (messages[msgIndex].$data || []).map(($msg) =>
                    `${$msg.id}` === `${msg.id}`
                      ? {
                          ...$msg,
                          $flags: Object.assign({}, $msg.$flags, flag),
                        }
                      : $msg
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
    [fetchAll, limit]
  );

  const loadMore = useCallback(() => {
    fetchAll({ offset: offset + limit, limit }, false);
  }, [offset, limit, fetchAll]);

  useAsyncEffect(async () => {
    fetchAll({ ...pagination, offset: 0 });
  }, [fetchAll]);

  return (
    <MessageBox
      fields={fields}
      data={messages}
      isMail={false}
      onFetch={fetchAll}
      onComment={postComment}
      onCommentRemove={removeComment}
      onAction={handleFlagsAction}
      {...(pagination.hasNext ? { onLoad: loadMore } : {})}
    />
  );
}
