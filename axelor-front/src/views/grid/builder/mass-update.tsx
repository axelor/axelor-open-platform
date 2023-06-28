import { useMemo, useState } from "react";
import {
  Box,
  Button,
  ClickAwayListener,
  Divider,
  Input,
  Popper,
  Link,
} from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Property } from "@/services/client/meta.types";
import {
  Select,
  Widget,
} from "@/view-containers/advance-search/editor/components";
import { i18n } from "@/services/client/i18n";
import styles from "./mass-update.module.scss";
import { toKebabCase } from "@/utils/names";
import { DataRecord } from "@/services/client/data.types";

type MassUpdateItem = {
  field?: null | Property;
  value?: any;
};

const getNewItem = () => ({ field: null, value: null } as MassUpdateItem);

export function useMassUpdateFields(fields?: Record<string, Property>) {
  return useMemo(
    () =>
      Object.keys(fields ?? {}).reduce(($fields, key) => {
        const field = fields?.[key];
        const { name = "" } = field || {};
        if (
          !field?.massUpdate ||
          /^(id|version|selected|archived|((updated|created)(On|By)))$/.test(
            name
          ) ||
          (field as any).large ||
          field.unique ||
          ["BINARY", "ONE_TO_MANY", "MANY_TO_MANY"].includes(field.type)
        ) {
          return $fields;
        }
        return [...$fields, field];
      }, [] as Property[]),
    [fields]
  );
}

export function MassUpdater({
  open,
  target,
  fields,
  onUpdate,
  onClose,
}: {
  open?: boolean;
  target?: HTMLElement | null;
  fields: Property[];
  onUpdate?: (values: Partial<DataRecord>, isAll?: boolean) => void;
  onClose: () => void;
}) {
  const [items, setItems] = useState<MassUpdateItem[]>([getNewItem()]);
  const [isUpdateAll, setUpdateAll] = useState(false);

  function onAdd() {
    if (items.length < fields.length) {
      const lastItem = [...items].pop();
      if (!lastItem?.field) return;
      setItems((items) => [...items, getNewItem()]);
    }
  }

  function onClear() {
    setItems([getNewItem()]);
  }

  function onChange(name: string, value: any, index: number) {
    setItems((items) =>
      items.map((item, ind) =>
        ind === index ? { ...item, [name]: value } : item
      )
    );
  }

  function onRemove(index: number) {
    if (items.length === 1) {
      return onClear();
    }
    setItems((items) => items.filter((_, i) => index !== i));
  }

  function handleUpdate() {
    const values = items
      .filter((x) => x.field)
      .reduce(
        (obj, item) => ({
          ...obj,
          [item.field?.name!]: item.value,
        }),
        {}
      );
    onUpdate?.(values, isUpdateAll);
  }

  const selectedItems = items
    .filter((item) => item.field)
    .map((item) => item.field);

  return (
    <Popper
      bg="body"
      open={open}
      target={target!}
      placement={`bottom-start`}
      offset={[0, 8]}
    >
      <ClickAwayListener onClickAway={onClose}>
        <Box className={styles.container} bg="body">
          <Box className={styles.title} p={2} py={1}>
            {i18n.get("Mass Update")}
          </Box>
          <Divider />
          <Box p={2} py={1}>
            <Box as="table" w={100}>
              <tbody>
                {items.map((item, index) => {
                  const { field } = item;
                  const options = fields.filter(
                    (f) => !selectedItems.includes(f) || f === field
                  );
                  return (
                    <tr key={index}>
                      <td
                        width={20}
                        className={styles.remove}
                        onClick={() => onRemove(index)}
                      >
                        <MaterialIcon icon="close" />
                      </td>
                      <td>
                        <Select
                          placeholder={i18n.get("Select")}
                          onChange={(e: any) => {
                            onChange(
                              "field",
                              fields.find((f) => f.name === e) ?? null,
                              index
                            );
                          }}
                          value={field?.name}
                          options={options}
                          optionLabel={"title"}
                          optionValue={"name"}
                        />
                      </td>
                      <td>
                        {field && (
                          <Widget
                            {...{
                              operator: "=",
                              className: styles.widget,
                              placeholder: field.placeholder || field.title,
                              type: toKebabCase(field.type),
                              field,
                              value: item,
                              onChange: (e: any) =>
                                onChange("value", e?.value ?? null, index),
                            }}
                          />
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </Box>
            <Box d="flex" ps={4} mt={2} style={{ height: 20 }}>
              <Box>
                <Link mx={1} onClick={onAdd}>
                  {i18n.get("Add Field")}
                </Link>
              </Box>
              <Divider vertical />
              <Box>
                <Link mx={1} onClick={onClear}>
                  {i18n.get("Clear")}
                </Link>
              </Box>
            </Box>
          </Box>
          <Divider />
          <Box p={2}>
            <Box d="flex" ms={1}>
              <Button
                outline
                size="sm"
                variant="primary"
                disabled={selectedItems.length === 0}
                onClick={handleUpdate}
              >
                {i18n.get("Update")}
              </Button>
              <Button
                outline
                ms={2}
                size="sm"
                variant="primary"
                onClick={onClose}
              >
                {i18n.get("Cancel")}
              </Button>
              <Box d="flex" ms={2}>
                <Box d="flex" alignItems="center">
                  <Input
                    m={0}
                    type="checkbox"
                    checked={isUpdateAll}
                    onChange={(e) => setUpdateAll(!isUpdateAll)}
                  />
                </Box>
                <Box d="flex" alignItems="center" ms={1}>
                  {i18n.get("Update all")}
                </Box>
              </Box>
            </Box>
          </Box>
        </Box>
      </ClickAwayListener>
    </Popper>
  );
}
