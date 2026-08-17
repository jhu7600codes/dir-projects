const ADJECTIVES = [
  "Quiet",
  "Clever",
  "Swift",
  "Bright",
  "Calm",
  "Gentle",
  "Golden",
  "Lucky",
  "Nimble",
  "Vivid",
];

const NOUNS = [
  "Falcon",
  "Otter",
  "Comet",
  "Maple",
  "Harbor",
  "Ember",
  "Willow",
  "Delta",
  "Pixel",
  "Ridge",
];

export function generateDeviceName(): string {
  const adjective = ADJECTIVES[Math.floor(Math.random() * ADJECTIVES.length)];
  const noun = NOUNS[Math.floor(Math.random() * NOUNS.length)];
  return `${adjective} ${noun}`;
}
