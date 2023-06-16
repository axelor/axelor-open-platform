import { ReactElement, memo, useCallback, useEffect, useState } from "react";
import { Box, Button, Link } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { useAtomCallback } from "jotai/utils";
import { useSetAtom } from "jotai";
import clsx from "clsx";

import { TreeRecord } from "./types";
import { i18n } from "@/services/client/i18n";
import { Formatters } from "@/utils/format";
import { ViewerInput } from "../../form/widgets/string/viewer";
import { DataRecord } from "@/services/client/data.types";
import { legacyClassNames } from "@/styles/legacy";
import { useAsync } from "@/hooks/use-async";
import { findView } from "@/services/client/meta-cache";
import { Form, useFormHandlers } from "../../form/builder";
import { FormView } from "@/services/client/meta.types";
import { ViewData } from "@/services/client/meta";
import { useViewDirtyAtom } from "@/view-containers/views/scope";
import styles from "./dms-details.module.scss";

const tagsFormName = "dms-file-tags-form";

interface TagFormProps {
  meta: ViewData<FormView>;
  record: DataRecord;
  onSave: (data: DataRecord) => void;
}
export const DmsDetails = memo(function DmsDetails({
  open,
  model,
  data,
  onSave,
  onView,
  onClose,
}: {
  open?: boolean;
  model?: string;
  data?: TreeRecord | null;
  onSave?: (data: TreeRecord) => void;
  onView?: (data: TreeRecord) => void;
  onClose?: () => void;
}) {
  const [edit, setEdit] = useState(false);
  const { fileName, createdBy, createdOn, updatedOn, isDirectory, tags } =
    data || {};

  const handleEdit = useCallback(() => {
    setEdit(true);
  }, []);

  const handleSave = useCallback(
    (data: DataRecord) => {
      setEdit(false);
      onSave?.(data);
    },
    [onSave]
  );

  useEffect(() => {
    data && setEdit(false);
  }, [data]);

  function renderField(title: string, value: string) {
    return (
      <Box>
        <Box as="label" mb={1} color="secondary">
          {title}
        </Box>
        <ViewerInput value={value} />
      </Box>
    );
  }

  return (
    <Box
      className={clsx(styles.drawer, {
        [styles.show]: open,
      })}
      shadow
      borderTop
      bgColor="body"
    >
      {data && (
        <>
          <Box p={2} d="flex" alignItems="center">
            <Box className={styles.title} flex={1} m={0}>
              <Box as="span" fontWeight={"bold"}>
                {fileName}
              </Box>
            </Box>
            <MaterialIcon
              className={styles.close}
              icon="close"
              onClick={onClose}
            />
          </Box>
          <Box p={2} borderTop>
            {renderField(
              i18n.get("Type"),
              isDirectory ? i18n.get("Directory") : i18n.get("File")
            )}
            {renderField(i18n.get("Owner"), createdBy?.name)}
            {renderField(i18n.get("Created"), Formatters.datetime(createdOn))}
            {renderField(i18n.get("Modified"), Formatters.datetime(updatedOn))}
          </Box>
          <Box borderTop borderBottom py={3} px={2}>
            <Link className={styles.link} onClick={() => onView?.(data)}>
              {i18n.get("Details")}
            </Link>
          </Box>
          <Box d="flex" mt={2} px={1}>
            {edit ? (
              <Box>
                <TagsFormView model={model} record={data} onSave={handleSave} />
              </Box>
            ) : tags?.length > 0 ? (
              <>
                <Box d="flex" flexWrap="wrap">
                  {tags?.map?.((tag: DataRecord) => (
                    <Box
                      key={tag.id}
                      ms={1}
                      mt={1}
                      className={legacyClassNames("label", tag.style)}
                    >
                      {tag.name}
                    </Box>
                  ))}
                  <Box
                    d="flex"
                    ms={1}
                    mt={1}
                    alignItems="center"
                    className={styles.icon}
                    title={i18n.get("Edit")}
                    onClick={handleEdit}
                  >
                    <MaterialIcon icon="edit" fill fontSize={"1rem"} />
                  </Box>
                </Box>
              </>
            ) : (
              <Box>
                <Button size="sm" variant="link" onClick={handleEdit}>
                  {i18n.get("Add some tags")}
                </Button>
              </Box>
            )}
          </Box>
        </>
      )}
    </Box>
  );
});

function TagsFormView({
  model,
  record,
  onSave,
}: Pick<TagFormProps, "record" | "onSave"> & {
  model?: string;
}) {
  const { data: meta } = useAsync(
    async () =>
      await findView<FormView>({
        type: "form",
        name: tagsFormName,
        model,
      }),
    [model]
  );
  return (meta && (
    <TagsForm meta={meta} record={record} onSave={onSave} />
  )) as ReactElement;
}

function TagsForm({ meta, record, onSave }: TagFormProps) {
  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, record);
  const setDirty = useSetAtom(useViewDirtyAtom());

  const handleSave = useAtomCallback(
    useCallback(
      async (get) => {
        const { record: data } = get(formAtom);
        const { id, version, tags } = data;

        onSave({
          id,
          version,
          tags,
        });
        setDirty(false);
      },
      [formAtom, onSave, setDirty]
    )
  );

  useEffect(() => {
    return () => {
      setDirty(false);
    };
  }, [setDirty]);

  return (
    <>
      <Form
        schema={meta.view}
        fields={meta.fields!}
        readonly={false}
        formAtom={formAtom}
        actionHandler={actionHandler}
        actionExecutor={actionExecutor}
        recordHandler={recordHandler}
        {...({} as any)}
      />
      <Box>
        <MaterialIcon
          icon="done"
          fill
          className={styles.icon}
          onClick={handleSave}
        />
      </Box>
    </>
  );
}
