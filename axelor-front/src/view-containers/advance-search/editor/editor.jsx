import { Box, Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { produce } from "immer"
import { useCallback, useEffect, useMemo } from "react";

import { moment } from "@/services/client/l10n";
import {
  BooleanCheckBox,
  BooleanRadio,
  ButtonLink,
  RelationalWidget,
  Select,
  SimpleButton,
} from "./components";
import Criteria from "./criteria";

export const defaultState = {
  title: "",
  operator: "and",
  isArchived: false,
  isShare: false,
  criteria: [{}],
  selected: false,
};

function EditorInput({
  t,
  canShare,
  id,
  title,
  shared,
  onChange,
  onSave,
  onRemove,
}) {
  const hide = ["", null, undefined].includes(id);
  return (
    <Box d="flex" flexDirection="column" p={1} g={2}>
      <Box d="flex" g={2} alignItems="center">
        <Input
          name="title"
          className="title"
          placeholder={t("Save filter as")}
          value={title}
          onChange={(e) => onChange("title", e.target.value)}
        />
        {canShare && (
          <BooleanCheckBox
            title={t("Share")}
            name="shared"
            value={shared}
            onChange={({ name, checked }) => onChange(name, checked)}
          />
        )}
      </Box>
      {(id || title || !hide) && (
        <Box d="flex" alignItems="center">
          {id || title ? (
            <SimpleButton title={t("Save")} onClick={onSave} />
          ) : null}
          {hide || <SimpleButton onClick={onSave} title={t("Update")} />}
          {hide || (
            <SimpleButton
              variant="danger"
              onClick={onRemove}
              title={t("Delete")}
              hide={hide}
            />
          )}
        </Box>
      )}
    </Box>
  );
}

export default function Editor({
  t,
  canExportFull,
  canShare,
  contextField,
  setContextField,
  filter,
  setFilter,
  fields,
  onClear,
  onApply,
  onSave,
  onExport,
  onDelete,
}) {
  const {
    id,
    operator,
    isArchived,
    criteria: criterias,
    title,
    shared,
  } = filter;

  const handleChange = useCallback(
    function handleChange(name, value) {
      setFilter((criteria) => ({
        ...criteria,
        [name]: value,
      }));
    },
    [setFilter]
  );

  const handleCriteriaAdd = useCallback(
    function handleCriteriaAdd() {
      setFilter(
        produce((draft) => {
          draft.criteria[draft.criteria.length - 1].operator &&
            draft.criteria.push({});
        })
      );
    },
    [setFilter]
  );

  const handleCriteriaRemove = useCallback(
    function handleCriteriaRemove(index) {
      setFilter(
        produce((draft) => {
          if (draft.criteria.length === 1) {
            draft.criteria = [{}];
          } else {
            draft.criteria.splice(index, 1);
          }
        })
      );
    },
    [setFilter]
  );

  const handleCriteriaChange = useCallback(
    function handleCriteriaChange({ name, value }, index) {
      function getDefaultValue(fieldName) {
        const field = fields[fieldName];
        const type = field?.type.toLowerCase();
        if (
          ["one_to_one", "many_to_one", "many_to_many", "one_to_many"].includes(
            type
          )
        ) {
          return null;
        } else if (["integer", "long", "decimal"].includes(type)) {
          return 0;
        } else if (["date", "time", "dateTime"].includes(type)) {
          return null;
        } else {
          return "";
        }
      }

      if (value instanceof moment) {
        value = moment(value).format("YYYY-MM-DD");
      }

      function shouldClearValue(prevOp, currentOp) {
        const pairs = [
          ["=", "!="],
          ["like", "notLike"],
          ["in", "notIn"],
        ];
        return !pairs.some(
          (pair) => pair.includes(prevOp) && pair.includes(currentOp)
        );
      }

      setFilter(
        produce((draft) => {
          const $prevValue = draft.criteria[index][name];
          draft.criteria[index][name] = value;
          if (name === "fieldName") {
            draft.criteria[index].operator = "";
            draft.criteria[index].value = getDefaultValue(value);
            draft.criteria[index].value2 = draft.criteria[index].value;
            draft.criteria[index].timeUnit = "";
          }
          if (name === "operator" && shouldClearValue($prevValue, value)) {
            draft.criteria[index].value = null;
            draft.criteria[index].value2 = null;
            draft.criteria[index].timeUnit = [
              "$inPast",
              "$inNext",
              "$inCurrent",
            ].includes(value)
              ? "day"
              : "";
          }
        })
      );
    },
    [setFilter, fields]
  );

  const handleFilterSave = useCallback(
    function handleFilterSave() {
      const { id, version, title, shared, operator, criteria } = filter;
      let savedFilter = {
        shared: shared || false,
        title: title,
        filterCustom: JSON.stringify({
          operator,
          criteria: criteria.map((c) => ({
            ...c,
            value: c.value === null ? undefined : c.value,
            value2: c.value2 === null ? undefined : c.value2,
            timeUnit: !c.timeUnit ? undefined : c.timeUnit,
          })),
        }),
      };
      if (id) {
        savedFilter.id = id;
        savedFilter.version = version;
      } else {
        savedFilter.name = title.replace(" ", "_").toLowerCase();
      }
      onSave(savedFilter);
    },
    [filter, onSave]
  );

  const handleFilterRemove = useCallback(
    function handleFilterRemove() {
      onDelete(filter);
    },
    [filter, onDelete]
  );

  const contextFields = useMemo(
    () =>
      Object.values(fields).reduce((ctxFields, field) => {
        const {
          contextField,
          contextFieldTitle,
          contextFieldValue,
          contextFieldTarget,
          contextFieldTargetName,
        } = field;
        if (contextField && !ctxFields.find((x) => x.name === contextField)) {
          ctxFields.push({
            name: contextField,
            title: `${contextField[0].toUpperCase()}${contextField.substr(1)}`,
            value: {
              id: contextFieldValue,
              [contextFieldTargetName]: contextFieldTitle,
            },
            target: contextFieldTarget,
            targetName: contextFieldTargetName,
          });
        }
        return ctxFields;
      }, []),
    [fields]
  );

  useEffect(() => {
    contextFields.length &&
      setContextField(
        produce((draft) => {
          !draft.name && (draft.name = contextFields[0].name);
        })
      );
  }, [contextFields, setContextField]);

  const $fields = useMemo(
    () =>
      Object.values(fields).filter((field) => {
        if (field.contextField) {
          return (
            contextField.name === field.contextField &&
            `${(contextField.value || {}).id}` === `${field.contextFieldValue}`
          );
        }
        return true;
      }),
    [contextField, fields]
  );
  const selectedContextField =
    contextField.name &&
    contextFields.find((x) => x.name === contextField.name);

  return (
    <Box d="flex" flexDirection="column" alignItems="start" g={2}>
      {contextFields.length > 0 && (
        <Box d="flex" alignItems="center">
          <Box
            aria-label="close"
            onClick={() =>
              setContextField((data) => ({ ...data, value: null }))
            }
          >
            <MaterialIcon icon="close" />
          </Box>
          <Select
            name={"ctxField"}
            onChange={(value) =>
              setContextField((data) => ({ ...data, name: value }))
            }
            value={contextField.name}
            options={contextFields}
          />
          {selectedContextField && (
            <RelationalWidget
              operator="in"
              isMulti={false}
              onChange={({ value }) =>
                setContextField((data) => ({ ...data, value }))
              }
              value={contextField.value}
              field={selectedContextField}
            />
          )}
        </Box>
      )}
      <Box d="flex" alignItems="center" g={2}>
        <BooleanRadio
          name="operator"
          onChange={(e) => handleChange("operator", e.target.value)}
          value={operator}
          data={[
            { label: t("and"), value: "and" },
            { label: t("or"), value: "or" },
          ]}
        />
        <BooleanCheckBox
          name="isArchived"
          title={t("Show archived")}
          value={isArchived}
          onChange={({ name, checked }) => handleChange(name, checked)}
          className="archived"
        />
      </Box>
      <Box d="flex" flexDirection="column" alignItems="flex-start" g={2}>
        {criterias.map((item, index) => (
          <Criteria
            t={t}
            key={index}
            index={index}
            value={item}
            fields={$fields}
            onRemove={handleCriteriaRemove}
            onChange={handleCriteriaChange}
          />
        ))}
      </Box>
      <Box d="flex" alignItems="center">
        <ButtonLink
          title={t("Add filter")}
          className={"add-filter"}
          onClick={handleCriteriaAdd}
        />
        <ButtonLink title={t("Clear")} className={"clear"} onClick={onClear} />
        <ButtonLink title={t("Export")} onClick={(e) => onExport()} />
        {canExportFull && (
          <>
            <ButtonLink
              title={t("Export full")}
              onClick={(e) => onExport(true)}
            />
          </>
        )}
        <ButtonLink
          title={t("Apply")}
          className={"apply-filter"}
          onClick={() => onApply(filter)}
        />
      </Box>
      <EditorInput
        t={t}
        canShare={canShare}
        id={id}
        title={title}
        shared={shared}
        onChange={handleChange}
        onSave={handleFilterSave}
        onRemove={handleFilterRemove}
      />
    </Box>
  );
}

Editor.defaultProps = {
  t: (e) => e,
};
