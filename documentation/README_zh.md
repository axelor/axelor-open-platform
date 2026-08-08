# Axelor开源平台文档

这是Axelor开源平台的文档。

文档使用 [Antora](https://antora.org/) 构建，这是一个用 [Asciidoc](https://asciidoctor.org/docs/what-is-asciidoc/) 编写的静态网站生成器。

## 本地预览

要生成 Antora 文档站点，您首先需要 [安装 Antora](https://docs.antora.org/antora/3.0/install/install-antora/)。
安装 Antora 后，运行 `antora` 命令并指定playbook文件。

```bash
$ antora antora-playbook-local.yml
```

站点将生成在 `build/preview/` 目录下。在浏览器中导航到 `build/preview/index.html` 以离线查看站点。
预览站点将使用 Antora 的 [默认 UI](https://gitlab.com/antora/antora-ui-default) 作为站点界面。它不反映最终的文档站点。
