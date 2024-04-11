import React, {
  ChangeEventHandler,
  ComponentProps,
  JSXElementConstructor,
  useCallback,
  useMemo,
  useRef,
  useState,
} from "react";

import { clsx, Box, Input, SelectIcon, SelectValue } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Select as AxSelect, SelectProps } from "@/components/select";
import { i18n } from "@/services/client/i18n";
import { toKebabCase } from "@/utils/names";
import { DateComponent } from "@/views/form/widgets";
import { useCompletion, useSelector } from "@/hooks/use-relation";
import { Field, Property, Schema } from "@/services/client/meta.types";
import { DataRecord, Filter, FilterOp } from "@/services/client/data.types";
import { useOptionLabel } from "@/views/form/widgets/many-to-one/utils";
import { getFieldType } from "./utils";

import styles from "./components.module.css";

function TextField(
  props: ComponentProps<typeof Input> & {
    onChange: (value: any) => void;
  },
) {
  return (
    <Input
      {...props}
      value={props.value ?? ""}
      onChange={(e) => props.onChange?.(e.target.value)}
    />
  );
}

function DateField(props: ComponentProps<typeof DateComponent>) {
  const schema = useRef({ type: "date" }).current;
  return <DateComponent trapFocus {...props} schema={schema} />;
}

function NumberField(props: ComponentProps<typeof TextField>) {
  return <TextField {...props} type="number" />;
}

function Select({
  value: _value = null,
  options,
  className,
  onChange,
  ...props
}: SelectProps<DataRecord, false>) {
  const value = useMemo(
    () => options?.find((opt) => opt.name === _value) ?? _value,
    [_value, options],
  );

  const handleChange = useCallback(
    (value: SelectValue<DataRecord, false>) => onChange?.(value?.name ?? value),
    [onChange],
  );

  return (
    <AxSelect<DataRecord, false>
      {...props}
      className={clsx(styles.select, className)}
      multiple={false}
      options={options}
      optionLabel={(x) => x.title}
      optionKey={(x) => x.name}
      optionEqual={(x, y) => x.name === y.name}
      value={value}
      onChange={handleChange}
    />
  );
}

type WidgetInputProps = {
  className?: string;
  placeholder?: string;
  options?: any[];
};

type WidgetProps = Pick<Filter, "value" | "value2" | "timeUnit"> & {
  operator: FilterOp;
  field: Field | Property;
  onChange: (e: { name: string; value: any }) => void;
  component?: JSXElementConstructor<any>;
  inputProps?: WidgetInputProps;
};

function SimpleWidget({
  component: Component,
  operator,
  onChange,
  value,
  value2,
  inputProps,
}: WidgetProps) {
  if (!Component) return;

  if (["=", "!=", ">", ">=", "<", "<=", "like", "notLike"].includes(operator)) {
    return (
      <Component
        name="value"
        onChange={(value: any) => onChange({ name: "value", value: value })}
        value={value}
        {...inputProps}
      />
    );
  }

  if (["between", "notBetween"].includes(operator)) {
    return (
      <>
        <Component
          name="value"
          onChange={(value: any) => onChange({ name: "value", value })}
          value={value}
          {...inputProps}
        />

        <Component
          name="value2"
          onChange={(value: any) => onChange({ name: "value2", value })}
          value={value2}
          {...inputProps}
        />
      </>
    );
  }

  return null;
}

const getOptionKey = (option: DataRecord) => option.id!;
const getOptionEqual = (a: DataRecord, b: DataRecord) => a.id === b.id;
const getOptionMatch = () => true;

type RelationWidgetProps = WidgetProps & {
  multiple?: boolean;
};

function RelationalSelectWidget({
  operator,
  multiple = operator !== "=",
  field: schema,
  value,
  onChange,
  inputProps,
}: RelationWidgetProps) {
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
  } = schema as Schema;
  const { className, placeholder } = inputProps || {};

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
      multiple,
      limit: searchLimit,
      onSelect: async (records) => {
        onChange({ name: "value", value: multiple ? records : records[0] });
      },
      ...(domain && {
        domain,
        context: {},
      }),
    });
  }, [
    showSelector,
    multiple,
    target,
    gridView,
    sortBy,
    searchLimit,
    domain,
    onChange,
  ]);

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
      multiple={multiple}
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

export function RelationalWidget(props: RelationWidgetProps) {
  const { operator, onChange, value, inputProps } = props;

  const isTextField = ["like", "notLike"].includes(operator);
  const isSelection = ["=", "in", "notIn"].includes(operator);

  if (isTextField) {
    return (
      <TextField
        name="value"
        onChange={(value) => onChange({ name: "value", value: value })}
        value={value}
        {...inputProps}
      />
    );
  } else if (isSelection) {
    return <RelationalSelectWidget {...props} />;
  }
  return null;
}

export function BooleanRadio({
  name,
  onChange,
  value: valueProp,
  options,
}: {
  name: string;
  options: {
    label: string;
    value: any;
  }[];
  value: any;
  onChange: ChangeEventHandler<HTMLInputElement>;
}) {
  return (
    <Box d="flex" alignItems="center" ms={1} me={1}>
      {options.map(({ value, label }, index: number) => (
        <Box as="label" d="flex" alignItems="center" key={index} me={2}>
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

type SelectType = {
  title: string;
  name: string;
  value?: any;
};

type BooleanSelectType = {
  title: string;
  value: boolean;
};

let currentSelection: SelectType[];
let pastOrNextSelection: SelectType[];
let booleanSelection: BooleanSelectType[];

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

const getBooleanSelection: () => BooleanSelectType[] = () =>
  booleanSelection ||
  (booleanSelection = [
    { value: true, title: i18n.get("Yes") },
    { value: false, title: i18n.get("No") },
  ]);

export function Widget({
  inputProps,
  field,
  operator,
  onChange,
  filter,
}: Omit<WidgetProps, "value" | "value2" | "timeUnit"> & {
  filter: Filter;
}) {
  const type = getFieldType(field);
  const props = {
    inputProps,
    field,
    operator,
    value: filter.value,
    value2: filter.value2,
    timeUnit: filter.timeUnit,
    onChange,
  };

  switch (toKebabCase(type)) {
    case "one-to-one":
    case "many-to-one":
    case "many-to-many":
    case "one-to-many":
      return <RelationalWidget {...props} />;
    case "boolean": {
      const selectionList = getBooleanSelection();
      const { value } = props;
      return (
        operator === "=" && (
          <AxSelect
            optionKey={(x) => x.value as any}
            optionLabel={(x) => x.title}
            optionEqual={(x, y) => x.value === y.value}
            className={styles.select}
            value={selectionList.find((item) => item.value === value) ?? null}
            onChange={(value: any) =>
              onChange({ name: "value", value: value?.value })
            }
            options={selectionList}
          />
        )
      );
    }
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
              {...inputProps}
              name="value"
              onChange={(value) => onChange({ name: "value", value: value })}
              value={value}
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
          onChange={onChange}
          component={DateField}
        />
      );
    }
    case "integer":
    case "long":
    case "decimal":
      return <SimpleWidget {...props} component={NumberField} />;
    case "enum": {
      const options = (field.selectionList ?? []).map(
        ({ title, value, data }) => ({
          name: (data && data.value) || value,
          title: title,
        }),
      );
      return (
        <SimpleWidget
          {...props}
          component={Select}
          inputProps={{ ...props.inputProps, options }}
        />
      );
    }
    default:
      return <SimpleWidget {...props} component={TextField} />;
  }
}
