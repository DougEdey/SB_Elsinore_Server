"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var isFunction = require("lodash/isFunction");
var common_1 = require("./common");
var utils_1 = require("../utils");
var InternalDecoratorFactory = (function () {
    function InternalDecoratorFactory() {
    }
    InternalDecoratorFactory.prototype.createDecorator = function (config) {
        var _this = this;
        var applicator = config.applicator;
        return function () {
            var args = [];
            for (var _i = 0; _i < arguments.length; _i++) {
                args[_i] = arguments[_i];
            }
            return function (target, name, _descriptor) {
                var descriptor = _this._resolveDescriptor(target, name, _descriptor);
                var value = descriptor.value, get = descriptor.get, set = descriptor.set;
                // If this decorator is being applied after an instance decorator we simply ignore it
                // as we can't apply it correctly.
                if (!common_1.InstanceChainMap.has([target, name])) {
                    if (isFunction(value)) {
                        descriptor.value = utils_1.copyMetadata(applicator.apply({ config: config, target: target, value: value, args: args }), value);
                    }
                    else if (isFunction(get) && config.getter) {
                        descriptor.get = utils_1.copyMetadata(applicator.apply({ config: config, target: target, value: get, args: args }), get);
                    }
                    else if (isFunction(set) && config.setter) {
                        descriptor.set = utils_1.copyMetadata(applicator.apply({ config: config, target: target, value: set, args: args }), get);
                    }
                }
                return descriptor;
            };
        };
    };
    InternalDecoratorFactory.prototype.createInstanceDecorator = function (config) {
        var _this = this;
        var applicator = config.applicator, bound = config.bound;
        return function () {
            var args = [];
            for (var _i = 0; _i < arguments.length; _i++) {
                args[_i] = arguments[_i];
            }
            return function (target, name, _descriptor) {
                var descriptor = _this._resolveDescriptor(target, name, _descriptor);
                var value = descriptor.value, writable = descriptor.writable, enumerable = descriptor.enumerable, configurable = descriptor.configurable, get = descriptor.get, set = descriptor.set;
                var isFirstInstance = !common_1.InstanceChainMap.has([target, name]);
                var chainData = common_1.InstanceChainMap.get([target, name]) || { fns: [], properties: [] };
                var isGetter = isFirstInstance && isFunction(get);
                var isSetter = isFirstInstance && isFunction(set);
                var isMethod = isFirstInstance && isFunction(value);
                var isProperty = isFirstInstance && !isGetter && !isSetter && !isMethod;
                chainData.properties.push(name);
                chainData.fns.push(function (fn, instance, context) {
                    if (!_this._isApplicable(context, config)) {
                        return fn;
                    }
                    if (bound) {
                        fn = utils_1.bind(fn, instance);
                    }
                    return utils_1.copyMetadata(applicator.apply({ args: args, target: target, instance: instance, value: fn, config: config }), fn);
                });
                common_1.InstanceChainMap.set([target, name], chainData);
                if (!isFirstInstance) {
                    return descriptor;
                }
                chainData.isSetter = isSetter;
                chainData.isGetter = isGetter;
                chainData.isMethod = isMethod;
                chainData.isProperty = isProperty;
                var applyChain = function (fn, context, instance) {
                    return chainData.fns.reduce(function (result, next) { return next(result, instance, context); }, fn);
                };
                var applyDecorator = function (instance) {
                    var getter = get || undefined;
                    var setter = set || undefined;
                    if (isGetter || isSetter) {
                        // If we have a getter apply the decorators to the getter and assign it to the instance.
                        if (isGetter) {
                            getter = applyChain(get, { value: get, getter: true }, instance);
                        }
                        if (isSetter) {
                            setter = applyChain(set, { value: set, setter: true }, instance);
                        }
                        Object.defineProperty(instance, name, {
                            enumerable: enumerable,
                            configurable: configurable,
                            get: getter,
                            set: setter
                        });
                    }
                    else if (isMethod || isProperty) {
                        var newFn = isMethod
                            ? applyChain(value, { value: value, method: true }, instance)
                            : applyChain(value, { value: value, property: true }, instance);
                        Object.defineProperty(instance, name, {
                            writable: writable,
                            enumerable: enumerable,
                            configurable: configurable,
                            value: newFn
                        });
                    }
                };
                if (isMethod || isProperty) {
                    delete descriptor.value;
                    delete descriptor.writable;
                }
                descriptor.get = function () {
                    applyDecorator(this);
                    var descriptor = Object.getOwnPropertyDescriptor(this, name);
                    if (descriptor.get) {
                        return descriptor.get.call(this);
                    }
                    return descriptor.value;
                };
                descriptor.set = function (value) {
                    applyDecorator(this);
                    var descriptor = Object.getOwnPropertyDescriptor(this, name);
                    if (descriptor.set) {
                        descriptor.set.call(this, value);
                    }
                    else if (isProperty || isMethod) {
                        this[name] = value;
                    }
                };
                return descriptor;
            };
        };
    };
    InternalDecoratorFactory.prototype._isApplicable = function (context, config) {
        return !Boolean(context.getter && !config.getter
            || context.setter && !config.setter
            || context.method && !config.method
            || context.property && !config.property);
    };
    InternalDecoratorFactory.prototype._resolveDescriptor = function (target, name, descriptor) {
        if (descriptor) {
            return descriptor;
        }
        return Object.getOwnPropertyDescriptor(target, name) || {};
    };
    return InternalDecoratorFactory;
}());
exports.InternalDecoratorFactory = InternalDecoratorFactory;
exports.DecoratorFactory = new InternalDecoratorFactory();
//# sourceMappingURL=DecoratorFactory.js.map