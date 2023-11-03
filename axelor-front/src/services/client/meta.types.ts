import { PrimitiveAtom } from "jotai";
import { DataContext, DataRecord } from "./data.types";
import { AdvancedSearchState } from "@/view-containers/advance-search/types";

export type PropertyType =
  | "STRING"
  | "TEXT"
  | "BOOLEAN"
  | "INTEGER"
  | "LONG"
  | "DOUBLE"
  | "DECIMAL"
  | "DATE"
  | "TIME"
  | "DATETIME"
  | "BINARY"
  | "ENUM"
  | "ONE_TO_ONE"
  | "MANY_TO_ONE"
  | "ONE_TO_MANY"
  | "MANY_TO_MANY";

export type DirectionType = "horizontal" | "vertical";

export interface Property {
  name: string;
  type: PropertyType;
  mappedBy?: string;
  target?: string;
  targetName?: string;
  targetSearch?: string[];
  enumType?: string;
  primary?: boolean;
  required?: boolean;
  unique?: boolean;
  orphan?: boolean;
  maxSize?: number | string;
  minSize?: number | string;
  precision?: number;
  scale?: number | string;
  title?: string;
  help?: string;
  image?: boolean;
  nullable?: boolean;
  readonly?: boolean;
  hidden?: boolean;
  virtual?: boolean;
  transient?: boolean;
  json?: boolean;
  password?: boolean;
  massUpdate?: boolean;
  nameColumn?: boolean;
  sequence?: boolean;
  translatable?: boolean;
  encrypted?: boolean;
  defaultNow?: boolean;
  defaultValue?: any;
  nameSearch?: string[];
  selection?: string;
  version?: boolean;
  reference?: boolean;
  collection?: boolean;
  enum?: boolean;
  perms?: Perms;

  // x-* attributes
  order?: string;
  currency?: string;
  trueText?: string;
  falseText?: string;
  big?: boolean;
  seconds?: boolean;
  
  // For react expression/template data/dateTime formatting
  dateFormat?: string;
  timeFormat?: string;

  // by view service
  autoTitle?: string;
  placeholder?: string;
  selectionList?: Selection[];
  widget?: string;
  widgetAttrs?: Record<string, any>; // incoming is string, processed to object
  jsonField?: string;
}

export interface Widget {
  uid: string;
  type: string;
  name?: string;
  title?: string;
  help?: string;
  showTitle?: boolean;
  hidden?: boolean;
  readonly?: boolean;
  showIf?: string;
  hideIf?: string;
  readonlyIf?: string;
  depends?: string;
  colSpan?: string | number;
  colOffset?: string | number;
  rowSpan?: string | number;
  rowOffset?: string | number;
  css?: string;
  height?: string;
  width?: string;
  autoTitle?: string;
  widget?: string;
  widgetAttrs?: Record<string, any>; // incoming is string, processed to object
}

export interface LayoutContainer {
  cols?: number;
  colWidths?: string;
  itemSpan?: number;
  gap?: number | string;
}

export interface WidgetContainer extends Widget, LayoutContainer {}

export interface Viewer {
  type: "viewer";
  depends?: string;
  template?: string;
  fields?: Record<string, Property>; // incoming is array, processed to object
}

export interface Editor extends Omit<Panel, "type"> {
  type: "editor";
  layout?: string;
  viewer?: boolean;
  flexbox?: boolean;
  json?: boolean;
  showOnNew?: boolean;
  onNew?: string;
  items?: (Field | Button | Spacer | Separator | Label | Panel)[];
  fields?: Record<string, Property>; // incoming is array, processed to object
}

export interface Tooltip extends Omit<Viewer, "type"> {
  type: "tooltip";
  call?: string;
}

export interface Hilite {
  color?: string;
  background?: string;
  strong?: boolean;
  condition?: string;
  css?: string;
}

export interface Label extends Widget {
  type: "label";
}

export interface Spacer extends Widget {
  type: "spacer";
}

export interface Separator extends Widget {
  type: "seperator";
}

export interface Button extends Widget {
  type: "button";
  icon?: string;
  iconHover?: string;
  link?: string;
  prompt?: string;
  onClick?: string;
  title?: string;
  field?: string;
}

export interface ButtonGroup extends WidgetContainer {
  type: "button-group";
}

export interface MenuItem extends Widget {
  type: "menu-item";
  name: string;
  title: string;
  xmlId?: string;
  parent?: string;
  icon?: string;
  iconBackground?: string;
  action?: string;
  prompt?: string;
  order?: number;
  groups?: string;
  left?: boolean;
  mobile?: boolean;
  category?: string;
  tag?: string;
  tagGet?: string;
  tagCount?: boolean;
  tagStyle?: string;
  hasTag?: boolean;
}

export interface MenuDivider extends Widget {
  type: "menu-item-divider";
}

export interface Menu extends Widget {
  type: "menu";
  icon?: string;
  items?: (MenuItem | MenuDivider | Menu)[];
}

export interface Static extends Widget {
  type: "static";
  text?: string;
}

export interface Help extends Omit<Static, "type"> {
  type: "help";
}

export interface Field extends Widget, Omit<Property, "type" | "sequence"> {
  type: "field";
  name: string;
  serverType?: string;
  placeholder?: string;
  widget?: string;
  canSuggest?: boolean;
  canSelect?: string;
  canNew?: string;
  canView?: string;
  canEdit?: string;
  canRemove?: string;
  canCopy?: boolean;
  canExport?: boolean;
  onChange?: string;
  onSelect?: string;
  target?: string;
  targetName?: string;
  domain?: string;
  required?: boolean;
  requiredIf?: string;
  validIf?: string;
  minSize?: string;
  maxSize?: string;
  pattern?: string;
  fgColor?: string;
  bgColor?: string;
  selection?: string;
  selectionIn?: string;
  aggregate?: string;
  massUpdate?: boolean;
  sortable?: boolean;
  editWindow?: string;
  formView?: string;
  gridView?: string;
  summaryView?: string;
  bind?: string;
  enumType?: string;
  related?: string;
  create?: string;
  canReload?: boolean;
  callOnSave?: boolean;
  icon?: string;
  iconHover?: string;
  iconActive?: string;
  exclusive?: boolean;
  showIcons?: string;
  showBars?: boolean;
  direction?: DirectionType;
  codeSyntax?: string;
  codeTheme?: string;
  lite?: boolean;
  labels?: boolean;
  orderBy?: string;
  order?: string;
  limit?: number;
  searchLimit?: number;
  colorField?: string;
  accept?: string;
  popupMaximized?: string;
  jsonModel?: string;
  hilites?: Hilite[];
  tooltip?: Tooltip;
  views?: (FormView | GridView)[];
  selectionList?: Selection[];
  jsonFields?: JsonField[];
  viewer?: Viewer;
  editor?: Editor;
}

export interface Panel extends WidgetContainer {
  type: "panel";
  itemSpan?: number;
  showFrame?: boolean;
  sidebar?: boolean;
  stacked?: boolean;
  attached?: boolean;
  onTabSelect?: string;
  canCollapse?: boolean;
  collapseIf?: string;
  icon?: string;
  iconBackground?: string;
  menu?: Menu;
  items?: (
    | Field
    | Spacer
    | Label
    | Static
    | Separator
    | Help
    | Button
    | ButtonGroup
    | Panel
    | PanelRelated
    | PanelDashlet
    | PanelInclude
  )[];
}

export type SelectorType = "checkbox" | "none";

export interface PanelRelated extends Omit<Panel, "type"> {
  type: "panel-related";
  name?: string;
  serverType?: string;
  formView?: string;
  gridView?: string;
  searchLimit?: number;
  rowHeight?: number;
  selector?: SelectorType;
  editable?: boolean;
  required?: boolean;
  requiredIf?: string;
  validIf?: string;
  orderBy?: string;
  groupBy?: string;
  domain?: string;
  target?: string;
  targetName?: string;
  onNew?: string;
  onChange?: string;
  onSelect?: string;
  canSelect?: string;
  canNew?: string;
  canView?: string;
  canEdit?: string;
  canRemove?: string;
  canMove?: boolean;
  editWindow?: string;
  items?: (Field | Button)[];
  fields?: Property[];
  perms?: Perms;
  showBars?: boolean;
}

export interface PanelDashlet extends Omit<Panel, "type"> {
  type: "dashlet";
  action?: string;
  canSearch?: boolean;
  canEdit?: string;
  showBars?: boolean;
}

export interface PanelInclude extends Omit<Panel, "type"> {
  type: "include";
  name?: string;
  module?: string;
  view?: View;
}

export interface PanelTabs extends Widget {
  type: "panel-tabs";
  items?: (Panel | PanelRelated | PanelDashlet | PanelInclude)[];
}

export interface PanelStack extends Omit<PanelTabs, "type"> {
  type: "panel-stack";
}

export interface PanelMail extends Omit<Panel, "type" | "items"> {
  type: "panel-mail";
  items?: (MailMessages | MailFollowers)[];
}

export interface MailMessages extends Widget {
  type: "mail-messages";
  limit?: number;
}

export interface MailFollowers extends Widget {
  type: "mail-followers";
}

export interface Perms {
  read?: boolean;
  write?: boolean;
  create?: boolean;
  remove?: boolean;
  export?: boolean;
  massUpdate?: boolean;
}

export interface JsonField extends Omit<Field, "type"> {
  name: string;
  type: string;

  model: string;
  modelField: string; // same as jsonField

  sequence: number;
  columnSequence: number;
  visibleInGrid?: boolean;

  jsonTarget?: string;
  jsonField?: string;
  jsonPath?: string;
  jsonType?: string;

  contextField?: string;
  contextFieldTarget?: string;
  contextFieldTargetName?: string;
  contextFieldValue?: string;
  contextFieldTitle?: string;
}

export interface Selection {
  value?: string;
  title?: string;
  icon?: string;
  color?: string;
  order?: number;
  hidden?: boolean;
  data?: any;
}

export interface HelpOverride {
  type: "tooltip" | "placeholder" | "inline";
  field: string;
  help: string;
  style?: string;
}

export interface View {
  type: string;
  xmlId?: string;
  viewId?: number;
  modelId?: number;
  customViewId?: number;
  customViewShared?: boolean;
  name?: string;
  title?: string;
  css?: string;
  model?: string;
  editable?: boolean;
  groups?: string;
  helpLink?: string;
  width?: string;
  minWidth?: string;
  maxWidth?: string;
  helpOverride?: HelpOverride[];
  items?: Widget[];
}

export interface FormView extends View, LayoutContainer {
  type: "form";
  json?: boolean;
  onLoad?: string;
  onSave?: string;
  onNew?: string;
  showOnNew?: boolean;
  readonlyIf?: string;
  canNew?: string;
  canEdit?: string;
  canSave?: string;
  canDelete?: string;
  canArchive?: string;
  canCopy?: string;
  canAttach?: string;
  toolbar?: Button[];
  menubar?: Menu[];
  items?: (
    | Help
    | Panel
    | PanelInclude
    | PanelDashlet
    | PanelRelated
    | PanelStack
    | PanelTabs
    | PanelMail
  )[];
}

export interface GridView extends View {
  type: "grid";
  expandable?: boolean;
  sortable?: boolean;
  orderBy?: string;
  groupBy?: string;
  customSearch?: boolean;
  freeSearch?: string;
  onNew?: string;
  canNew?: boolean;
  canEdit?: boolean;
  canSave?: boolean;
  canDelete?: boolean;
  canArchive?: boolean;
  canMove?: boolean;
  editIcon?: boolean;
  rowHeight?: number;
  colWidth?: number;
  noFetch?: boolean;
  selector?: SelectorType;
  inlineHelp?: Help;
  toolbar?: Button[];
  menubar?: Menu[];
  hilites?: Hilite[];
  items?: (Field | Button)[];
}

export interface CardsView extends View {
  type: "cards";
  orderBy?: string;
  customSearch?: boolean;
  freeSearch?: string;
  toolbar?: Button[];
  menubar?: Menu[];
  items?: Widget[];
  hilites?: Hilite[];
  template?: string;
  canNew?: boolean;
  canEdit?: boolean;
  canDelete?: boolean;
  editWindow?: string;
}

export interface TreeNode {
  model?: string;
  parent?: string;
  onClick?: string;
  draggable?: boolean;
  domain?: string;
  orderBy?: string;
  items?: (TreeField | Button)[];
}

export interface TreeField extends Field {
  as?: string;
  selection?: string;
  onClick?: string;
}

export interface TreeColumn extends SearchField {}

export interface TreeView extends View {
  type: "tree";
  showHeader?: boolean;
  columns?: TreeColumn[];
  nodes?: TreeNode[];
}

export interface SearchField extends Omit<Field, "type"> {
  multiple?: boolean;
  type?: "string" | "integer" | "decimal" | "date" | "datetime" | "boolean";
}

export interface ChartCategory {
  key?: string;
  type?: string;
  title?: string;
}

export interface ChartSeries {
  key?: string;
  groupBy?: string;
  type?: string;
  side?: string;
  title?: string;
  aggregate?: string;
  scale?: number;
}

export interface ChartConfig {
  name?: string;
  value?: string;
  scale?: number;
  min?: number;
  max?: number;
  onClick?: string;
  hideLegend?: boolean;
}

export interface ChartAction {
  name?: string;
  title?: string;
  action?: string;
}

export interface ChartView extends View {
  type: "chart";
  title?: string;
  xAxis?: string;
  xTitle?: null | string;
  xType?: string;
  stacked?: boolean;
  onInit?: string;
  search?: SearchField[];
  category?: ChartCategory;
  config?: ChartConfig;
  series?: ChartSeries[];
  actions?: ChartAction[];
  usingSQL?: boolean;
}

export interface KanbanView extends View {
  type: "kanban";
  template?: string;
  customSearch?: boolean;
  freeSearch?: string;
  editWindow?: string;
  columnBy?: string;
  sequenceBy?: string;
  draggable?: boolean;
  onNew?: string;
  onMove?: string;
  limit?: number;
  columns?: Selection[];
  hilites?: Hilite[];
}

export type CalendarModeType = "month" | "week" | "day";

export interface CalendarView extends View {
  type: "calendar";
  mode?: CalendarModeType;
  colorBy?: string;
  onChange?: string;
  eventStart: string;
  eventStop?: string;
  eventLength?: number;
  dayLength?: number;
  items?: Widget[];
}

export type GanttModeType = "year" | CalendarModeType;

export interface GanttView extends View {
  type: "gantt";
  mode?: GanttModeType;
  taskStart?: string;
  taskDuration?: string;
  taskEnd?: string;
  taskParent?: string;
  taskSequence?: string;
  taskProgress?: string;
  taskUser?: string;
  startToStart?: string;
  startToFinish?: string;
  finishToStart?: string;
  finishToFinish?: string;
  items?: Widget[];
}

export interface CustomView extends View {
  type: "custom";
  items?: Widget[];
  template?: string;
}

export interface HtmlView extends View {
  type: "html";
  name?: string;
  resource?: string;
}

export interface Dashboard extends View {
  type: "dashboard";
  items?: PanelDashlet[];
}

export interface SearchView extends View {
  type: "search";
  limit?: number;
  searchForm?: string;
  searchFields?: SearchField[];
  hilites?: Hilite[];
  resultFields?: SearchResultField[];
  buttons?: Button[];
  selects?: SearchSelect[];
  actionMenus?: MenuItem[];
}

export interface SearchSelect {
  model?: string;
  title?: string;
  viewTitle?: string;
  selected?: boolean;
  formView?: string;
  gridView?: string;
  fields?: SearchSelectField[];
  limit?: number;
  distinct?: boolean;
}

export interface SearchSelectField {
  name?: string;
  as?: string;
  selectionList?: Selection[];
}

export interface SearchResultField extends SearchField {}

export interface SearchFilter {
  name?: string;
  title?: string;
  domain?: string;
  context?: SearchContext;
  checked?: boolean;
}

export interface SearchContext {
  name?: string;
  value?: string;
}

export interface SearchFilters extends View {
  type: "search-filters";
  items?: Widget[];
  filters?: SearchFilter[];
}

export type AdvancedSearchAtom = PrimitiveAtom<AdvancedSearchState>;

export interface SavedFilter {
  id: number;
  version?: number;
  name: string;
  title: string;
  shared: boolean;
  user?: DataRecord;
  filterView: string;
  filterCustom: string;
  filters?: string;
  checked?: boolean;
}

export interface ActionView {
  xmlId?: string;
  actionId?: number;
  model?: string;
  name: string;
  title: string;
  icon?: string;
  home?: boolean;
  domain?: string;
  viewType: string;
  views?: View[];
  context?: DataContext;
  params?: DataRecord;
  resource?: string; // url resource
}

export type ViewType =
  | GridView
  | FormView
  | CardsView
  | TreeView
  | CalendarView
  | KanbanView
  | GanttView
  | ChartView
  | CustomView
  | HtmlView
  | SearchView
  | SearchFilters;

export type ViewTypes<Types extends { type: string } = ViewType> = {
  [T in Types as T["type"]]: T;
};

// the above types will be too much restrictive during
// processing schema json. This simplified schema type
// can be used instead.
export interface Schema {
  type?: string;
  name?: string;
  title?: string;
  help?: string;
  items?: Schema[];
  [K: string]: any;
}

export type QuickMenuItem = {
  title: string;
  action?: string;
  selected?: boolean;
  model?: string;
  context?: DataContext;
};

export type QuickMenu = {
  title: string;
  order?: number;
  showingSelected?: boolean;
  items?: QuickMenuItem[];
};

export type Tag = {
  name: string;
  value: string;
  style: string;
};
