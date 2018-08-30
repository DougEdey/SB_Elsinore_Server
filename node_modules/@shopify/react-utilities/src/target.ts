export const isServer = (typeof window === 'undefined' || typeof document === 'undefined');
export const isClient = !isServer;
