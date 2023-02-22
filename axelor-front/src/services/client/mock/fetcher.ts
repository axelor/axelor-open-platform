type Fetch = typeof fetch;

type Handler = (...params: string[]) => Fetch;

const bodyJson = async (init?: RequestInit) => {
  if (typeof init?.body === "string") {
    return JSON.parse(init.body);
  }
  return Promise.reject();
};

const simpleName = (model: string) => model.replaceAll(/.*?\./g, "");

const respond = (module: { default: any }): Response => {
  const { default: data } = module;
  return respondData(data);
};

const respondData = (data: any): Response => {
  return new Response(JSON.stringify(data));
};

const handleLogin: Handler = () => async (input, init) => {
  const body = await bodyJson(init);
  if (body.username && body.username === body.password) {
    return new Response(JSON.stringify(body));
  }
  return Promise.reject();
};

const handleLogout: Handler = () => async () => {
  return new Response(null, { status: 401 });
};

const handleInfo: Handler = () => async () => {
  const module = await import("./data/app/info.json");
  return respond(module);
};

const handleMenuAll: Handler = () => async () => {
  const module = await import("./data/app/menu.json");
  return respond(module);
};

const handleMenuQuick: Handler = () => async () => {
  const module = await import("./data/app/menu-quick.json");
  return respond(module);
};

const handleFilters: Handler = () => async (input, init) => {
  const json = await bodyJson(init);
  const {
    context: { filterView },
  } = json;
  const module = await import(`./data/filters/${filterView}.json`);
  return respond(module);
};

const handleFields: Handler = (model) => async (input, init) => {
  const name = simpleName(model);
  const module = await import(`./data/fields/${name}.json`);
  return respond(module);
};

const handleView: Handler = () => async (input, init) => {
  const json = await bodyJson(init);
  const {
    model,
    data: { type },
  } = json;
  const name = simpleName(model);
  const module = await import(`./data/views/${type}/${name}.json`);
  return respond(module);
};

const handleActionByName: Handler = (name) => async () => {
  const module = await import(`./data/actions/${name}.json`);
  return respond(module);
};

const handleActionByBody: Handler = () => async (input, init) => {
  const json = await bodyJson(init);
  const { action } = json;
  const module = await import(`./data/actions/${action}.json`);
  return respond(module);
};

const handleFetch: Handler = (model, recordId) => async (input, init) => {
  const id = parseInt(recordId);
  const resp = await handleSearch(model)(`/ws/rest/${model}/search`);
  const { data } = await resp.json();

  const record = data.find((x: any) => x.id === id);
  const body = {
    status: 0,
    data: [record],
  };
  return respondData(body);
};

const handleSearch: Handler = (model) => async (input, init) => {
  const name = simpleName(model);
  const module = await import(`./data/records/${name}.json`);
  return respond(module);
};

const handleExport: Handler = (model) => async (input, init) => {
  const res = {
    status: 0,
    data: {
      exportSize: 3,
      fileName: "3881756375896635984.csv",
    },
  };
  return respondData(res);
};

const handleSave: Handler = (model) => async (input, init) => {
  const { data } = await bodyJson(init);
  const res = {
    status: 0,
    data: [
      {
        id: 10000,
        version: 0,
        ...data,
      },
    ],
  };

  return respondData(res);
};

const handleDelete: Handler = (model) => async (input, init) => {
  const { records } = await bodyJson(init);
  const res = {
    status: 0,
    data: records,
  };
  return respondData(res);
};

const handleCopy: Handler = (model, id) => async (input, init) => {
  const fn = handleFetch(model, id);
  const resp = await fn(input, init);
  const {
    data: [record],
  } = await resp.json();

  const { id: _id, version, notes, addresses, ...rec } = record;
  const res = {
    status: 0,
    data: [rec],
  };

  return respondData(res);
};

const handlers: Record<string, Handler> = {
  "/callback": handleLogin,
  "/logout": handleLogout,
  "/ws/app/info": handleInfo,
  "/ws/action/menu/all": handleMenuAll,
  "/ws/action/menu/quick": handleMenuQuick,
  "/ws/action/com.axelor.meta.web.MetaFilterController:findFilters":
    handleFilters,
  "/ws/meta/fields/(?<model>[^/]+)": handleFields,
  "/ws/meta/view": handleView,
  "/ws/action/(?<action>[^/]+)": handleActionByName,
  "/ws/action": handleActionByBody,
  "/ws/rest/(?<model>[^/]+)/(?<id>[\\d]+)/fetch": handleFetch,
  "/ws/rest/(?<model>[^/]+)/search": handleSearch,
  "/ws/rest/(?<model>[^/]+)/export": handleExport,
  "/ws/rest/(?<model>[^/]+)": handleSave,
  "/ws/rest/(?<model>[^/]+)/removeAll": handleDelete,
  "/ws/rest/(?<model>[^/]+)/(?<id>[\\d]+)/copy": handleCopy,
};

const findHandler = (input: string): Fetch => {
  for (const [key, handler] of Object.entries(handlers)) {
    const s = `^.${key}$`;
    const re = new RegExp(s);
    const res = re.exec(input);
    if (res) {
      const params = res.slice(1);
      return handler(...params);
    }
  }
  return () => Promise.reject();
};

export const fetcher: Fetch = async (input, init) => {
  if (typeof input === "string") {
    return await findHandler(input)(input, init);
  }
  return Promise.reject();
};
