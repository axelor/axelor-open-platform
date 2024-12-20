import { produce } from "immer";
import Chrome from "@uiw/react-color-chrome";
import Color from "color";
import React, {
  ChangeEvent,
  ChangeEventHandler,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  AdornedInput,
  Input,
  Box,
  ClickAwayListener,
  clsx,
  Popper,
} from "@axelor/ui";
import { ThemeOptions } from "@axelor/ui/core/styles/theme/types";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { deepGet, deepSet } from "@/utils/objects";

import { Select } from "../select";
import {
  ThemeElement,
  ThemeElementProperty,
  elements as ELEMENTS,
} from "./theme-elements";
import { usePropertiesContext } from "./scope";
import { isValidCssValue } from "./utils";
import defaultTheme from "@/hooks/use-app-theme/themes/default.json";
import styles from "./theme-editor.module.scss";

interface ThemeDesignerProps {
  theme: ThemeOptions;
  onChange: (theme: ThemeOptions) => void;
}

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

  const { getCssVar, setInvalids } = usePropertiesContext();
  const value = useMemo(() => deepGet(theme, path) ?? "", [path, theme]);

  const placeholder = useMemo(() => {
    const defaultValue = deepGet(defaultTheme, path);
    if (defaultValue) {
      return defaultValue;
    }
    if (cssVariable) {
      return getCssVar?.(cssVariable);
    }
    return property.placeholder ?? "";
  }, [path, cssVariable, getCssVar, property.placeholder]);

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
        deepSet(draft, path, event.target.value),
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

const DefaultColor = { h: 0, s: 0, v: 0, a: 1 };

function ColorInput({
  invalid,
  value,
  placeholder,
  onChange,
}: {
  invalid?: boolean;
  value?: string;
  placeholder?: string;
  onChange: ChangeEventHandler<HTMLInputElement>;
}) {
  const [target, setTarget] = useState<HTMLElement | null>(null);
  const show = Boolean(target);

  const handleClose = useCallback(() => {
    setTarget(null);
  }, []);

  const color = useMemo(() => {
    try {
      return Color(value).hex();
    } catch {
      return placeholder || DefaultColor;
    }
  }, [placeholder, value]);

  return (
    <>
      <AdornedInput
        type="text"
        placeholder={placeholder}
        invalid={invalid}
        value={value}
        startAdornment={
          <Box
            border
            className={styles.colorInput}
            style={{
              backgroundColor: value || placeholder || "transparent",
            }}
            onClick={(e) => setTarget(e.target as HTMLElement)}
          />
        }
        onChange={onChange}
      />
      {show && (
        <Popper
          open
          shadow
          rounded
          target={target as HTMLElement}
          placement={"bottom-start"}
        >
          <ClickAwayListener onClickAway={handleClose}>
            <Box>
              <Chrome
                inputType={"hexa" as any}
                showAlpha={true}
                showEyeDropper={false}
                showColorPreview={false}
                color={color}
                onChange={(e) =>
                  onChange({
                    target: { value: e.hex },
                  } as ChangeEvent<HTMLInputElement>)
                }
              />
            </Box>
          </ClickAwayListener>
        </Popper>
      )}
    </>
  );
}
