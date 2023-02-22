# Axelor WEB

The next generation web frontend of Axelor.

## Prerequisite

### Engines

- node >= v18.14.0
- yarn >= 3.4.1

## Quickstart

Run following command from terminal to install dependencies:

```bash
yarn install
```

In [.env](.env), update the proxy settings. This will forward requests to that instance :

```conf
# Target host to proxy to
VITE_PROXY_TARGET=http://localhost:8080
VITE_PROXY_CONTEXT=/
```

Then, to start dev server, run following command from terminal:

```bash
yarn dev
```

Wait and the application should start in browser at: http://localhost:5173/

## Build

Run following command to create a directory with a production build of your app :

```
yarn build
```

Production build are available under `dist` directory.

If you encounter the following error:

```
FATAL ERROR: Ineffective mark-compacts near heap limit Allocation failed -
JavaScript heap out of memory
```

you may need to export `NODE_OPTIONS=--max_old_space_size=4096` and build again.
