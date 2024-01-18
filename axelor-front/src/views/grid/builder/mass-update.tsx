import _ from "lodash";
import { useMemo, useState, useId } from "react";

import {
  Box,
  Button,
  ClickAwayListener,
  Divider,
  Input,
  InputLabel,
  Link,
  Popper,
} from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Select } from "@/components/select";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { Field, GridView, Property } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";
import { Widget } from "@/view-containers/advance-search/editor/components";

import styles from "./mass-update.module.scss";

type MassUpdateItem = {
  field?: null | Property;
  value?: any;
};

const getNewItem = () => ({ field: null, value: null }) as MassUpdateItem;

export function useMassUpdateFields(
  fields: Record<string, Property> | undefined,
  items: GridView["items"],
) {
  return useMemo(() => {
    const _fields: Property[] = [];
    const accept = (field: Property | undefined, item: Field) => {
      if (
        !field ||
        !(item.massUpdate ?? field.massUpdate) ||
        /^(id|version|selected|archived|((updated|created)(On|By)))$/.test(
          field.name,
        ) ||
        (field as any).large ||
        field.unique ||
        ["BINARY", "ONE_TO_MANY", "MANY_TO_MANY"].includes(field.type)
      ) {
        return;
      }
      _fields.push({
        ...field,
        ...item,
        type: field.type,
        placeholder: item.placeholder ?? item?.title ?? field.title,
      });
    };
    _.each(items, (item) => {
      if (item.type === "field") {
        accept(fields?.[item.name!], item as Field);
      }
    });

    return _fields;
  }, [fields, items]);
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
  const hasAllId = useId();
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
        ind === index ? { ...item, [name]: value } : item,
      ),
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
        (obj, item) =>
          item.field ? { ...obj, [item.field.name]: item.value } : obj,
        {},
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
        <Box className={styles.container} bg="body" p={2}>
          <Box d="flex" alignItems="center">
            <Box as="p" mb={0} p={1} flex={1} fontWeight="bold">
              {i18n.get("Mass Update")}
            </Box>
            <Box as="span" className={styles.icon} onClick={onClose}>
              <MaterialIcon icon="close" />
            </Box>
          </Box>
          <Divider />
          <Box p={2} py={1}>
            <Box as="table" w={100}>
              <tbody>
                {items.map((item, index) => {
                  const { field } = item;
                  const options = fields.filter(
                    (f) => !selectedItems.includes(f) || f === field,
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
                          multiple={false}
                          placeholder={i18n.get("Select")}
                          options={options}
                          optionKey={(x) => x.name}
                          optionLabel={(x) => x.title ?? x.autoTitle ?? x.name}
                          optionEqual={(x, y) => x.name === y.name}
                          onChange={(value) => {
                            onChange(
                              "field",
                              fields.find((x) => x.name === value?.name),
                              index,
                            );
                          }}
                          value={field}
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
                    id={hasAllId}
                    m={0}
                    type="checkbox"
                    checked={isUpdateAll}
                    onChange={(e) => setUpdateAll(!isUpdateAll)}
                  />
                  <InputLabel
                    ms={1}
                    mb={0}
                    alignItems="center"
                    htmlFor={hasAllId}
                  >
                    {i18n.get("Update all")}
                  </InputLabel>
                </Box>
              </Box>
            </Box>
          </Box>
        </Box>
      </ClickAwayListener>
    </Popper>
  );
}
