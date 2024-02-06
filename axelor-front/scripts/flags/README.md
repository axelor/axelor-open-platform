# Combined Flag Image and CSS Generator Script for Offline Use

By default, `react-international-phone` relies on downloading Twemoji flag images from an external CDN.
This script generates a combine flags image asset and the required CSS file for offline use.

## How It Works

The script executes several steps to prepare the combined flags image and CSS file:

1. **Country Code Extraction:** Collects a list of country codes from `react-international-phone` dependency.
2. **Twemoji Conversion:** Transforms each country code into a specific Twemoji image URL.
3. **Local Download:** Downloads the Twemoji images to a working directory.
4. **Image Renaming and Combination:** Renames downloaded SVG files combines them into a single SVG file (`flags.svg`).
5. **CSS Generation:** Produces a CSS file (`flags.module.scss`) that maps each country code to its respective background image within the combined SVG.


## Usage

This directory contains several scripts required to performed the tasks mentioned above,
but you just need to run one of them to execute all the steps:

```sh
./generate_combined_flags_and_css.sh
```
