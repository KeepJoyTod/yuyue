export const createId = (prefix: string) => {
  const rand = Math.random().toString(16).slice(2, 10);
  return `${prefix}_${Date.now()}_${rand}`;
};

