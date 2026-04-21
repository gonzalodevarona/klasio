"use client";

interface PasswordPolicyCheckerProps {
  password: string;
}

interface Rule {
  key: string;
  label: string;
  test: (password: string) => boolean;
}

export const PASSWORD_RULES: Rule[] = [
  {
    key: "minLength",
    label: "At least 8 characters",
    test: (p) => p.length >= 8,
  },
  {
    key: "uppercase",
    label: "At least 1 uppercase letter",
    test: (p) => /[A-Z]/.test(p),
  },
  {
    key: "digit",
    label: "At least 1 digit",
    test: (p) => /\d/.test(p),
  },
  {
    key: "special",
    label: "At least 1 special character",
    test: (p) => /[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(p),
  },
];

/**
 * Returns true if the password satisfies all policy rules.
 */
export function validatePassword(password: string): boolean {
  return PASSWORD_RULES.every((rule) => rule.test(password));
}

export default function PasswordPolicyChecker({ password }: PasswordPolicyCheckerProps) {
  return (
    <ul className="mt-2 space-y-1">
      {PASSWORD_RULES.map((rule) => {
        const satisfied = password.length > 0 && rule.test(password);
        return (
          <li
            key={rule.key}
            className={`flex items-center text-xs ${
              password.length === 0
                ? "text-gray-400"
                : satisfied
                  ? "text-green-600"
                  : "text-red-600"
            }`}
          >
            <span className="mr-1.5">{satisfied ? "\u2713" : "\u2717"}</span>
            {rule.label}
          </li>
        );
      })}
    </ul>
  );
}
