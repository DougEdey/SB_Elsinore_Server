"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var ary = require("lodash/ary");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(ary, new applicators_1.PreValueApplicator()));
/**
 * Creates a function that invokes func, with up to n arguments, ignoring any additional arguments.
 * @param {number} n The arity cap.
 * @example
 *
 * class MyClass {
 *   @Ary(1)
 *   fn(...args) {
 *     return args;
 *   }
 * }
 *
 * const myClass = new MyClass();
 *
 * myClass.fn(1, 2, 3, 4); // => [ 1 ]
 */
function Ary(n) {
    return decorator(n);
}
exports.Ary = Ary;
exports.ary = Ary;
exports.default = decorator;
//# sourceMappingURL=ary.js.map