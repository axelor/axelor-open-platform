import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { vi } from "vitest";

import * as parser from "./parser";

const context = {
  css: "some-class",
  ctor: "constructor",
  items: [
    {
      id: 1,
      title: "Hello",
    },
    {
      id: 2,
      title: "World",
    },
  ],
  props: {
    style: {
      width: 100,
      color: "red",
    },
  },
};

describe("parser", () => {
  it("should parse react template", async () => {
    const template = `
    <ul className={css} style={{width: 100}}>
      {items.map(x => <li key={x.id}>{x.title}</li>)}
    </ul>
    `;
    const res = parser.parse(template);

    expect(res).toBeInstanceOf(Function);
    expect(res.source).toEqual(template);
    expect(res.code).toBeTruthy();

    render(<div data-testid="template-test">{res(context)}</div>);

    const elem = await waitFor(() => screen.getByTestId("template-test"));

    expect(elem).toBeInTheDocument();
    expect(elem.querySelectorAll("ul.some-class")).toHaveLength(1);
    expect(elem.querySelectorAll("ul.some-class > li")).toHaveLength(2);
  });

  it("should parse js expression", () => {
    const expression = `
    const names = items.map(x => x.title);
    const first = names[0];
    const last = (function (data, n) { return data[n]; })(names, 1);
    first;
    ;
    `;
    const res = parser.parse(expression);
    expect(res).toBeInstanceOf(Function);
    expect(res.source).toEqual(expression);
    expect(res.code).toBeTruthy();

    const value = res(context);
    expect(value).toBeTruthy();
    expect(value).toEqual(context.items[0].title);
  });

  it("should parse empty js expression", () => {
    const expression = ``;
    const res = parser.parse(expression);
    expect(res.code).toBeTruthy();
    const value = res(context);
    expect(value).toBeUndefined();
  });

  it("should not allow access to function properties", () => {
    const cases = [
      `"".sub.constructor("console.log(1)")()`,
      `items[0].title.sub.constructor('console.log(1)')()`,
      `(function (x) { return x.constructor;})("".sub)('console.log(1)')()`,
      `(function (x) { return x['constructor'];})("".sub)('console.log(1)')()`,
      `(function (x, v) { return x[v] | x[ctor];})("".sub, 'constructor')('console.log(1)')()`,
      `(function (x) { return x[\`constructor\`];})("".sub)('console.log(1)')()`,
      `(function (x, v) { return x[\`construc\${v}\`];})("".sub, 'tor')('console.log(1)')()`,
    ];

    for (let expr of cases) {
      let fn = parser.parse(expr);
      expect(() => fn(context)).toThrowError();
    }
  });

  it("should not allow access to globals", () => {
    const cases = [
      `console.log(1)`,
      `alert(1)`,
      `const x = new XMLHttpRequest()`,
      `window.location.href = '/'`,
    ];

    for (let expr of cases) {
      let fn = parser.parse(expr);
      expect(() => fn(context)).toThrowError();
    }
  });

  it("should not allow access to `eval` and `Function`", () => {
    const cases = [
      `eval('console.log(1)')`,
      `new Function('x', 'console.log(x)')(1)`,
    ];
    for (let expr of cases) {
      expect(() => parser.parse(expr)).toThrowError();
    }
  });

  it("should not allow access to `eval` and `Function`", () => {
    const cases = [
      `eval('console.log(1)')`,
      `new Function('x', 'console.log(x)')(1)`,
    ];
    for (let expr of cases) {
      expect(() => parser.parse(expr)).toThrowError();
    }
  });

  it("should not allow access to `React.createElement`", () => {
    const cases = [
      `React.createElement('div')`,
      `React['createElement']('div')`,
      `React[\`createElement\`]('div')`,
      `const x = 'createElement';
       const r = React;
       r[x]('div')`,
      `const x = 'Element';
       const r = React;
       r[\`create\${x}\`]('div')`,
    ];
    for (let expr of cases) {
      expect(() => parser.parse(expr)(context)).toThrowError();
    }
  });

  it("should support optional chaining (elvis operator)", () => {
    expect(parser.parse("props.style.width")(context)).toEqual(100);
    expect(() => parser.parse("props.data.id")(context)).toThrowError();
    expect(parser.parse("props?.data?.id")(context)).toBeUndefined();
  });

  it("should not allow javascript: URLs", () => {
    const ctx = { x: `javascript: alert(1)`, y: "some?value=1" };
    expect(() =>
      parser.parse(`<a href="javaScript: alert(1)">Test</a>`)(ctx)
    ).toThrowError();
    expect(() =>
      parser.parse(`<a href="\\t\\x00javascript: alert(1)">Test</a>`)(ctx)
    ).toThrowError();
    expect(() =>
      parser.parse(`<a href="some?value=1">Test</a>`)(ctx)
    ).not.toThrowError();
    expect(() => parser.parse(`<a href={x}>Test</a>`)(ctx)).toThrowError();
    expect(() =>
      parser.parse(`<a href={y}>Test</a> | <a href=""></a>`)(ctx)
    ).not.toThrowError();
  });

  it("should handle expressions with class declaration", () => {
    const fn = parser.parse(`
    class Hello {
      constructor(msg) { this.msg = msg; }
      say() { return this.msg; }
    }
    const hello = new Hello('Hello!!!');
    hello.say();
    `);
    const res = fn({});
    expect(res).toEqual("Hello!!!");
  });

  it("should trap access to dom elements", async () => {
    const ctx = {
      value: "",
      inputAttrs: {
        value: undefined,
        appendChild: undefined,
      },
      targetAttrs: {
        value: undefined,
        appendChild: undefined,
      },
    };

    const fn = parser.parse(`
    const input = React.createRef();
    let button;

    const props = {
      onClick: (e) => {
        targetAttrs.appendChild = e.target.appendChild === undefined;
      },
      ref: (element => button = element)
    };

    function handleChange(e) {
      value = e.target.value;
      inputAttrs.value = input.current.value;
      inputAttrs.appendChild = input.current.appendChild === undefined;
      targetAttrs.value = e.target.value;
      targetAttrs.appendChild = e.target.appendChild === undefined;
    }
    <div>
      <input data-testid="input" type="text" ref={input} value={value} onChange={handleChange} />
      <button data-testid="button" type="button" {...props} {...{onChange: e => {}}}>Test</button>
    </div>
    `);

    render(<div>{fn(ctx)}</div>);

    const input = await waitFor(() => screen.getByTestId("input"));
    const button = await waitFor(() => screen.getByTestId("button"));

    expect(input).toBeDefined();
    expect(button).toBeDefined();

    fireEvent.change(input, {
      target: { value: "test", appendChild: () => {} },
    });

    fireEvent.click(button, {
      target: { appendChild: () => {} },
    });

    expect(ctx.value).toEqual("test");
    expect(ctx.value).toEqual(ctx.inputAttrs.value);
    expect(ctx.value).toEqual(ctx.targetAttrs.value);
    expect(ctx.inputAttrs.appendChild).toBeTruthy();
    expect(ctx.targetAttrs.appendChild).toBeTruthy();
  });

  it("should sanitize `dangerouslySetInnerHTML`", async () => {
    const ctx = {
      value: `
      <div>
      <a href="javascript: console.log(1)">Test</a>
      <button onclick="javascript: alert(1)">Test</button>
      <img src="some.png" onerror="javascript: console.log(1)" alt="some.png">
      <img srcset="some.png 1x, some.png 2x" sizes="(max-width: 320px) 280px">
      </div>`
        .replace(/\n/g, "")
        .replace(/\s*</g, "<"),
    };

    const fn = parser.parse(
      `<div data-testid="inner-html" dangerouslySetInnerHTML={{__html: value}}></div>`
    );
    const Comp = () => fn(ctx);

    render(<Comp />);

    const div = await waitFor(() => screen.getByTestId("inner-html"));
    expect(div).toBeDefined();
    expect(div.innerHTML).toEqual(
      `<div><a>Test</a>Test<img src="some.png" alt="some.png"><img srcset="some.png 1x, some.png 2x" sizes="(max-width: 320px) 280px"></div>`
    );
  });

  it("should handle jsx member expressions", async () => {
    const ctx = {
      ui: {
        String: (props: any) => (
          <input data-testid="input" {...props} type="text" />
        ),
      },
    };
    const fn = parser.parse(`<ui.String />`);
    const Comp = () => fn(ctx);

    render(<Comp />);

    const input = await waitFor(() => screen.getByTestId("input"));
    expect(input).toBeDefined();
  });

  it("should evaluate script in strict mode", () => {
    const fn = parser.parse(`
    function test() {
      return this;
    }
    test();
    `);
    const res = fn(context);
    expect(res).toBeUndefined();
  });

  it('should not allow re-declaring "React"', () => {
    const cases = [`function test(React) {}`, `const React = {}`];
    for (let expr of cases) {
      expect(() => parser.parse(expr)).toThrowError();
    }
  });
});

describe("safe parser", () => {
  it("should forgive property access on `null` or `undefined`", () => {
    const ctx = {
      some: { nested: { value: 100 } },
    };
    const cases = [`some.another.value`, `some.nested.name.toUpperCase()`];
    for (let expr of cases) {
      expect(parser.parseSafe(expr)(ctx)).toBeUndefined();
    }
  });
  it("should not handle expression with optional chaining", () => {
    const ctx = {
      some: { nested: { value: 100 } },
    };
    const cases = [`some?.another.value`, `some.nested?.name.toUpperCase()`];
    for (let expr of cases) {
      expect(() => parser.parseSafe(expr)(ctx)).toThrowError();
    }
  });
  it("should not transform left hand side of assignment expressions", () => {
    const ctx = {
      some: { nested: { value: 100 } },
    };
    const res = parser.parseSafe(`some.nested.value = some.another.value`)(ctx);
    expect(res).toBeUndefined();
    expect(ctx.some.nested.value).toBeUndefined();
  });
});
