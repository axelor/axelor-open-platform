import merge from "lodash/merge";
import cloneDeep from "lodash/cloneDeep";
import { produce } from "immer";
import React, {
  ChangeEvent,
  ChangeEventHandler,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import { AdornedInput, Input, Box, clsx } from "@axelor/ui";
import { ThemeOptions } from "@axelor/ui/core/styles/theme/types";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import defaultTheme from "@/hooks/use-app-theme/themes/default.json";
import darkTheme from "@/hooks/use-app-theme/themes/dark.json";
import {
  ColorPicker,
  colorToHex,
  useColorPicker,
} from "@/components/color-picker";
import { deepGet, deepSet } from "@/utils/objects";
import { ColorResult } from "@uiw/color-convert";

import { Select } from "../select";
import {
  ThemeElement,
  ThemeElementProperty,
  elements as ELEMENTS,
} from "./theme-elements";
import { usePropertiesContext } from "./scope";
import { isValidCssValue } from "./utils";

import styles from "./theme-editor.module.scss";

interface ThemeDesignerProps {
  theme: ThemeOptions;
  onChange: (theme: ThemeOptions) => void;
}

const cssPropertyExamples: Record<string, string> = {
  padding: "ex: 4px 8px, 0.25rem 0.5rem",
  margin: "ex: 4px 8px, 0.25rem 0.5rem",
  gap: "ex: 16px, 1rem",
  height: "ex: 100px, 100%",
  width: "ex: 100px, 100%",
  "z-index": "ex: 1, 2, 3",
  "font-size": "ex: 16px, 1rem",
  "font-weight": "ex: 400, 500, bold, bolder",
  "box-shadow": "ex: 0 0.5rem 1rem rgba(0, 0, 0, 0.15)",
  "row-gap": "ex: 1rem, 16px",
  "column-gap": "ex: 1rem, 16px",
  border: "ex: 1px solid #eee",
  "border-width": "ex: 1px, 0.625rem",
  "border-style": "ex: solid, dashed, dotted",
  "border-radius": "ex: 6px, 0.375rem, 15%",
};

export function ThemeDesigner(props: ThemeDesignerProps) {
  const [selected, setSelected] = useState<ThemeElement>(ELEMENTS[0]);
  const onItemClick = useCallback((node: ThemeElement) => {
    setSelected(node);
  }, []);

  return (
    <div className={styles.designer}>
      <div className={styles.side}>
        <ThemeMenu
          elements={ELEMENTS}
          selected={selected}
          onSelect={onItemClick}
        />
      </div>
      <div className={styles.main}>
        {selected && <ElementEditor element={selected} {...props} />}
      </div>
    </div>
  );
}

type ThemeMenuProps = {
  elements: ThemeElement[];
  selected: ThemeElement;
  onSelect: (node: ThemeElement) => void;
};

function ThemeMenuItem({
  item,
  active,
  onSelect,
}: {
  item: ThemeElement;
  active?: boolean;
  onSelect: ThemeMenuProps["onSelect"];
}) {
  const { invalids: invalidProperties } = usePropertiesContext();

  const invalid = useMemo(
    () =>
      item.editors?.some((editor) =>
        editor.props.some((prop) => invalidProperties[prop.path]),
      ),
    [invalidProperties, item],
  );

  return (
    <div
      className={clsx(styles.item, {
        [styles.active]: active,
      })}
      onClick={() => onSelect(item)}
    >
      <MaterialIcon icon={item.icon ?? "widgets"} />
      <div className={styles.title}>{item.title}</div>
      {invalid && <MaterialIcon icon={"error"} color="danger" />}
    </div>
  );
}

function ThemeMenu(props: ThemeMenuProps) {
  const { elements, selected, onSelect } = props;
  return (
    <div className={styles.menu}>
      {elements.map((item) => (
        <ThemeMenuItem
          key={item.name}
          item={item}
          active={item === selected}
          onSelect={onSelect}
        />
      ))}
    </div>
  );
}

function ElementEditor(
  props: ThemeDesignerProps & {
    element: ThemeElement;
  },
) {
  const { element, ...rest } = props;
  const { editors = [] } = element;

  return (
    <div className={styles.palette}>
      {editors.map((editor) => (
        <div key={editor.name} className={styles.panel}>
          <div className={styles.header}>
            <div className={styles.title}>{editor.name}</div>
          </div>
          <div className={styles.body}>
            {editor.props.map((item) => (
              <PropertyEditor key={item.name} property={item} {...rest} />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

function PropertyEditor(
  props: ThemeDesignerProps & {
    property: ThemeElementProperty;
  },
) {
  const { property, theme, onChange } = props;
  const {
    name,
    path,
    type,
    isValid: isPropertyValid,
    cssProperty = type,
    cssVariable,
  } = property;

  const { getCssVar, readonly, setInvalids } = usePropertiesContext();
  const value = useMemo(() => deepGet(theme, path) ?? "", [path, theme]);
  const baseTheme = useMemo(() => {
    return theme?.palette?.mode === "dark"
      ? merge(cloneDeep(defaultTheme), darkTheme)
      : defaultTheme;
  }, [theme?.palette?.mode]);

  const placeholder = useMemo(() => {
    const defaultValue = deepGet(baseTheme, path);
    if (defaultValue) {
      return defaultValue;
    }
    if (cssVariable) {
      return getCssVar?.(cssVariable);
    }
    if (readonly) {
      return "";
    }
    return property.placeholder ?? cssPropertyExamples[cssProperty ?? ""] ?? "";
  }, [
    baseTheme,
    path,
    cssVariable,
    readonly,
    property.placeholder,
    cssProperty,
    getCssVar,
  ]);

  const invalid = useMemo(
    () =>
      isPropertyValid
        ? !isPropertyValid(value)
        : !isValidCssValue(cssProperty, value),
    [isPropertyValid, cssProperty, value],
  );

  const handleChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const newTheme = produce(theme, (draft) =>
        deepSet(draft, path, event.target.value || undefined),
      );
      onChange(newTheme);
    },
    [onChange, path, theme],
  );

  useEffect(() => {
    setInvalids(
      produce((draft) => {
        if (invalid) {
          draft[path] = invalid;
        } else {
          delete draft[path];
        }
      }),
    );
  }, [path, invalid, setInvalids]);

  function render() {
    switch (type) {
      case "color":
        return (
          <ColorInput
            value={value}
            readonly={readonly}
            placeholder={placeholder}
            invalid={invalid}
            onChange={handleChange}
          />
        );
      case "select": {
        const { options = [] } = property;
        const selected = options.find((x) => x.value === value) ?? null;
        return (
          <Select
            readOnly={readonly}
            className={styles.select}
            autoComplete={false}
            placeholder={placeholder}
            onChange={(e: any) =>
              handleChange({
                target: {
                  value: e?.value,
                },
              } as ChangeEvent<HTMLInputElement>)
            }
            value={selected}
            options={options}
            optionKey={(x) => x.value}
            optionLabel={(x) => x.title}
            optionEqual={(x, y) => x.value === y.value}
          />
        );
      }
      default:
        return (
          <Input
            type="text"
            disabled={readonly}
            placeholder={placeholder}
            invalid={invalid}
            value={value}
            onChange={handleChange}
          />
        );
    }
  }

  return (
    <div className={styles.property}>
      <div className={styles.title}>{name}</div>
      <div className={styles.value}>{render()}</div>
    </div>
  );
}

function ColorInput({
  invalid,
  readonly,
  value,
  placeholder,
  onChange,
}: {
  invalid?: boolean;
  readonly?: boolean;
  value?: string;
  placeholder?: string;
  onChange: ChangeEventHandler<HTMLInputElement>;
}) {
  const { open, pickerPopoverProps } = useColorPicker();

  const color = useMemo(() => {
    return colorToHex(value) ?? colorToHex(placeholder) ?? null;
  }, [placeholder, value]);

  const handleChange = useCallback(
    (result: ColorResult) => {
      onChange({
        target: { value: result.hsva?.a < 1 ? result.hexa : result.hex },
      } as ChangeEvent<HTMLInputElement>);
    },
    [onChange],
  );

  return (
    <>
      <AdornedInput
        disabled={readonly}
        type="text"
        placeholder={placeholder}
        invalid={invalid}
        value={value}
        className={clsx(styles.colorInput, {
          [styles.readonly]: readonly,
        })}
        startAdornment={
          <Box
            border
            className={clsx(styles.colorInputIcon)}
            style={{
              backgroundColor: value || placeholder || "transparent",
            }}
            onClick={(e) => !readonly && open(e.currentTarget)}
          />
        }
        onChange={onChange}
      />
      <ColorPicker
        {...pickerPopoverProps}
        showAlpha={true}
        value={color}
        onChange={handleChange}
      />
    </>
  );
}
