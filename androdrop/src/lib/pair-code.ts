import { customAlphabet } from "nanoid";

// Excludes visually ambiguous characters (0/O, 1/I/L) so codes are easy to
// read off a screen and type by hand.
const ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

const generate = customAlphabet(ALPHABET, 6);

export function generatePairCode(): string {
  return generate();
}

export function normalizePairCode(code: string): string {
  return code.trim().toUpperCase().replace(/[^A-Z0-9]/g, "");
}
