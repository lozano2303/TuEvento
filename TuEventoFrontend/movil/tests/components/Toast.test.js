import { render, screen } from "@testing-library/react-native";
import Toast from "../../src/components/Toast";

// El tema real lee AsyncStorage y la API; para el componente basta con los colores
jest.mock("../../src/context/ThemeContext", () => ({
  useTheme: () => ({ colors: { success: "#10b981", surface: "#111", text: "#fff" } }),
}));

describe("Toast", () => {
  it("muestra el mensaje recibido", async () => {
    await render(<Toast toast={{ key: 1, message: "Guardado", type: "success" }} onHide={jest.fn()} />);
    expect(screen.getByText("Guardado")).toBeTruthy();
  });

  it("no renderiza nada sin toast", async () => {
    await render(<Toast toast={null} onHide={jest.fn()} />);
    expect(screen.queryByText("Guardado")).toBeNull();
  });
});
