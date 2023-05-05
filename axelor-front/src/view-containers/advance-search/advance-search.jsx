import isNaN from "lodash/isNaN";
import isNumber from "lodash/isNumber";
import isObject from "lodash/isObject";
import React, {
  useCallback,
  useEffect,
  useReducer,
  useRef,
  useState,
} from "react";

import { Box, ClickAwayListener, Divider, FocusTrap, Popper } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { toKebabCase } from "@/utils/names";
import { i18n } from "@/services/client/i18n";

import FilterEditor, { defaultState as initialEditorState } from "./editor";
import FilterList from "./filter-list";
import { getCriteria } from "./utils";

import styles from "./advance-search.module.scss";
import { SearchInput } from "./search-input";

function getJSON(data) {
  try {
    return isObject(data) ? data : JSON.parse(data || "{}");
  } catch {
    return {};
  }
}

function getFilter(
  filters,
  domain,
  values,
  filterCustom = {},
  fields,
  { contextField, userId, userGroup, isSingleFilter } = {}
) {
  let { operator, criteria } = filterCustom;
  const selectedFilters = filters.filter((x) => values.includes(x.id));
  const selectedDomains = domain.filter((x) => values.includes(x.id));
  const graphqlFilter = {
    criteria: [],
    operator: operator || "and",
  };

  function mapCriteria(criteria) {
    return criteria
      .map((c) =>
        getCriteria(c, fields, {
          userId,
          userGroup,
        })
      )
      .filter((c) => c);
  }

  if (criteria) {
    criteria = mapCriteria(criteria);
  }

  if (contextField && contextField.name && contextField.value) {
    const { name, value } = contextField;
    criteria = [
      {
        operator: "and",
        criteria: [{ fieldName: `${name}.id`, operator: "=", value: value.id }],
      },
      ...(criteria?.length ? [{ operator: "and", criteria }] : []),
    ];
  }

  if (isSingleFilter) {
    graphqlFilter.criteria = criteria;
  } else {
    if (selectedFilters.length) {
      graphqlFilter.criteria.push(
        ...selectedFilters.map((filter) => {
          const { criteria = [], operator } = getJSON(filter.filterCustom);
          return { operator, criteria: mapCriteria(criteria) };
        })
      );
      if (graphqlFilter.criteria.length === 1) {
        graphqlFilter.operator =
          graphqlFilter.criteria[0].operator || graphqlFilter.operator;
        graphqlFilter.criteria = graphqlFilter.criteria[0].criteria;
      }
    }
    graphqlFilter.criteria.push(...(criteria || []));
  }
  if (selectedDomains.length) {
    graphqlFilter._domains = selectedDomains.map(({ title, domain }) => ({
      title,
      domain,
    }));
  }
  return graphqlFilter;
}

const useStateReset = (value, setter) => {
  useEffect(() => {
    value !== undefined && setter(value);
  }, [value, setter]);
};

function AdvanceSearch({
  canShare = true,
  canExportFull = true,
  customSearch = true,
  freeSearch = "all",
  userId,
  userGroup,
  value,
  setValue,
  fields,
  items,
  filters,
  domains,
  onExport,
  onSave,
  onDelete,
  onSearch,
}) {
  let anchorEl = useRef();
  const [open, setOpen] = useState(false);
  const [activeFilters, setActiveFilters] = useState([]);
  const [isSingleFilter, setFilterSingle] = useState(false);
  const [contextField, setContextField] = useState({
    name: "",
    value: null,
  });
  const [customFilter, setCustomFilter] = useState(initialEditorState);
  const [savedFilter, setSavedFilter] = useState(null);
  const [applyCount, autoApplyFilter] = useReducer((x) => x + 1, 0);

  const rtl = false;

  const { isArchived: archived, operator } = customFilter;

  const handleClose = useCallback(function handleClose() {
    setOpen(false);
  }, []);

  const handleOpen = useCallback(function handleOpen() {
    setOpen(true);
  }, []);

  function applyCustomSearch(text) {
    if (!text) return handleApply(customFilter);
    const freeSearchList = freeSearch.split(",");
    const viewFields = (items || []).filter((item) => {
      if (item.searchable === false) return false;
      if (freeSearch === "all") {
        return true;
      }
      if (freeSearch === "none") {
        return false;
      }
      if (freeSearch) {
        return freeSearchList.includes(item.name);
      }
      return true;
    });

    const getCriteria = () => {
      if (text) {
        const filters = [];
        const number = +text;

        viewFields.forEach((item) => {
          let fieldName = null,
            operator = "like",
            value = text;

          const field = fields[item.name] || item;
          const { name, targetName, jsonField } = field;

          const type = toKebabCase(field.type);
          switch (type) {
            case "integer":
            case "long":
            case "decimal":
              if (isNaN(number) || !isNumber(number)) return;
              if (
                type === "integer" &&
                (number > 2147483647 || number < -2147483648)
              )
                return;
              fieldName = name;
              operator = "=";
              value = number;
              break;
            case "text":
            case "string":
              fieldName = name;
              break;
            case "one-to-one":
            case "many-to-one":
              if (jsonField) {
                fieldName = name;
              } else if (targetName) {
                fieldName = name + "." + targetName;
              }
              break;
            case "boolean":
              if (/^(t|f|y|n|true|false|yes|no)$/.test(text)) {
                fieldName = name;
                operator = "=";
                value = /^(t|y|true|yes)$/.test(text);
              }
              break;
            default:
              break;
          }

          if (!fieldName) return;

          filters.push({
            fieldName: fieldName,
            operator: operator,
            value: value,
          });
        });

        return filters;
      }
      return [];
    };

    setValue({
      state: {
        contextField,
        customFilter,
        isSingleFilter,
        activeFilters,
        searchText: text,
      },
      selected: [],
      query: {
        freeSearchText: text,
        archived,
        criteria: getCriteria(),
      },
    });
    onSearch?.({});
  }

  function applyCustomFilters(filterCustom, selected = [null]) {
    setValue({
      state: {
        contextField,
        customFilter: filterCustom,
        isSingleFilter,
        activeFilters,
      },
      selected,
      query: {
        archived,
        ...getFilter(filters, domains, activeFilters, filterCustom, fields, {
          contextField,
          userId,
          userGroup,
          isSingleFilter,
        }),
      },
    });
    onSearch?.({});
  }

  const applyFilters = useCallback(
    function applyFilters(
      values,
      state = initialEditorState,
      hasApplied,
      isSingleFilter
    ) {
      state = { ...state, operator };
      let query = { archived, operator, criteria: [] };
      if (values.length > 0 || (contextField?.name && contextField?.value)) {
        query = {
          archived,
          operator,
          ...getFilter(filters, domains, values, { operator }, fields, {
            contextField,
            userId,
            userGroup,
          }),
        };
      }

      setValue({
        state: {
          contextField,
          isSingleFilter,
          activeFilters: values,
          customFilter: hasApplied
            ? (() => {
                const current = filters.find((x) => x.id === values[0]);
                if (current) {
                  const { criteria, operator } = getJSON(current.filterCustom);
                  return {
                    ...current,
                    criteria,
                    operator,
                  };
                }
                return state;
              })()
            : state,
        },
        selected: [...values],
        query: query,
      });
      onSearch?.({});
    },
    [
      archived,
      operator,
      filters,
      domains,
      setValue,
      fields,
      contextField,
      userId,
      userGroup,
      onSearch,
    ]
  );

  const resetCustomFilter = useCallback(() => {
    setCustomFilter((state) => ({
      ...initialEditorState,
      operator: state.operator,
    }));
  }, []);

  const handleFilterClick = useCallback(
    function handleFilterClick(filter, checked) {
      const isDomainFilter = !!filter.domain;
      const active = checked && filter ? [filter.id] : [];
      const isSingleFilter = isDomainFilter || !customSearch ? false : checked;
      resetCustomFilter();
      setFilterSingle(isSingleFilter);
      setActiveFilters(active);
      applyFilters(active, initialEditorState, checked, isSingleFilter);
    },
    [customSearch, applyFilters, resetCustomFilter]
  );

  const handleFilterCheck = useCallback(
    function handleFilterCheck({ id }, checked) {
      setActiveFilters((activeFilters) => {
        const ind = activeFilters.findIndex((x) => x === id);
        if (ind > -1) {
          !checked && activeFilters.splice(ind, 1);
        } else {
          checked && activeFilters.push(id);
        }
        return [...activeFilters];
      });
      if (!customSearch) {
        resetCustomFilter();
        autoApplyFilter();
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [customSearch]
  );

  function handleClear() {
    setValue({});
    setActiveFilters([]);
    setFilterSingle(false);
    setContextField((state) => ({ ...state, value: null }));
    resetCustomFilter();
    onSearch?.({});
  }

  function handleApply(customFilter, hasAuto) {
    const criteria = (customFilter.criteria || []).filter(
      (x) => x.fieldName && x.operator
    );
    if (customFilter && criteria.length) {
      applyCustomFilters(
        {
          ...customFilter,
          criteria,
        },
        hasAuto ? activeFilters : [null]
      );
    } else if (activeFilters.length > 0) {
      applyFilters(
        activeFilters.length ? [...activeFilters] : [null],
        customFilter
      );
    } else {
      applyFilters([], customFilter);
    }
    !hasAuto && setOpen(false);
  }

  async function handleFilterSave(filter) {
    const record = await onSave(filter);
    record && setSavedFilter(record);
  }

  async function handleFilterRemove(filter) {
    const hasRemoved = await onDelete(filter);
    hasRemoved && handleClear();
  }

  const state = value?.state || {};
  const criteriaCount = (state?.criteria || []).length;

  useStateReset(state.customFilter, setCustomFilter);
  useStateReset(state.activeFilters, setActiveFilters);
  useStateReset(state.isSingleFilter, setFilterSingle);
  useStateReset(state.contextField, setContextField);

  useEffect(() => {
    if (savedFilter) {
      // apply new saved filter
      handleFilterClick(savedFilter, true);
      // clear saved filter for new save
      setSavedFilter(null);
    }
  }, [filters, savedFilter, handleFilterClick]);

  useEffect(() => {
    applyCount && handleApply(customFilter, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [applyCount]);

  return (
    <Box className={styles.root} ref={anchorEl}>
      <SearchInput
        text={state.searchText}
        search={freeSearch !== "none"}
        value={value && value.selected}
        filters={filters}
        domains={domains}
        count={criteriaCount}
        onOpen={handleOpen}
        onClear={handleClear}
        onSearch={applyCustomSearch}
      />
      <Popper
        open={open}
        target={anchorEl.current}
        placement={`bottom-${rtl ? "end" : "start"}`}
      >
        <ClickAwayListener onClickAway={handleClose}>
          <Box
            {...(rtl ? { dir: "rtl" } : {})}
            shadow
            className={styles.popper}
            p={2}
          >
            <FocusTrap enabled={open}>
              <Box d="flex" flexDirection="column">
                <Box d="flex" alignItems="center">
                  <Box as="p" mb={0} p={1} flex={1} fontWeight="bold">
                    {i18n.get("Advanced Search")}
                  </Box>
                  <Box as="span" className={styles.close} onClick={handleClose}>
                    <MaterialIcon icon="close" />
                  </Box>
                </Box>
                <Divider />
                <Box d="flex" alignItems="flex-start" mb={customSearch ? 0 : 1}>
                  {domains.length > 0 && (
                    <FilterList
                      title={i18n.get("Filters")}
                      items={domains}
                      active={activeFilters}
                      disabled={isSingleFilter}
                      onFilterClick={handleFilterClick}
                      onFilterChange={handleFilterCheck}
                    />
                  )}
                  {filters.length > 0 && (
                    <FilterList
                      title={i18n.get("My Filter")}
                      items={filters}
                      active={activeFilters}
                      disabled={isSingleFilter}
                      onFilterClick={handleFilterClick}
                      onFilterChange={handleFilterCheck}
                    />
                  )}
                  {!customSearch &&
                    domains.length === 0 &&
                    filters.length === 0 && (
                      <Box as="p" mb={0} p={1} flex={1}>
                        {i18n.get("No filters available")}
                      </Box>
                    )}
                </Box>
                {customSearch && (filters.length > 0 || domains.length > 0) && (
                  <Divider mt={1} />
                )}
                {customSearch && (
                  <FilterEditor
                    {...{
                      canShare,
                      canExportFull,
                      fields,
                      contextField,
                      setContextField,
                      filter: customFilter,
                      setFilter: setCustomFilter,
                      onApply: handleApply,
                      onClear: handleClear,
                      onExport,
                      onSave: handleFilterSave,
                      onDelete: handleFilterRemove,
                    }}
                  />
                )}
              </Box>
            </FocusTrap>
          </Box>
        </ClickAwayListener>
      </Popper>
    </Box>
  );
}

AdvanceSearch.defaultProps = {
  filters: [],
  domains: [],
};

export default React.memo(AdvanceSearch);
