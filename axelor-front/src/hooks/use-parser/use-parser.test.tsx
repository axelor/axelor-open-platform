import { render } from "@testing-library/react";
import { useTemplate } from "./use-parser";

describe("use-parser tests", () => {
  function TestComponent({ template, ...props }: any) {
    const Component = useTemplate(template);
    return <Component {...props} />;
  }

  it("should parse and render component properly", async () => {
    const context = {
      amount: 1000,
      taxAmount: 50,
      totalAmount: 1050,
      currency: "$",
    };

    const { container } = render(
      <TestComponent
        template={`
        <dl class="dl-horizontal">
          <dt x-translate>Amount</dt>
          <dd id="amount">{{amount}} {{currency}}</dd>
          <dt x-translate>Tax amount</dt>
          <dd id="taxAmount">{{taxAmount}} {{currency}}</dd>
          <dt class="order-subtotal-total" x-translate>Total amount</dt>
          <dd id="totalAmount" class="order-subtotal-total">{{totalAmount}} {{currency}}</dd>
        </dl>
        `}
        context={context}
      />
    );

    const total = container.querySelector("#amount");
    const taxAmount = container.querySelector("#taxAmount");
    const totalAmount = container.querySelector("#totalAmount");

    expect(total).toBeDefined();
    expect(total?.textContent).toEqual("1000 $");

    expect(taxAmount).toBeDefined();
    expect(taxAmount?.textContent).toEqual("50 $");

    expect(totalAmount).toBeDefined();
    expect(totalAmount?.textContent).toEqual("1050 $");
  });
});
