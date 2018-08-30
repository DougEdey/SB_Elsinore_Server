"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var assignAll_1 = require("./assignAll");
var PROPERTY_EXCLUDES = [
    'length',
    'name',
    'arguments',
    'called',
    'prototype'
];
/**
 * Wraps a constructor in a wrapper function and copies all static properties
 * and methods to the new constructor.
 * @export
 * @param {Function} Ctor
 * @param {(Ctor: Function, ...args: any[]) => any} wrapper
 * @returns {Function}
 */
function wrapConstructor(Ctor, wrapper) {
    function ConstructorWrapper() {
        var args = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            args[_i] = arguments[_i];
        }
        return wrapper.call.apply(wrapper, [this, Ctor].concat(args));
    }
    ConstructorWrapper.prototype = Ctor.prototype;
    Object.defineProperty(ConstructorWrapper, 'name', {
        // These values should coincide with the default descriptor values for `name`.
        configurable: true,
        enumerable: false,
        value: Ctor.name,
        writable: false
    });
    return assignAll_1.assignAll(ConstructorWrapper, Ctor, PROPERTY_EXCLUDES);
}
exports.wrapConstructor = wrapConstructor;
//# sourceMappingURL=wrapConstructor.js.map