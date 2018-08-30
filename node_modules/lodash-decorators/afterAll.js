"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var after = require("lodash/after");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(after, new applicators_1.PostValueApplicator(), { setter: true }));
/**
 * The opposite of Before. This method creates a function that invokes once it's called n or more times.
 * This spans across all instances of the class instead of the instance.
 * @param {number} n The number of calls before the function is invoked.
 * @example
 *
 * class MyClass {
 *   @AfterAll(2)
 *   fn() {
 *     return 10;
 *   }
 * }
 *
 * const myClass = new MyClass();
 * const myClass2 = new MyClass();
 *
 * myClass.fn(); // => undefined
 * myClass.fn(); // => 10
 *
 * myClass2.fn(); // => 10
 * myClass2.fn(); // => 10
 */
function AfterAll(n) {
    return decorator(n);
}
exports.AfterAll = AfterAll;
exports.afterAll = AfterAll;
exports.default = decorator;
//# sourceMappingURL=afterAll.js.map