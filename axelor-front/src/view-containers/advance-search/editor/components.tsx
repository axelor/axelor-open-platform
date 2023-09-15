import clsx from "clsx";
import isNumber from "lodash/isNumber";
import React, { useEffect, useMemo, useRef } from "react";

import { Box, Input } from "@axelor/ui";

import { Select as AxSelect, SelectProps } from "@/components/select";
import { useDataStore } from "@/hooks/use-data-store";
import { DataStore } from "@/services/client/data-store";
import { i18n } from "@/services/client/i18n";
import { toKebabCase } from "@/utils/names";
import { DateComponent } from "@/views/form/widgets";

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

function DateField(props: any) {
  const schema = useRef({ type: "date" }).current;
  return <DateComponent schema={schema} trapFocus {...props} />;
}

function NumberField(props: any) {
  return <TextField {...props} type="number" />;
}

export function Select({
  value = null,
  options,
  className,
  ...props
}: Omit<
  SelectProps<SelectType, false>,
  "optionKey" | "optionLabel" | "optionEqual"
>) {
  return (
    <AxSelect
      {...props}
      className={clsx(styles.select, className)}
      multiple={false}
      options={options}
      optionLabel={(x) => x.title}
      optionKey={(x) => x.name}
      optionEqual={(x, y) => x.name === y.name}
      value={value}
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
  const dataStore = useMemo(
    () => new DataStore(field.target, {}),
    [field.target],
  );

  const options = useDataStore(dataStore, (res) => res.records);

  const fetchData = React.useCallback(async () => {
    const res = await dataStore.search({
      filter: {
        _domain: undefined,
        _domainContext: {},
      },
    });
    return res.records;
  }, [dataStore]);

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

    const $value = isMulti
      ? value?.map?.((id: any) =>
          isNumber(id) ? options.find((opt) => opt.id === id) || { id } : id,
        ) ?? null
      : value ?? null;

    const trTargetName = `$t:${targetName}`;

    return (
      <AxSelect
        multiple={isMulti}
        className={clsx(styles.select, className)}
        placeholder={operator === "=" ? rest.placeholder : ""}
        options={options}
        optionKey={(x) => x.id!}
        optionLabel={(x) => x[trTargetName] ?? x[targetName] ?? ""}
        optionEqual={(x, y) => x.id === y.id}
        fetchOptions={fetchData}
        value={$value}
        onChange={(value) => {
          onChange({
            name: "value",
            value: Array.isArray(value)
              ? value.map((x) => ({
                  id: x.id,
                  [targetName]: x[targetName],
                  [trTargetName]: x[trTargetName],
                }))
              : value && {
                  id: value.id,
                  [targetName]: value[targetName],
                  [trTargetName]: value[trTargetName],
                },
          });
        }}
      />
    );
  }
  return null;
}

type SelectType = {
  name: string;
  title: string;
};

const CurrentSelection: SelectType[] = [
  { name: "day", title: i18n.get("Day") },
  { name: "week", title: i18n.get("Week") },
  { name: "month", title: i18n.get("Month") },
  { name: "quarter", title: i18n.get("Quarter") },
  { name: "year", title: i18n.get("Year") },
];

const PastOrNextSelection: SelectType[] = [
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
    case "datetime": {
      const { value, value2, timeUnit, onChange } = props;

      const renderSelect = () => {
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
      };

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
          value={value}
          value2={value2}
          onChange={({ name, value }: any) => onChange({ name, value })}
          {...{ component: DateField }}
        />
      );
    }
    case "integer":
    case "long":
    case "decimal":
      return <SimpleWidget {...props} {...{ component: NumberField, type }} />;
    case "enum": {
      const options = (rest.field.selectionList ?? []).map(
        ({ title, value, data }: any) => ({
          name: (data && data.value) || value,
          title: title,
        }),
      );
      return <SimpleWidget {...props} {...{ component: Select, options }} />;
    }
    default:
      return <SimpleWidget {...props} {...{ component: TextField }} />;
  }
}
