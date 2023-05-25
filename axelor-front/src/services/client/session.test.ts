import { session } from "./session";

describe("session tests", async () => {
  it("should load app info", async () => {
    const res = await session.init();
    expect(res).toBeDefined();
    expect(res.app).toBeDefined();
    expect(res.app?.author).toBe("Axelor");
  });

  it("should login", async () => {
    const res = await session.login({
      username: "admin",
      password: "admin",
    });
    expect(res).toBeDefined();
    expect(res.app).toBeDefined();
    expect(res.app?.author).toBe("Axelor");
  });

  it("should logout", async () => {
    const { status } = await session.logout();
    expect(status).toBeDefined();
    expect(status).toBe(204);
  });
});
