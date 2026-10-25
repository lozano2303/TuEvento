import i18n from "../../src/i18n";
import { mapErrorMessage, parseApiError, parseValidationErrors } from "../../src/utils/errorMessages";

describe("mapErrorMessage", () => {
  beforeAll(() => i18n.changeLanguage("es"));

  it("traduce un mensaje conocido del backend", () => {
    expect(mapErrorMessage("Invalid email or password")).toBe(i18n.t("mobileErrors.invalidCredentials"));
  });

  it("reconoce el mensaje dentro de un texto más largo", () => {
    expect(mapErrorMessage("Error: Account is locked until 10:30")).toBe(i18n.t("mobileErrors.locked"));
  });

  it("devuelve el mensaje original si no lo conoce y un genérico si está vacío", () => {
    expect(mapErrorMessage("Algo raro")).toBe("Algo raro");
    expect(mapErrorMessage("")).toBe(i18n.t("mobileErrors.unexpected"));
  });
});

describe("parseValidationErrors", () => {
  it("convierte los errores por campo del backend", () => {
    const errors = parseValidationErrors(
      "email: must not be blank, password: size must be between 8 and 100",
    );
    expect(errors).toEqual({
      email: i18n.t("mobileErrors.fields.emailRequired"),
      password: i18n.t("mobileErrors.fields.passwordSize"),
    });
  });

  it("devuelve null si no es un error por campos", () => {
    expect(parseValidationErrors("Invalid email or password")).toBeNull();
  });
});

describe("parseApiError", () => {
  it("traduce el mensaje de una respuesta fallida", async () => {
    const response = { json: async () => ({ success: false, message: "User not found" }) };
    await expect(parseApiError(response)).resolves.toBe(i18n.t("mobileErrors.userNotFound"));
  });

  it("si la respuesta no es JSON devuelve el error de conexión", async () => {
    const response = {
      json: async () => {
        throw new SyntaxError("bad json");
      },
    };
    await expect(parseApiError(response)).resolves.toBe(i18n.t("mobileErrors.connection"));
  });
});
