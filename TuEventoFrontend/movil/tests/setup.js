// AsyncStorage es nativo: en Jest se usa el mock en memoria que trae el paquete
jest.mock("@react-native-async-storage/async-storage", () =>
  require("@react-native-async-storage/async-storage/jest/async-storage-mock"),
);
