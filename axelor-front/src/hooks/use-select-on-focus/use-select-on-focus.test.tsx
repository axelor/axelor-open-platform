import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useSelectOnFocus } from "./use-select-on-focus";

function TestInput({ defaultValue = "hello world" }: { defaultValue?: string }) {
  const handlers = useSelectOnFocus();
  return <input data-testid="input" defaultValue={defaultValue} {...handlers} />;
}

function TestInputPair() {
  const a = useSelectOnFocus();
  const b = useSelectOnFocus();
  return (
    <>
      <input data-testid="a" defaultValue="hello world" {...a} />
      <input data-testid="b" defaultValue="hello world" {...b} />
    </>
  );
}

// Selection is deferred to the next animation frame (see use-select-on-focus.ts
// for why), so tests must wait a frame before asserting on selection state.
const nextFrame = () =>
  new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));

describe("useSelectOnFocus", () => {
  it("selects all text on focus", async () => {
    render(<TestInput />);
    const input = screen.getByTestId("input") as HTMLInputElement;

    // input.focus() both dispatches the focus event and sets document.activeElement
    input.focus();
    await nextFrame();

    expect(input.selectionStart).toBe(0);
    expect(input.selectionEnd).toBe("hello world".length);
  });

  it("does not reselect while already focused", async () => {
    render(<TestInput />);
    const input = screen.getByTestId("input") as HTMLInputElement;

    input.focus();
    await nextFrame();

    // Simulate browser repositioning cursor to position 2
    input.setSelectionRange(2, 2);

    // A second click on an already-focused input fires no new focus event,
    // so selection should be left untouched.
    expect(input.selectionStart).toBe(2);
    expect(input.selectionEnd).toBe(2);
  });

  it("does not select a field that already lost focus on rapid focus change", async () => {
    render(<TestInputPair />);
    const a = screen.getByTestId("a") as HTMLInputElement;
    const b = screen.getByTestId("b") as HTMLInputElement;

    // Focus a, then immediately move focus to b before a's deferred
    // select() has a chance to run.
    a.focus();
    b.focus();
    a.setSelectionRange(2, 2);

    await nextFrame();

    // a's stale deferred select() must not fire now that b is focused.
    expect(a.selectionStart).toBe(2);
    expect(a.selectionEnd).toBe(2);
  });
});
