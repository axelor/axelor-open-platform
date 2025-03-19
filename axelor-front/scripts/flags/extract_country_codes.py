#!/usr/bin/env python3

import re
import sys

DEFAULT_FILE_PATH = "../../node_modules/react-international-phone/dist/types.d.ts"

def extract_country_codes_from_line_with_prefix(file_path, prefix):
    country_codes = []

    with open(file_path, "r") as file:
        for line in file:
            if line.startswith(prefix):
                # Match country codes
                pattern = re.compile(r"'(\w{2})'")
                matches = pattern.findall(line)
                country_codes.extend(matches)
                break  # Assuming only one line starts with the given prefix

    return sorted(set(country_codes))  # Sort and remove duplicates


if __name__ == "__main__":
    file_path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_FILE_PATH

    prefix = "export type CountryIso2 ="
    country_codes = extract_country_codes_from_line_with_prefix(file_path, prefix)

    for country_code in country_codes:
        print(country_code)
