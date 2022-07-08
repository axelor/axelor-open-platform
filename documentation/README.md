# Axelor Open Platform Documentation

Here is the documentation of Axelor Open Platform

The documentation is build using [Antora](https://antora.org/), a static site generator written in [Asciidoc](https://asciidoctor.org/docs/what-is-asciidoc/).

## Local Preview

To generate the Antora Docs site, you first need to [install Antora](https://docs.antora.org/antora/3.0/install/install-antora/). 
Once Antora is installed, run the `antora` command on the playbook file.

```bash
$ antora antora-playbook-local.yml
```

The site will be generated into `build/preview/`. Navigate to `build/preview/index.html` in your browser to view the site offline.
The preview site will use Antora’s [default UI](https://gitlab.com/antora/antora-ui-default) as the UI for the site. It doesn't reflect the final doc site.
