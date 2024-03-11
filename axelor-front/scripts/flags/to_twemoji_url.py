#!/usr/bin/env python3

import sys
import re

DEFAULT_BASE_URL_SOURCE = "../../node_modules/react-international-phone/dist/index.js"


def get_base_url(base_url_source):
    try:
        # Open and read the content of the file
        with open(base_url_source, "r") as file:
            file_content = file.read()

        # Regular expression pattern to find the URL containing 'twemoji' and 'svg'
        url_pattern = r'"([^"]*?\btwemoji\b[^"]*?\bsvg\b[^"]*?)"'

        # Searching the file content for the pattern
        match = re.search(url_pattern, file_content)

        if not match:
            raise ValueError("No URL found in the file")

        base_url = match.group(1)

        if base_url.startswith(":"):
            base_url = "https" + base_url
        if not base_url.endswith("/"):
            base_url += "/"

        return base_url
    except Exception as e:
        print(f"Failed to read the file {base_url_source!r}: {e}")


def country_code_to_twemoji_url(base_url, country_code):
    """
    Convert an ISO 3166-1 alpha-2 country code to a Twemoji URL.

    Args:
    - country_code (str): The country code to convert.

    Returns:
    - str: A URL to the corresponding Twemoji image.
    """
    unicode_scalar_values = [
        hex(ord(char) - ord("A") + 0x1F1E6)[2:] for char in country_code.upper()
    ]
    emoji_filename = "-".join(unicode_scalar_values) + ".svg"
    return base_url + emoji_filename


if __name__ == "__main__":
    base_url_source = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_BASE_URL_SOURCE
    base_url = get_base_url(base_url_source)
    for line in sys.stdin:
        country_code = line.strip()
        if country_code:  # Check if the line is not empty
            try:
                twemoji_url = country_code_to_twemoji_url(base_url, country_code)
                print(twemoji_url)
            except Exception as e:
                print(f"Error processing '{country_code}': {e}", file=sys.stderr)
