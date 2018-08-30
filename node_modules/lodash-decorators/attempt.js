"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var attempt = require("lodash/attempt");
var partial = require("lodash/partial");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var attemptFn = function (fn) { return partial(attempt, fn); };
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(attemptFn, new applicators_1.PreValueApplicator()));
/**
 * Attempts to invoke func, returning either the result or the caught error object. Any additional arguments are provided to func when it's invoked.
 * @param {...*} [args] The arguments to invoke func with.
 * @example
 *
 * class MyClass {
 *   @Attempt()
 *   fn(value) {
 *     if (typeof value === 'number') {
 *       return value
 *     }
 *
 *     throw new Error();
 *   }
 * }
 *
 * const myClass = new MyClass();
 *
 * myClass.fn(10); // => 10;
 * myClass.fn(null); // => Error
 */
function Attempt() {
    var partials = [];
    for (var _i = 0; _i < arguments.length; _i++) {
        partials[_i] = arguments[_i];
    }
    return decorator.apply(void 0, partials);
}
exports.Attempt = Attempt;
exports.attempt = Attempt;
exports.default = decorator;
//# sourceMappingURL=attempt.js.map