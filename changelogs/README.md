## Changelog entries

#### Overview

The `unreleased` folder contains all changelog entries that haven't been release yet.

At the release time, all unreleased entries are combined into final CHANGELOG.md file. 

#### Change log entry

Under `changelogs/unreleased`, create a new file.

The file is expected to be a YAML file in the following format:

```yaml
---
title: Some text
type: added
```
The `title` describe the entry.

`type` can be : 
* **added** for new features.
* **changed** for changes in existing functionality.
* **deprecated** for soon-to-be removed features.
* **removed** for now removed features.
* **fixed** for any bug fixes.
* **security** in case of vulnerabilities.

#### Generate CHANGELOG.md

To generate the `CHANGELOG.md` with unreleased entries, run following gradle task:
```
./gradlew generateChangeLog
```

The unreleased entries are also automatically removed from `changelogs/unreleased`.

`--preview` arguments can also be used to preview the generated change log without deleting/updating files.

#### Source

* [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
* [Gitlab: How we solved GitLab's CHANGELOG conflict crisis] (https://about.gitlab.com/2018/07/03/solving-gitlabs-changelog-conflict-crisis/)



