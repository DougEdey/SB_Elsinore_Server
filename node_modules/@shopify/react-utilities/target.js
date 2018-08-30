"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.isServer = (typeof window === 'undefined' || typeof document === 'undefined');
exports.isClient = !exports.isServer;
