"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var before = require("lodash/before");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createInstanceDecorator(new factory_1.DecoratorConfig(before, new applicators_1.PostValueApplicator(), { setter: true }));
/**
 * Creates a function that invokes func, with the this binding and arguments of the created function, while it's called less than n times.
 * Subsequent calls to the created function return the result of the last func invocation.
 * @param {number} n The number of calls at whichc func is no longer invoked.
 * @example
 *
 * let calls = 0;
 *
 * class MyClass {
 *   @Before(3)
 *   fn() {
 *     calls++;
 *   }
 * }
 *
 * const myClass = new MyClass();
 *
 * myClass.fn();
 * myClass.fn();
 * myClass.fn();
 * myClass.fn();
 *
 * calls === 2; // => true
 */
function Before(n) {
    return decorator(n);
}
exports.Before = Before;
exports.before = Before;
exports.default = decorator;
//# sourceMappingURL=before.js.map