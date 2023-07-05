# Axelor Front

The next generation web frontend of Axelor.

## Pre-requisites

- node >= v18.14.0
- pnpm >= 8

```bash
# Nodejs
$ curl -fsSL https://deb.nodesource.com/setup_lts.x | bash -
$ sudo apt-get install -y nodejs

# Alternatively, `nvm` can be used as a Node Version Manager
$ curl https://raw.githubusercontent.com/creationix/nvm/master/install.sh | bash
$ source ~/.profile
$ nvm install 18

# pnpm
$ corepack enable
$ corepack prepare pnpm@latest-8 --activate
```

See more installation methods:
- pnpm : https://pnpm.io/installation
- Node.js : https://nodejs.org/en/download

## Quickstart

Run following command from terminal to install dependencies:

```bash
pnpm install
```

In [.env](.env), update the proxy settings. This will forward requests to that instance :

```conf
# Target host to proxy to
VITE_PROXY_TARGET=http://localhost:8080
VITE_PROXY_CONTEXT=/
```

Then, to start dev server, run following command from terminal:

```bash
pnpm dev
```

Wait and the application should start in browser at: http://localhost:5173/

## Build

Run following command to create a directory with a production build of your app :

```
pnpm build
```

Production build are available under `dist` directory.

## Troubleshooting

### Developing along with `@axelor/ui`

For development purposes we suggest using `pnpm link` to link `@axelor/ui` library. 

Make sure to clone `axelor-ui` project next to the `axelor-open-platform` sources :
```
.
├── axelor-open-platform
│   ├── axelor-common
│   ├── axelor-core
│   ├── axelor-front
│   ├── axelor-gradle
│   ├── axelor-test
│   ├── axelor-tomcat
│   ├── axelor-tools
│   ├── axelor-web
├── axelor-ui
```
Then, navigate to `axelor-open-platform/axelor-front/` and run `pnpm link ../../axelor-ui` 
to use the linked version of the library.

### JavaScript heap out of memory

If you encounter the following error:

```
FATAL ERROR: Ineffective mark-compacts near heap limit Allocation failed -
JavaScript heap out of memory
```

you may need to export `NODE_OPTIONS=--max_old_space_size=4096` and build again.
