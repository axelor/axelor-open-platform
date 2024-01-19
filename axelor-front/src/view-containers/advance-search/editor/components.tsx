import clsx from "clsx";
import React, { useCallback, useMemo, useRef, useState } from "react";

import { Box, Input, SelectIcon } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Select as AxSelect, SelectProps } from "@/components/select";
import { i18n } from "@/services/client/i18n";
import { toKebabCase } from "@/utils/names";
import { DateComponent } from "@/views/form/widgets";
import { useCompletion, useSelector } from "@/hooks/use-relation";
import { DataRecord } from "@/services/client/data.types";
import { useOptionLabel } from "@/views/form/widgets/many-to-one/utils";

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

function Select({
  value = null,
  options,
  className,
  ...props
}: SelectProps<DataRecord, false>) {
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

const getOptionKey = (option: DataRecord) => option.id!;
const getOptionEqual = (a: DataRecord, b: DataRecord) => a.id === b.id;
const getOptionMatch = () => true;

export function RelationalSelectWidget({
  operator,
  onChange,
  isMulti = operator !== "=",
  field: schema,
  value,
  placeholder,
  className,
}: any) {
  const [hasSearchMore, setSearchMore] = useState(false);
  const {
    sortBy,
    limit,
    target,
    targetName,
    targetSearch,
    gridView,
    searchLimit,
    domain,
  } = schema;
  const showSelector = useSelector();

  const search = useCompletion({
    sortBy,
    limit,
    target,
    targetName,
    targetSearch,
  });

  const getOptionLabel = useOptionLabel(schema);

  const fetchOptions = useCallback(
    async (text: string) => {
      const { records, page } = await search(text, {
        ...(domain && {
          _domain: domain,
          _domainContext: {},
        }),
      });
      setSearchMore((page.totalCount ?? 0) > records.length);
      return records;
    },
    [search, domain],
  );

  const showSelect = useCallback(() => {
    showSelector({
      model: target,
      viewName: gridView,
      orderBy: sortBy,
      multiple: false,
      limit: searchLimit,
      onSelect: async (records) => {
        onChange(records[0]);
      },
      ...(domain && {
        domain,
        context: {},
      }),
    });
  }, [showSelector, target, gridView, sortBy, searchLimit, domain, onChange]);

  const icons = useMemo(
    () =>
      [
        {
          icon: <MaterialIcon icon="search" />,
          onClick: showSelect,
        },
      ] as SelectIcon[],
    [showSelect],
  );

  return (
    <AxSelect
      className={clsx(styles.select, className)}
      multiple={isMulti}
      options={[] as DataRecord[]}
      toggleIcon={false}
      icons={icons}
      placeholder={operator === "=" ? placeholder : ""}
      optionKey={getOptionKey}
      optionLabel={getOptionLabel}
      optionEqual={getOptionEqual}
      optionMatch={getOptionMatch}
      fetchOptions={fetchOptions}
      value={value}
      onChange={(value) => onChange({ name: "value", value })}
      {...(hasSearchMore && {
        onShowSelect: showSelect,
      })}
    />
  );
}

export function RelationalWidget(props: any) {
  const { operator, onChange, ...rest } = props;
  const { field, value } = rest;

  const isTextField = ["like", "notLike"].includes(operator);
  const isSelection = ["=", "in", "notIn"].includes(operator);

  if (isTextField) {
    return (
      <TextField
        name="value"
        onChange={(value: any) => onChange({ name: "value", value: value })}
        {...rest}
      />
    );
  } else if (isSelection) {
    return <RelationalSelectWidget {...props} />;
  }
  return null;
}

type SelectType = {
  name: string;
  title: string;
};

let currentSelection: SelectType[];
let pastOrNextSelection: SelectType[];

const getCurrentSelection: () => SelectType[] = () =>
  currentSelection ||
  (currentSelection = [
    { name: "day", title: i18n.get("Day") },
    { name: "week", title: i18n.get("Week") },
    { name: "month", title: i18n.get("Month") },
    { name: "quarter", title: i18n.get("Quarter") },
    { name: "year", title: i18n.get("Year") },
  ]);

const getPastOrNextSelection: () => SelectType[] = () =>
  pastOrNextSelection ||
  (pastOrNextSelection = [
    { name: "day", title: i18n.get("Days") },
    { name: "week", title: i18n.get("Weeks") },
    { name: "month", title: i18n.get("Months") },
    { name: "quarter", title: i18n.get("Quarters") },
    { name: "year", title: i18n.get("Years") },
  ]);

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
        const props: any = {
          isClearOnDelete: false,
          name: "timeUnit",
          value: timeUnit,
          onChange: (value: any) => onChange({ name: "timeUnit", value }),
          options: ["$inCurrent"].includes(operator)
            ? getCurrentSelection()
            : getPastOrNextSelection(),
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
