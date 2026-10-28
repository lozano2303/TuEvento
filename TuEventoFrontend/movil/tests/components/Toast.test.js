import { render, screen } from "@testing-library/react-native";
import { StyleSheet } from "react-native";
import Toast from "../../src/components/Toast";

// El tema real lee AsyncStorage y la API; para el componente basta con los colores
jest.mock("../../src/context/ThemeContext", () => ({
  useTheme: () => ({ colors: { success: "#10b981", surface: "#111", text: "#fff" } }),
}));

// Sin proveedor de áreas seguras en el test: una barra de estado de 30 px
jest.mock("react-native-safe-area-context", () => ({
  useSafeAreaInsets: () => ({ top: 30, bottom: 0, left: 0, right: 0 }),
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

  it("aparece arriba, bajo la barra de estado, y se anuncia como alerta", async () => {
    await render(<Toast toast={{ key: 2, message: "Agregá una silla", type: "warning" }} onHide={jest.fn()} />);

    const alert = screen.getByRole("alert");
    const style = StyleSheet.flatten(alert.props.style);
    expect(style.top).toBe(30 + 8);
    expect(style.bottom).toBeUndefined();
  });
});
