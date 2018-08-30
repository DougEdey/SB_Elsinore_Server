"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var memoize = require("lodash/memoize");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createInstanceDecorator(new factory_1.DecoratorConfig(memoize, new applicators_1.MemoizeApplicator()));
/**
 * Creates a function that memoizes the result of func. If resolver is provided,
 * it determines the cache key for storing the result based on the arguments provided to the memoized function.
 * By default, the first argument provided to the memoized function is used as the map cache key.
 * The func is invoked with the this binding of the memoized function.
 *
 * You can use a Function or a string that references a method on the class as the resolver.
 * You can also use a configuration object that lets provide a prexisting cache or specify
 * the map type to use.
 *
 * @example
 *
 * class MyClass {
 *   @Memoize({ type: WeakMap })
 *   getName(item) {
 *     return item.name;
 *   }
 *
 *   @Memoize('getName')
 *   getLastName(item) {
 *     return item.lastName;
 *   }
 * }
 */
function Memoize(resolver) {
    return decorator(resolver);
}
exports.Memoize = Memoize;
exports.memoize = Memoize;
exports.default = decorator;
//# sourceMappingURL=memoize.js.map