import { useMemo } from "react";

import { Box } from "@axelor/ui";

import { i18n } from "@/services/client/i18n";
import { moment } from "@/services/client/l10n";
import { MetaData } from "@/services/client/meta.ts";
import { Property, Field } from "@/services/client/meta.types";
import format, { getDateFormat, getDateTimeFormat } from "@/utils/format";
import { toSnakeCase } from "@/utils/names";
import { sanitize } from "@/utils/sanitize.ts";
import { FormProps } from "@/views/form/builder";

import { MessageTrack } from "./types";

import styles from "./message-track.module.scss";

export function formatter(item: MessageTrack, field?: Property) {

  function formatValue(value?: MessageTrack["value"]) {
    if (!value) {
      return value;
    }

    if (value.toLowerCase() === "true") return i18n.get("True");
    if (value.toLowerCase() === "false") return i18n.get("False");

    if (
      field &&
      !["MANY_TO_ONE", "ONE_TO_MANY", "ONE_TO_ONE", "MANY_TO_MANY"].includes(
        toSnakeCase(field.type).toUpperCase(),
      )
    ) {
      let formattedValue = format(value, {
        props: field as unknown as Field,
        context: { [field.name]: value },
      });
      
      // multi-select hack
      if (value && !formattedValue && field?.selection && value.indexOf(",") > 0) {
        formattedValue = format(value, {
          props: {
            ...(field as unknown as Field),
            widget: "multi-select"
          },
          context: { [field.name]: value },
        });
      }

      if (formattedValue !== value) {
        return formattedValue;
      }
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?(\.\d+)?$/.test(value)) {
      return moment(value).format(getDateTimeFormat());
    }
    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return moment(value).format(getDateFormat());
    }
    if (value === "0E-10") value = "0.000000000000";
    return value;
  }
  let displayValue = formatValue(item.value);
  const oldDisplayValue = formatValue(item.oldValue);
  if (oldDisplayValue !== undefined) {
    displayValue =
      oldDisplayValue +
      " → " +
      (displayValue ||
        `<span class="${styles["empty-value"]}">${i18n.get("None")}</span>`);
  }

  return displayValue;
}

function MessageTrackComponent({
  track,
  fields,
  jsonFields,
}: {
  track: MessageTrack;
  fields?: FormProps["fields"];
  jsonFields?: MetaData["jsonFields"];
}) {
  const field = useMemo(() => {
    const parts = track?.name.split(".");
    const jsonField = jsonFields?.[parts[0]];
    if (jsonField) {
      return jsonField[parts[1]] as unknown as Property;
    }
    return fields?.[track?.name];
  }, [track, fields, jsonFields]);
  const value = formatter(track, field);
  return (
    <li className={styles["track-field"]}>
      <Box d="flex" alignItems="center">
        <Box as="p" mb={0} me={1} fontWeight="bold">
          {i18n.get(track.title)} {" : "}
        </Box>
        {value && (
          <span dangerouslySetInnerHTML={{ __html: sanitize(value) }} />
        )}
      </Box>
    </li>
  );
}

export function MessageTracks({
  data,
  fields,
  jsonFields,
}: {
  data: MessageTrack[];
  fields?: FormProps["fields"];
  jsonFields?: MetaData["jsonFields"];
}) {
  return (
    <Box as="ul" m={0} p={0} pe={1}>
      {data.map((track, index) => (
        <MessageTrackComponent
          key={index}
          track={track}
          fields={fields}
          jsonFields={jsonFields}
        />
      ))}
    </Box>
  );
}
