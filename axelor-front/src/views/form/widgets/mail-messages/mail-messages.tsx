import { useAtom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import noop from "lodash/noop";
import { useCallback, useEffect, useMemo, useState } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useTags } from "@/hooks/use-tags";
import {
  useViewAction,
  useViewTabRefresh,
} from "@/view-containers/views/scope";

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
    useMemo(() => focusAtom(formAtom, (o) => o.prop("fields")), [formAtom]),
  );
  const isMessageBox = model === "com.axelor.mail.db.MailMessage";
  const folder = isMessageBox ? name.split(".").pop() : "";
  const hasMessages = isMessageBox || (recordId ?? 0) > 0;

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
      }
    },
    [hasMessages, fetchMessages, recordId, model, folder, filter],
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
            messages[msgIndex] = { ...messages[msgIndex], $children: $children };
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
        fetchTags();
      }

      const flag = { ...attrs, version: undefined };
      // version is outdated after flags update

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

  // register tab:refresh
  useViewTabRefresh("form", isMessageBox ? onRefresh : noop);

  const { total } = pagination;

  return (
    <>
      {isMessageBox && <MessageBoxUpdates count={total} formAtom={formAtom} />}
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
}: {
  count: number;
  formAtom: FormAtom;
}) {
  const [__empty, setEmpty] = useAtom(
    useMemo(
      () => focusAtom(formAtom, (o) => o.prop("record").prop("__empty")),
      [formAtom],
    ),
  );
  const empty = count === 0;
  const shouldMarkEmpty = empty !== __empty;
  useEffect(() => {
    shouldMarkEmpty && setEmpty(empty);
  }, [shouldMarkEmpty, empty, setEmpty]);

  return null;
}
