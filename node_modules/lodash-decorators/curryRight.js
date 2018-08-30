"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var curryRight = require("lodash/curryRight");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createInstanceDecorator(new factory_1.DecoratorConfig(curryRight, new applicators_1.PreValueApplicator(), { bound: true }));
/**
 * This method is like _.curry except that arguments are applied to func in the manner of _.partialRight instead of _.partial.
 * The arity of func may be specified if func.length is not sufficient.
 * The original function is bound to the instance. If the method is needed to call in a different context use `CurryAll`.
 *
 * The _.curryRight.placeholder value, which defaults to _ in monolithic builds, may be used as a placeholder for provided arguments.
 *
 * Note: This method doesn't set the "length" property of curried functions.
 * @param {number} [arity] The arity of func.
 * @example
 *
 * class MyClass {
 *   multiplier = 2;
 *
 *   @CurryRight()
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
function CurryRight(arity) {
    return decorator(arity);
}
exports.CurryRight = CurryRight;
exports.curryRight = CurryRight;
exports.default = decorator;
//# sourceMappingURL=curryRight.js.map