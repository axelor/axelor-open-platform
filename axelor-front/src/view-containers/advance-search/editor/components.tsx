import clsx from "clsx";
import React, { useEffect, useRef } from "react";
import isNumber from "lodash/isNumber";
import { Select as AxSelect, Box, Input } from "@axelor/ui";

import { DataStore } from "@/services/client/data-store";
import { moment } from "@/services/client/l10n";
import { i18n } from "@/services/client/i18n";
import { toKebabCase } from "@/utils/names";
import { useDataStore } from "@/hooks/use-data-store";
import styles from "./components.module.css";

function TextField(props: any) {
  return (
    <Input
      {...props}
      value={props.value ?? ""}
      onChange={(e) => props.onChange(e.target.value)}
    />
  );
}

function DateTimePicker(props: any) {
  return (
    <TextField
      {...props}
      type="date"
      value={
        props.value ? moment(props.value).format("YYYY-MM-DD") : props.value
      }
    />
  );
}

function NumberField(props: any) {
  return <TextField {...props} type="number" />;
}

export function Select({ value, options, onChange, className, ...props }: any) {
  return (
    <Box
      as={AxSelect}
      classNamePrefix="ax-select"
      className={clsx(styles.select, className)}
      isClearable={false}
      options={options}
      optionLabel="title"
      optionValue="name"
      onChange={(option: any) => onChange(option?.name)}
      value={options.find((o: any) => o.name === value) || null}
      icons={[
        {
          id: "more",
          icon: "arrow_drop_down",
        },
      ]}
      {...props}
    />
  );
}

export function BooleanRadio({ name, onChange, value: valueProp, data }: any) {
  return (
    <Box d="flex" alignItems="center" ms={1} me={1}>
      {data.map(({ value, label }: any, index: number) => (
        <Box d="flex" alignItems="center" key={index} me={2}>
          <Input
            type="radio"
            value={value}
            checked={value === valueProp}
            onChange={onChange}
            name={name}
            m={0}
            me={2}
          />
          <Box as="p" mb={0}>
            {label}
          </Box>
        </Box>
      ))}
    </Box>
  );
}

export function SimpleWidget({
  component: Component,
  operator,
  onChange,
  value,
  value2,
  style,
  ...rest
}: any) {
  if (["=", "!=", ">", ">=", "<", "<=", "like", "notLike"].includes(operator)) {
    return (
      <Component
        name="value"
        onChange={(value: any) => onChange({ name: "value", value: value })}
        value={value}
        {...rest}
      />
    );
  }

  if (["between", "notBetween"].includes(operator)) {
    return (
      <>
        <Component
          name="value"
          style={{ ...style }}
          onChange={(value: any) => onChange({ name: "value", value })}
          value={value}
          {...rest}
        />

        <Component
          name="value2"
          onChange={(value: any) => onChange({ name: "value2", value })}
          value={value2}
          {...rest}
        />
      </>
    );
  }

  return null;
}

export function RelationalWidget({ operator, onChange, ...rest }: any) {
  const { field, value } = rest;
  const dataStore = useRef(new DataStore(field.target, {})).current;
  const options = useDataStore(dataStore, (res) => res.records);

  const fetchData = React.useCallback(
    async () =>
      dataStore.search({
        filter: {
          _domain: undefined,
          _domainContext: {},
        },
      }),
    [dataStore]
  );

  const isTextField = ["like", "notLike"].includes(operator);
  const isSelection = ["=", "in", "notIn"].includes(operator);

  useEffect(() => {
    if (isSelection) {
      const ids = value?.filter?.((id: any) => isNumber(id));
      if (ids?.length) {
        dataStore.search({
          filter: {
            _domain: "self.id in (:_ids)",
            _domainContext: {
              _ids: ids as number[],
            },
          },
        });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isSelection, dataStore]);

  if (isTextField) {
    return (
      <TextField
        name="value"
        onChange={(value: any) => onChange({ name: "value", value: value })}
        {...rest}
      />
    );
  } else if (isSelection) {
    const { isMulti = operator !== "=", field, value, className } = rest;
    const { targetName } = field;
    const $value = value?.map?.((id: any) =>
      isNumber(id)
        ? options.find((opt) => opt.id === id) || {
            id,
          }
        : id
    );
    return (
      <AxSelect
        placeholder={operator === "=" ? rest.placeholder : ""}
        className={clsx(styles.select, className)}
        optionLabel={targetName}
        optionValue="id"
        value={$value}
        isMulti={isMulti}
        options={options}
        onFocus={fetchData}
        onChange={(value) =>
          onChange({
            name: "value",
            value: Array.isArray(value)
              ? value.map((x) => ({ id: x.id, [targetName]: x[targetName] }))
              : value && { id: value.id, [targetName]: value[targetName] },
          })
        }
      />
    );
  }
  return null;
}

const CurrentSelection = [
  { name: "day", title: i18n.get("Day") },
  { name: "week", title: i18n.get("Week") },
  { name: "month", title: i18n.get("Month") },
  { name: "quarter", title: i18n.get("Quarter") },
  { name: "year", title: i18n.get("Year") },
];

const PastOrNextSelection = [
  { name: "day", title: i18n.get("Days") },
  { name: "week", title: i18n.get("Weeks") },
  { name: "month", title: i18n.get("Months") },
  { name: "quarter", title: i18n.get("Quarters") },
  { name: "year", title: i18n.get("Years") },
];

export function Widget({ type, operator, onChange, value, ...rest }: any) {
  const props = {
    operator,
    value: value.value,
    value2: value.value2,
    timeUnit: value.timeUnit,
    onChange,
    ...rest,
  };

  switch (toKebabCase(type)) {
    case "one-to-one":
    case "many-to-one":
    case "many-to-many":
    case "one-to-many":
      return <RelationalWidget {...props} />;
    case "date":
    case "time":
    case "datetime":
      const dateFormats = {
        date: ["YYYY-MM-DD", "MM/DD/YYYY"],
        datetime: ["YYYY-MM-DD", "MM/DD/YYYY"],
        time: ["HH:mm", "LT"],
      };
      const [format, displayFormat] =
        dateFormats[type as "date" | "datetime" | "time"];
      const { value, value2, timeUnit, onChange } = props;

      function renderSelect() {
        const props = {
          isClearOnDelete: false,
          name: "timeUnit",
          value: timeUnit,
          onChange: (value: any) => onChange({ name: "timeUnit", value }),
          options: ["$inCurrent"].includes(operator)
            ? CurrentSelection
            : PastOrNextSelection,
        };
        return <Select {...props} />;
      }

      if (["$inPast", "$inNext"].includes(operator)) {
        return (
          <>
            <TextField
              name="value"
              onChange={(value: any) =>
                onChange({ name: "value", value: value })
              }
              value={value}
              {...rest}
            />
            {renderSelect()}
          </>
        );
      }

      if (["$inCurrent"].includes(operator)) {
        return renderSelect();
      }

      return (
        <SimpleWidget
          {...props}
          format={displayFormat}
          value={value ? moment(value, format) : null}
          value2={value2 ? moment(value2, format) : null}
          onChange={({ name, value }: any) => onChange({ name, value })}
          {...{ component: DateTimePicker, type: "date" }}
        />
      );
    case "integer":
    case "long":
    case "decimal":
      return <SimpleWidget {...props} {...{ component: NumberField, type }} />;
    case "enum":
      const options = rest.field.selectionList.map(
        ({ title, value, data }: any) => ({
          name: (data && data.value) || value,
          title: title,
        })
      );
      return <SimpleWidget {...props} {...{ component: Select, options }} />;
    default:
      return <SimpleWidget {...props} {...{ component: TextField }} />;
  }
}
