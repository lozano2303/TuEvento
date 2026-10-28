import { isTargetValidated, isValidationFor } from "../../src/utils/qrValidation";

describe("isValidationFor", () => {
  const orderMsg = { type: "QR_VALIDATED", scope: "ORDER", targetId: 300, ticketIds: [11, 12] };
  const ticketMsg = { type: "QR_VALIDATED", scope: "TICKET", targetId: 11, ticketIds: [11] };

  it("reconoce la validación de la orden mostrada", () => {
    expect(isValidationFor("order", 300, orderMsg)).toBe(true);
    expect(isValidationFor("order", "300", orderMsg)).toBe(true);
    expect(isValidationFor("order", 301, orderMsg)).toBe(false);
  });

  it("reconoce la entrada mostrada aunque se validara con el QR de la orden", () => {
    expect(isValidationFor("ticket", 11, ticketMsg)).toBe(true);
    expect(isValidationFor("ticket", 12, orderMsg)).toBe(true);
    expect(isValidationFor("ticket", 13, orderMsg)).toBe(false);
  });

  it("ignora otros mensajes", () => {
    expect(isValidationFor("order", 300, { ...orderMsg, type: "OTHER" })).toBe(false);
    expect(isValidationFor("ticket", 11, null)).toBe(false);
  });
});

describe("isTargetValidated", () => {
  it("una entrada está validada cuando quedó USED", () => {
    expect(isTargetValidated("ticket", 11, [{ ticketId: 11, status: "USED" }])).toBe(true);
    expect(isTargetValidated("ticket", 11, [{ ticketId: 11, status: "PAID" }])).toBe(false);
    expect(isTargetValidated("ticket", 11, [])).toBe(false);
  });

  it("una orden está validada cuando no le quedan entradas por validar", () => {
    expect(isTargetValidated("order", 300, [{ status: "USED" }, { status: "REFUNDED" }])).toBe(true);
    expect(isTargetValidated("order", 300, [{ status: "USED" }, { status: "PAID" }])).toBe(false);
    expect(isTargetValidated("order", 300, [{ status: "CANCELLED" }])).toBe(false);
    expect(isTargetValidated("order", 300, null)).toBe(false);
  });
});
