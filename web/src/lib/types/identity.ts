export type IdentityDocumentType = "CC" | "TI" | "CE" | "PA" | "RC";

export const IDENTITY_DOCUMENT_TYPES: { value: IdentityDocumentType; label: string }[] = [
  { value: "CC", label: "Cédula de Ciudadanía" },
  { value: "TI", label: "Tarjeta de Identidad" },
  { value: "CE", label: "Cédula de Extranjería" },
  { value: "PA", label: "Pasaporte" },
  { value: "RC", label: "Registro Civil" },
];
