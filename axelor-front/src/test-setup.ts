import "@testing-library/jest-dom/extend-expect";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";
import setupMock from "./services/client/mock/setup";

setupMock();

afterEach(() => {
  cleanup();
});
