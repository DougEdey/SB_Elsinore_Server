"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var curry = require("lodash/curry");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createInstanceDecorator(new factory_1.DecoratorConfig(curry, new applicators_1.PreValueApplicator(), { bound: true }));
/**
 * Creates a function that accepts arguments of func and either invokes func returning its result, if at least arity number of arguments have been provided, or returns a function that accepts the remaining func arguments, and so on.
 * The arity of func may be specified if func.length is not sufficient.
 * The original function is bound to the instance. If the method is needed to call in a different context use `CurryAll`.
 *
 * The _.curry.placeholder value, which defaults to _ in monolithic builds, may be used as a placeholder for provided arguments.
 *
 * Note: This method doesn't set the "length" property of curried functions.
 * @param {number} [arity] The arity of func.
 * @example
 *
 * class MyClass {
 *   multiplier = 2;
 *
 *   @Curry()
 *   add(a, b) {
 *     return (a + b) * this.multiplier;
 *   }
 * }
 *
 * const myClass = new MyClass();
 *
 * const add5 = myClass.add(5);
 *
 * add5AndMultiply(10); // => 30
 */
function Curry(arity) {
    return decorator(arity);
}
exports.Curry = Curry;
exports.curry = Curry;
exports.default = decorator;
//# sourceMappingURL=curry.js.map