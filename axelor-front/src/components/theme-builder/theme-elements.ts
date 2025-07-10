import { MaterialIconProps } from "@axelor/ui/icons/material-icon";

export type ThemeElementPropertyType = "color" | "text" | "select";

export type ThemeElementProperty = {
  name: string;
  path: string;
  type?: ThemeElementPropertyType;
  options?: { title: string; value: string }[]; // for selection
  placeholder?: string;
  cssProperty?: string;
  cssVariable?: string;
  isValid?: (value: any) => boolean;
};

export type ThemeElementEditor = {
  name: string;
  props: ThemeElementProperty[];
};

export type ThemeElement = {
  name: string;
  title: string;
  icon?: MaterialIconProps["icon"];
  editors?: ThemeElementEditor[];
};

export const elements: ThemeElement[] = [
  {
    name: "palette",
    title: "Palette",
    icon: "palette",
    editors: [
      {
        name: "Theme",
        props: [
          {
            name: "Mode",
            path: "palette.mode",
            type: "select",
            placeholder: "Light/Dark",
            options: [
              { title: "Light", value: "light" },
              { title: "Dark", value: "dark" },
            ],
            isValid: () => true,
          },
        ],
      },
      {
        name: "Theme Colors",
        props: [
          {
            name: "Primary",
            path: "palette.primary",
            type: "color",
            cssVariable: "--bs-primary",
          },
          {
            name: "Secondary",
            path: "palette.secondary",
            type: "color",
            cssVariable: "--bs-secondary",
          },
          {
            name: "Success",
            path: "palette.success",
            type: "color",
            cssVariable: "--bs-success",
          },
          {
            name: "Warning",
            path: "palette.warning",
            type: "color",
            cssVariable: "--bs-warning",
          },
          {
            name: "Danger",
            path: "palette.danger",
            type: "color",
            cssVariable: "--bs-danger",
          },
          {
            name: "Info",
            path: "palette.info",
            type: "color",
            cssVariable: "--bs-info",
          },
          {
            name: "Light",
            path: "palette.light",
            type: "color",
            cssVariable: "--bs-light",
          },
          {
            name: "Dark",
            path: "palette.dark",
            type: "color",
            cssVariable: "--bs-dark",
          },
        ],
      },
      {
        name: "Body Colors",
        props: [
          {
            name: "Color",
            path: "palette.body_color",
            type: "color",
            cssVariable: "--bs-body-color",
          },
          {
            name: "Background",
            path: "palette.body_bg",
            type: "color",
            cssVariable: "--bs-body-bg",
          },
          {
            name: "Secondary color",
            path: "palette.secondary_color",
            type: "color",
            cssVariable: "--bs-secondary-color",
          },
          {
            name: "Secondary background",
            path: "palette.secondary_bg",
            type: "color",
            cssVariable: "--bs-secondary-bg",
          },
          {
            name: "Tertiary color",
            path: "palette.tertiary_color",
            type: "color",
            cssVariable: "--bs-tertiary-color",
          },
          {
            name: "Tertiary background",
            path: "palette.tertiary_bg",
            type: "color",
            cssVariable: "--bs-tertiary-bg",
          },
          {
            name: "Emphasis color",
            path: "palette.emphasis_color",
            type: "color",
            cssVariable: "--bs-emphasis-color",
          },
        ],
      },
      {
        name: "Web Colors",
        props: [
          {
            name: "Blue",
            path: "palette.blue",
            type: "color",
            cssVariable: "--bs-blue",
          },
          {
            name: "Indigo",
            path: "palette.indigo",
            type: "color",
            cssVariable: "--bs-indigo",
          },
          {
            name: "Purple",
            path: "palette.purple",
            type: "color",
            cssVariable: "--bs-purple",
          },
          {
            name: "Pink",
            path: "palette.pink",
            type: "color",
            cssVariable: "--bs-pink",
          },
          {
            name: "Red",
            path: "palette.red",
            type: "color",
            cssVariable: "--bs-red",
          },
          {
            name: "Orange",
            path: "palette.orange",
            type: "color",
            cssVariable: "--bs-orange",
          },
          {
            name: "Yellow",
            path: "palette.yellow",
            type: "color",
            cssVariable: "--bs-yellow",
          },
          {
            name: "Green",
            path: "palette.green",
            type: "color",
            cssVariable: "--bs-green",
          },
          {
            name: "Teal",
            path: "palette.teal",
            type: "color",
            cssVariable: "--bs-teal",
          },
          {
            name: "Cyan",
            path: "palette.cyan",
            type: "color",
            cssVariable: "--bs-cyan",
          },
        ],
      },
      {
        name: "Gray Colors",
        props: [
          {
            name: "White",
            path: "palette.white",
            type: "color",
            cssVariable: "--bs-white",
          },
          {
            name: "Black",
            path: "palette.black",
            type: "color",
            cssVariable: "--bs-black",
          },
          {
            name: "Gray",
            path: "palette.gray",
            type: "color",
            cssVariable: "--bs-gray",
          },
          {
            name: "Gray dark",
            path: "palette.gray_dark",
            type: "color",
            cssVariable: "--bs-gray-dark",
          },
          {
            name: "Gray 100",
            path: "palette.gray_100",
            type: "color",
            cssVariable: "--bs-gray-100",
          },
          {
            name: "Gray 200",
            path: "palette.gray_200",
            type: "color",
            cssVariable: "--bs-gray-200",
          },
          {
            name: "Gray 300",
            path: "palette.gray_300",
            type: "color",
            cssVariable: "--bs-gray-300",
          },
          {
            name: "Gray 400",
            path: "palette.gray_400",
            type: "color",
            cssVariable: "--bs-gray-400",
          },
          {
            name: "Gray 500",
            path: "palette.gray_500",
            type: "color",
            cssVariable: "--bs-gray-500",
          },
          {
            name: "Gray 600",
            path: "palette.gray_600",
            type: "color",
            cssVariable: "--bs-gray-600",
          },
          {
            name: "Gray 700",
            path: "palette.gray_700",
            type: "color",
            cssVariable: "--bs-gray-700",
          },
          {
            name: "Gray 800",
            path: "palette.gray_800",
            type: "color",
            cssVariable: "--bs-gray-800",
          },
          {
            name: "Gray 900",
            path: "palette.gray_900",
            type: "color",
            cssVariable: "--bs-gray-900",
          },
        ],
      },
    ],
  },
  {
    name: "typography",
    title: "Typography",
    icon: "custom_typography",
    editors: [
      {
        name: "Typography",
        props: [
          {
            name: "Font family",
            path: "typography.fontFamily",
            cssProperty: "font-family",
            cssVariable: "--bs-body-font-family",
          },
          {
            name: "Font size",
            path: "typography.fontSize",
            cssProperty: "font-size",
            cssVariable: "--bs-body-font-size",
          },
          {
            name: "Font weight",
            path: "typography.fontWeight",
            cssProperty: "font-weight",
            cssVariable: "--bs-body-font-weight",
          },
          {
            name: "Line height",
            path: "typography.lineHeight",
            cssProperty: "line-height",
            cssVariable: "--bs-body-line-height",
          },
        ],
      },
    ],
  },
  {
    name: "border",
    title: "Border",
    icon: "border_style",
    editors: [
      {
        name: "Border",
        props: [
          {
            name: "Width",
            path: "border.width",
            cssProperty: "border-width",
            cssVariable: "--bs-border-width",
          },
          {
            name: "Color",
            path: "border.color",
            type: "color",
            cssVariable: "--bs-border-color",
          },
          {
            name: "Style",
            path: "border.style",
            cssProperty: "border-style",
            cssVariable: "--bs-border-style",
          },
          {
            name: "Radius",
            path: "border.radius",
            cssProperty: "border-radius",
          },
        ],
      },
    ],
  },
  {
    name: "link",
    title: "Link",
    icon: "link",
    editors: [
      {
        name: "Link",
        props: [
          {
            name: "Color",
            path: "link.color",
            type: "color",
            cssVariable: "--bs-link-color",
          },
          {
            name: "Hover",
            path: "link.hover",
            type: "color",
            cssVariable: "--bs-link-hover-color",
          },
          {
            name: "Decoration",
            path: "link.decoration",
            cssProperty: "text-decoration",
            cssVariable: "--bs-link-decoration",
          },
        ],
      },
    ],
  },
  {
    name: "shell",
    title: "Shell",
    icon: "code_blocks",
    editors: [
      {
        name: "Shell",
        props: [
          {
            name: "Color",
            path: "components.Shell.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.Shell.bg",
            type: "color",
          },
          {
            name: "Scrollbar",
            path: "components.Shell.scrollbar.color",
            type: "color",
          },
        ],
      },
      {
        name: "View toolbar",
        props: [
          {
            name: "Color",
            path: "components.Shell.view.toolbar.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.Shell.view.toolbar.bg",
            type: "color",
          },
          {
            name: "Gap",
            path: "components.Shell.view.toolbar.gap",
            cssProperty: "gap",
          },
          {
            name: "Padding",
            path: "components.Shell.view.toolbar.padding",
            cssProperty: "padding",
          },
          {
            name: "Border width",
            path: "components.Shell.view.toolbar.border.width",
            cssProperty: "border-width",
          },
          {
            name: "Border color",
            path: "components.Shell.view.toolbar.border.color",
            type: "color",
          },
          {
            name: "Border style",
            path: "components.Shell.view.toolbar.border.style",
            cssProperty: "border-style",
          },
        ],
      },
      {
        name: "View content",
        props: [
          {
            name: "Color",
            path: "components.Shell.view.content.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.Shell.view.content.bg",
            type: "color",
          },
          {
            name: "Padding",
            path: "components.Shell.view.content.padding",
            cssProperty: "padding",
          },
          {
            name: "Border width",
            path: "components.Shell.view.content.border.width",
            cssProperty: "border-width",
          },
          {
            name: "Border color",
            path: "components.Shell.view.content.border.color",
            type: "color",
          },
          {
            name: "Border style",
            path: "components.Shell.view.content.border.style",
            cssProperty: "border-style",
          },
        ],
      },
    ],
  },
  {
    name: "Badge",
    title: "Badge",
    icon: "badge",
    editors: [
      {
        name: "Badge",
        props: [
          {
            name: "Opacity",
            path: "components.Badge.opacity",
            cssProperty: "opacity",
          },
          {
            name: "Primary",
            path: "components.Badge.primary.color",
            type: "color",
          },
          {
            name: "Secondary",
            path: "components.Badge.secondary.color",
            type: "color",
          },
          {
            name: "Success",
            path: "components.Badge.success.color",
            type: "color",
          },
          {
            name: "Danger",
            path: "components.Badge.danger.color",
            type: "color",
          },
          {
            name: "Warning",
            path: "components.Badge.warning.color",
            type: "color",
          },
          {
            name: "Info",
            path: "components.Badge.info.color",
            type: "color",
          },
          {
            name: "Light",
            path: "components.Badge.light.color",
            type: "color",
          },
          {
            name: "Dark",
            path: "components.Badge.dark.color",
            type: "color",
          },
        ],
      },
    ],
  },
  {
    name: "Button",
    title: "Button",
    icon: "buttons_alt",
    editors: [
      {
        name: "Button",
        props: [
          {
            name: "Padding",
            path: "components.Button.padding",
            cssProperty: "padding",
          },
          {
            name: "PaddingX",
            path: "components.Button.paddingX",
            cssProperty: "padding",
          },
          {
            name: "PaddingY",
            path: "components.Button.paddingY",
            cssProperty: "padding",
          },
          {
            name: "Gap",
            path: "components.Button.gap",
            cssProperty: "gap",
          },
        ],
      },
    ],
  },
  {
    name: "Input",
    title: "Input",
    icon: "input",
    editors: [
      {
        name: "Input",
        props: [
          {
            name: "Padding",
            path: "components.Input.padding",
            cssProperty: "padding",
          },
          {
            name: "Placeholder",
            path: "components.Input.placeholder.color",
            type: "color",
          },
          {
            name: "Border width",
            path: "components.Input.border_width",
            cssProperty: "border-width",
          },
          {
            name: "Border width (focus)",
            path: "components.Input.focus.border_width",
            cssProperty: "border-width",
          },
          {
            name: "Border width (invalid)",
            path: "components.Input.invalid.border_width",
            cssProperty: "border-width",
          },
          {
            name: "Border width (invalid & focus)",
            path: "components.Input.invalid_focus.border_width",
            cssProperty: "border-width",
          },
        ],
      },
      {
        name: "Border",
        props: [
          {
            name: "Border width",
            path: "components.Input.border.width",
            cssProperty: "border-width",
          },
          {
            name: "Border style",
            path: "components.Input.border.style",
            cssProperty: "border-style",
          },
          {
            name: "Border color",
            path: "components.Input.border.color",
            type: "color",
          },
        ],
      },
      {
        name: "Radius",
        props: [
          {
            name: "Default",
            path: "components.Input.border.radius",
            cssProperty: "border-radius",
          },
          {
            name: "Small",
            path: "components.Input.border_sm.radius",
            cssProperty: "border-radius",
          },
          {
            name: "Large",
            path: "components.Input.border_lg.radius",
            cssProperty: "border-radius",
          },
        ],
      },
      {
        name: "Focus",
        props: [
          {
            name: "Border width",
            path: "components.Input.focus.border.width",
            cssProperty: "border-width",
          },
          {
            name: "Border style",
            path: "components.Input.focus.border.style",
            cssProperty: "border-style",
          },
          {
            name: "Border color",
            path: "components.Input.focus.border.color",
            type: "color",
          },
          {
            name: "Shadow",
            path: "components.Input.focus.shadow",
            cssProperty: "box-shadow",
          },
        ],
      },
      {
        name: "Invalid",
        props: [
          {
            name: "Border width",
            path: "components.Input.invalid.border.width",
            cssProperty: "border-width",
          },
          {
            name: "Border style",
            path: "components.Input.invalid.border.style",
            cssProperty: "border-style",
          },
          {
            name: "Border color",
            path: "components.Input.invalid.border.color",
            type: "color",
          },
          {
            name: "Shadow",
            path: "components.Input.invalid.shadow",
            cssProperty: "box-shadow",
          },
        ],
      },
      {
        name: "Invalid focus",
        props: [
          {
            name: "Border width",
            path: "components.Input.invalid_focus.border.width",
            cssProperty: "border-width",
          },
          {
            name: "Border style",
            path: "components.Input.invalid_focus.border.style",
            cssProperty: "border-style",
          },
          {
            name: "Border color",
            path: "components.Input.invalid_focus.border.color",
            type: "color",
          },
          {
            name: "Shadow",
            path: "components.Input.invalid_focus.shadow",
            cssProperty: "box-shadow",
          },
        ],
      },
    ],
  },
  {
    name: "Form",
    title: "Form",
    icon: "article",
    editors: [
      {
        name: "Form",
        props: [
          {
            name: "Padding",
            path: "components.Form.padding",
            cssProperty: "padding",
          },
          {
            name: "Gap",
            path: "components.Form.gap",
            cssProperty: "gap",
          },
          {
            name: "Row gap",
            path: "components.Form.rowGap",
            cssProperty: "row-gap",
          },
          {
            name: "Column gap",
            path: "components.Form.columnGap",
            cssProperty: "column-gap",
          },
        ],
      },
    ],
  },
  {
    name: "Panel",
    title: "Panel",
    icon: "shelf_position",
    editors: [
      {
        name: "Panel",
        props: [
          {
            name: "Color",
            path: "components.Panel.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.Panel.bg",
            type: "color",
          },
          {
            name: "Padding",
            path: "components.Panel.body.padding",
            cssProperty: "padding",
          },
          {
            name: "Shadow",
            path: "components.Panel.shadow",
            cssProperty: "box-shadow",
          },
          {
            name: "Border width",
            path: "components.Panel.border.width",
            cssProperty: "border-width",
          },
          {
            name: "Border style",
            path: "components.Panel.border.style",
            cssProperty: "border-style",
          },
          {
            name: "Border color",
            path: "components.Panel.border.color",
            type: "color",
          },
        ],
      },
      {
        name: "Header",
        props: [
          {
            name: "Color",
            path: "components.Panel.header.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.Panel.header.bg",
            type: "color",
          },
          {
            name: "Padding",
            path: "components.Panel.header.padding",
            cssProperty: "padding",
          },
          {
            name: "Title padding",
            path: "components.Panel.title.padding",
            cssProperty: "padding",
          },
          {
            name: "Gap",
            path: "components.Panel.header.gap",
            cssProperty: "gap",
          },
          {
            name: "Font size",
            path: "components.Panel.header.fontSize",
            cssProperty: "font-size",
          },
          {
            name: "Font weight",
            path: "components.Panel.header.fontWeight",
            cssProperty: "font-weight",
          },
          {
            name: "Border width",
            path: "components.Panel.header.border.width",
            cssProperty: "border-width",
          },
          {
            name: "Border style",
            path: "components.Panel.header.border.style",
            cssProperty: "border-style",
          },
          {
            name: "Border color",
            path: "components.Panel.header.border.color",
            type: "color",
          },
        ],
      },
      {
        name: "Footer",
        props: [
          {
            name: "Color",
            path: "components.Panel.footer.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.Panel.footer.bg",
            type: "color",
          },
          {
            name: "Padding",
            path: "components.Panel.footer.padding",
            cssProperty: "padding",
          },
          {
            name: "Gap",
            path: "components.Panel.footer.gap",
            cssProperty: "gap",
          },
          {
            name: "Font size",
            path: "components.Panel.footer.fontSize",
            cssProperty: "font-size",
          },
          {
            name: "Font weight",
            path: "components.Panel.footer.fontWeight",
            cssProperty: "font-weight",
          },
          {
            name: "Border width",
            path: "components.Panel.footer.border.width",
            cssProperty: "border-width",
          },
          {
            name: "Border style",
            path: "components.Panel.footer.border.style",
            cssProperty: "border-style",
          },
          {
            name: "Border color",
            path: "components.Panel.footer.border.color",
            type: "color",
          },
        ],
      },
    ],
  },
  {
    name: "Table",
    title: "Table",
    icon: "list",
    editors: [
      {
        name: "Table",
        props: [
          {
            name: "Color",
            path: "components.Table.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.Table.bg",
            type: "color",
          },
          {
            name: "Border color",
            path: "components.Table.border.color",
            type: "color",
          },
        ],
      },
      {
        name: "Header",
        props: [
          {
            name: "Color",
            path: "components.Table.header.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.Table.header.bg",
            type: "color",
          },
          {
            name: "Font weight",
            path: "components.Table.header.fontWeight",
            cssProperty: "font-weight",
          },
        ],
      },
      {
        name: "Row",
        props: [
          {
            name: "Striped color",
            path: "components.Table.row_odd.color",
            type: "color",
          },
          {
            name: "Striped background",
            path: "components.Table.row_odd.bg",
            type: "color",
          },
          {
            name: "Active color",
            path: "components.Table.row_active.color",
            type: "color",
          },
          {
            name: "Active background",
            path: "components.Table.row_active.bg",
            type: "color",
          },
          {
            name: "Hover color",
            path: "components.Table.row_hover.color",
            type: "color",
          },
          {
            name: "Hover background",
            path: "components.Table.row_hover.bg",
            type: "color",
          },
        ],
      },
      {
        name: "Cell",
        props: [
          {
            name: "Padding",
            path: "components.Table.cell.padding",
            cssProperty: "padding",
          },
          {
            name: "Active color",
            path: "components.Table.cell_active.color",
            type: "color",
          },
          {
            name: "Active background",
            path: "components.Table.cell_active.bg",
            type: "color",
          },
        ],
      },
    ],
  },
  {
    name: "CommandBar",
    title: "Tools",
    editors: [
      {
        name: "Divider",
        props: [
          {
            name: "Color",
            path: "components.CommandBar.divider.color",
            type: "color",
          },
        ],
      },
      {
        name: "Button",
        props: [
          {
            name: "Padding",
            path: "components.CommandBar.button.padding",
            cssProperty: "padding",
          },
          {
            name: "Color",
            path: "components.CommandBar.button.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.CommandBar.button.bg",
            type: "color",
          },
          {
            name: "Border color",
            path: "components.CommandBar.button.border.color",
            type: "color",
          },
          {
            name: "Border radius",
            path: "components.CommandBar.button.border.radius",
            cssProperty: "border-radius",
          },
          {
            name: "Hover color",
            path: "components.CommandBar.button_hover.color",
            type: "color",
          },
          {
            name: "Hover background",
            path: "components.CommandBar.button_hover.bg",
            type: "color",
          },
          {
            name: "Hover border color",
            path: "components.CommandBar.button_hover.border.color",
            type: "color",
          },
          {
            name: "Active color",
            path: "components.CommandBar.button_active.color",
            type: "color",
          },
          {
            name: "Active background",
            path: "components.CommandBar.button_active.bg",
            type: "color",
          },
          {
            name: "Active border color",
            path: "components.CommandBar.button_active.border.color",
            type: "color",
          },
        ],
      },
    ],
  },
  {
    name: "Dropdown",
    title: "Dropdown",
    icon: "dropdown",
    editors: [
      {
        name: "Dropdown",
        props: [
          {
            name: "Padding",
            path: "components.Dropdown.padding",
            cssProperty: "padding",
          },
          {
            name: "PaddingX",
            path: "components.Dropdown.paddingX",
            cssProperty: "padding",
          },
          {
            name: "PaddingY",
            path: "components.Dropdown.paddingY",
            cssProperty: "padding",
          },
          {
            name: "Gap",
            path: "components.Dropdown.gap",
            cssProperty: "gap",
          },
          {
            name: "zIndex",
            path: "components.Dropdown.zIndex",
            cssProperty: "z-index",
          },
          {
            name: "Color",
            path: "components.Dropdown.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.Dropdown.bg",
            type: "color",
          },
          {
            name: "Border width",
            path: "components.Dropdown.border.width",
            cssProperty: "border-width",
          },
          {
            name: "Border color",
            path: "components.Dropdown.border.color",
            type: "color",
          },
          {
            name: "Border radius",
            path: "components.Dropdown.border.radius",
            cssProperty: "border-radius",
          },
        ],
      },
      {
        name: "Header",
        props: [
          {
            name: "Padding",
            path: "components.Dropdown.header.padding",
            cssProperty: "padding",
          },
          {
            name: "PaddingX",
            path: "components.Dropdown.header.paddingX",
            cssProperty: "padding",
          },
          {
            name: "PaddingY",
            path: "components.Dropdown.header.paddingY",
            cssProperty: "padding",
          },
          {
            name: "Color",
            path: "components.Dropdown.header.color",
            type: "color",
          },
        ],
      },

      {
        name: "Item",
        props: [
          {
            name: "Padding",
            path: "components.Dropdown.item.padding",
            cssProperty: "padding",
          },
          {
            name: "PaddingX",
            path: "components.Dropdown.item.paddingX",
            cssProperty: "padding",
          },
          {
            name: "PaddingY",
            path: "components.Dropdown.item.paddingY",
            cssProperty: "padding",
          },
          {
            name: "Color",
            path: "components.Dropdown.item.color",
            type: "color",
          },
          {
            name: "Hover color",
            path: "components.Dropdown.item_hover.color",
            type: "color",
          },
          {
            name: "Hover background",
            path: "components.Dropdown.item_hover.bg",
            type: "color",
          },
          {
            name: "Active color",
            path: "components.Dropdown.item_active.color",
            type: "color",
          },
          {
            name: "Active background",
            path: "components.Dropdown.item_active.bg",
            type: "color",
          },
        ],
      },
      {
        name: "Divider",
        props: [
          {
            name: "Background",
            path: "components.Dropdown.divider.bg",
            type: "color",
          },
        ],
      },
    ],
  },
  {
    name: "NavMenu",
    title: "Nav Menu",
    icon: "menu",
    editors: [
      {
        name: "Menu",
        props: [
          {
            name: "Color",
            path: "components.NavMenu.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.NavMenu.bg",
            type: "color",
          },
          {
            name: "Width",
            path: "components.NavMenu.width",
            cssProperty: "width",
          },
          {
            name: "Border right",
            path: "components.NavMenu.borderRight",
            cssProperty: "border",
          },
          {
            name: "Margin",
            path: "components.NavMenu.margin",
            cssProperty: "margin",
          },
        ],
      },
      {
        name: "Hover menu",
        props: [
          {
            name: "Border width",
            path: "components.NavMenu.border.width",
            cssProperty: "border-width",
          },
          {
            name: "Border style",
            path: "components.NavMenu.border.style",
            cssProperty: "border-style",
          },
          {
            name: "Border color",
            path: "components.NavMenu.border.color",
            type: "color",
          },
          {
            name: "Border radius",
            path: "components.NavMenu.border.radius",
            cssProperty: "border-radius",
          },
          {
            name: "Shadow",
            path: "components.NavMenu.shadow",
            cssProperty: "box-shadow",
          },
          {
            name: "zIndex",
            path: "components.NavMenu.zIndex",
            cssProperty: "z-index",
          },
        ],
      },
      {
        name: "Header",
        props: [
          {
            name: "Color",
            path: "components.NavMenu.header.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.NavMenu.header.bg",
            type: "color",
          },
          {
            name: "Padding",
            path: "components.NavMenu.header.padding",
            cssProperty: "padding",
          },
        ],
      },
      {
        name: "Buttons",
        props: [
          {
            name: "Width",
            path: "components.NavMenu.buttons.width",
            cssProperty: "width",
          },
          {
            name: "Background",
            path: "components.NavMenu.buttons.bg",
            type: "color",
          },
        ],
      },
      {
        name: "Icon",
        props: [
          {
            name: "Color",
            path: "components.NavMenu.icon.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.NavMenu.icon.bg",
            type: "color",
          },
          {
            name: "Hover color",
            path: "components.NavMenu.icon_hover.color",
            type: "color",
          },
          {
            name: "Hover background",
            path: "components.NavMenu.icon_hover.bg",
            type: "color",
          },
          {
            name: "Active color",
            path: "components.NavMenu.icon_active.bg",
            type: "color",
          },
          {
            name: "Active background",
            path: "components.NavMenu.icon_active.bg",
            type: "color",
          },
        ],
      },
      {
        name: "Item",
        props: [
          {
            name: "Border radius",
            path: "components.NavMenu.item.border.radius",
            cssProperty: "border-radius",
          },
          {
            name: "Hover color",
            path: "components.NavMenu.item_hover.color",
            type: "color",
          },
          {
            name: "Hover background",
            path: "components.NavMenu.item_hover.bg",
            type: "color",
          },
          {
            name: "Active color",
            path: "components.NavMenu.item_active.bg",
            type: "color",
          },
          {
            name: "Active background",
            path: "components.NavMenu.item_active.bg",
            type: "color",
          },
        ],
      },
    ],
  },
  {
    name: "NavTabs",
    title: "Nav Tabs",
    icon: "tabs",
    editors: [
      {
        name: "Text",
        props: [
          {
            name: "Padding",
            path: "components.NavTabs.text.padding",
            cssProperty: "padding",
          },
          {
            name: "Transform",
            path: "components.NavTabs.text.transform",
            cssProperty: "text-transform",
          },
        ],
      },
      {
        name: "Icon",
        props: [
          {
            name: "Padding",
            path: "components.NavTabs.icon.padding",
            cssProperty: "padding",
          },
          {
            name: "Color",
            path: "components.NavTabs.icon.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.NavTabs.icon.bg",
            type: "color",
          },
          {
            name: "Hover color",
            path: "components.NavTabs.icon_hover.color",
            type: "color",
          },
          {
            name: "Hover background",
            path: "components.NavTabs.icon_hover.bg",
            type: "color",
          },
          {
            name: "Active color",
            path: "components.NavTabs.icon_active.color",
            type: "color",
          },
          {
            name: "Active background",
            path: "components.NavTabs.icon_active.bg",
            type: "color",
          },
        ],
      },

      {
        name: "Tab",
        props: [
          {
            name: "Padding",
            path: "components.NavTabs.item.padding",
            cssProperty: "padding",
          },
          {
            name: "Color",
            path: "components.NavTabs.item.color",
            type: "color",
          },
          {
            name: "Background",
            path: "components.NavTabs.item.bg",
            type: "color",
          },
          {
            name: "Font weight",
            path: "components.NavTabs.item.fontWeight",
            cssProperty: "font-weight",
          },
          {
            name: "Hover color",
            path: "components.NavTabs.item_hover.color",
            type: "color",
          },
          {
            name: "Hover background",
            path: "components.NavTabs.item_hover.bg",
            type: "color",
          },
          {
            name: "Hover font weight",
            path: "components.NavTabs.item_hover.fontWeight",
            cssProperty: "font-weight",
          },
          {
            name: "Active color",
            path: "components.NavTabs.item_active.color",
            type: "color",
          },
          {
            name: "Active background",
            path: "components.NavTabs.item_active.bg",
            type: "color",
          },
          {
            name: "Active font weight",
            path: "components.NavTabs.item_active.fontWeight",
            cssProperty: "font-weight",
          },
        ],
      },
      {
        name: "Indicator",
        props: [
          {
            name: "Height",
            path: "components.NavTabs.indicator.height",
            cssProperty: "height",
          },
          {
            name: "Background",
            path: "components.NavTabs.indicator.bg",
            type: "color",
          },
        ],
      },
    ],
  },
];
