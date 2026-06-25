## 更新日志条目

#### 概述

`unreleased` 文件夹包含所有尚未发布的更新日志条目。

在发布时，所有未发布的条目将合并到最终的 `CHANGELOG.md` 文件中。

#### 更新日志条目

在 `changelogs/unreleased` 下创建一个新文件。

文件应为以下格式的 YAML 文件：

````yaml
---
title: 主题
type: feature
description: |
  某些描述内容 包含更多详细信息。
  以及一些关于重大更改和迁移步骤的详细信息。

  ```
  $ find -iregex ".*domains.*.xml" | xargs -l sed -i 's|cachable="|cacheable="|g'
  ```
````

`title` 描述该条目。

`type` 可以是：
* **feature** 表示新功能。
* **change** 表示现有功能的更改。
* **deprecate** 表示即将被移除的功能。
* **remove** 表示已移除的功能。
* **fix** 表示任何错误修复。
* **security** 表示存在漏洞的情况。

`description` 是可选的，应提供关于更改的详细描述，包括任何迁移步骤。

#### 生成 CHANGELOG.md

要使用未发布的条目生成 `CHANGELOG.md`，运行以下 Gradle 任务：

```
./gradlew generateChangelog
```

未发布的条目也会自动从 `changelogs/unreleased` 中移除。

还可以使用 `--preview` 参数来预览生成的更新日志，而不删除或更新文件。

#### 参考资料

* [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
* [Gitlab: 如何解决GitLab的CHANGELOG 冲突](https://about.gitlab.com/2018/07/03/solving-gitlabs-changelog-conflict-crisis/)
