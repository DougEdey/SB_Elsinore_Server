"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var isFunction = require("lodash/isFunction");
var utils_1 = require("./utils");
var factory_1 = require("./factory");
/**
 * Binds methods of an object to the object itself, overwriting the existing method.
 * @export
 * @param {string[]} [methods=[]]
 * @returns {ClassDecorator}
 * @example
 *
 * @BindAll()
 * class MyClass {
 *   bound() {
 *     return this;
 *   }
 *
 *   unbound() {
 *     return this;
 *   }
 * }
 *
 * const myClass = new MyClass();
 *
 * myClass.bound.call(null); // => MyClass {}
 * myClass.unbound.call(null); // => MyClass {}
 */
function BindAll(methods) {
    if (methods === void 0) { methods = []; }
    return function (target) {
        return utils_1.wrapConstructor(target, function (Ctor) {
            var args = [];
            for (var _i = 1; _i < arguments.length; _i++) {
                args[_i - 1] = arguments[_i];
            }
            bindAllMethods(target, this, methods);
            Ctor.apply(this, args);
        });
    };
}
exports.BindAll = BindAll;
exports.bindAll = BindAll;
function bindAllMethods(target, instance, methods) {
    if (methods === void 0) { methods = []; }
    var proto = target.prototype;
    while (proto && proto !== Object.prototype) {
        for (var _i = 0, _a = Object.getOwnPropertyNames(proto); _i < _a.length; _i++) {
            var key = _a[_i];
            var include = methods.length ? methods.indexOf(key) !== -1 : true;
            var descriptor = Object.getOwnPropertyDescriptor(proto, key);
            if (include && key !== 'constructor' && !instance.hasOwnProperty(key)) {
                // If this property is a getter and it's NOT an instance decorated
                // method, ignore it. Instance decorators are getters until first accessed.
                if (descriptor.get) {
                    var chainData = factory_1.InstanceChainMap.get([proto, key]);
                    if (!chainData || !chainData.isMethod) {
                        continue;
                    }
                }
                var value = instance[key];
                if (isFunction(value)) {
                    Object.defineProperty(instance, key, {
                        configurable: true,
                        enumerable: descriptor.enumerable,
                        value: utils_1.copyMetadata(value.bind(instance), value),
                        writable: descriptor.writable
                    });
                }
            }
        }
        proto = Object.getPrototypeOf(proto);
    }
}
exports.default = BindAll;
//# sourceMappingURL=bindAll.js.map