import type { Config } from "jest";

const config: Config = {
  testEnvironment: "jsdom",
  transform: {
    "^.+\\.tsx?$": [
      "ts-jest",
      {
        tsconfig: {
          jsx: "react-jsx",
        },
      },
    ],
    // Transform ESM packages that ship without CommonJS builds
    "^.+\\.js$": [
      "ts-jest",
      {
        tsconfig: {
          jsx: "react-jsx",
          allowJs: true,
        },
      },
    ],
  },
  // Do NOT ignore next-intl and its ESM deps when transforming node_modules
  transformIgnorePatterns: [
    "/node_modules/(?!(next-intl|use-intl|@formatjs|intl-messageformat|@internationalized)/)",
  ],
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
  },
  setupFilesAfterEnv: ["@testing-library/jest-dom"],
  testMatch: ["**/__tests__/**/*.{ts,tsx}"],
  modulePathIgnorePatterns: ["<rootDir>/.next/"],
};

export default config;
