"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var assign = require("lodash/assign");
/**
 * Mixins an object into the classes prototype.
 * @export
 * @param {...Object[]} srcs
 * @returns {ClassDecorator}
 * @example
 *
 * const myMixin = {
 *   blorg: () => 'blorg!'
 * }
 *
 * @Mixin(myMixin)
 * class MyClass {}
 *
 * const myClass = new MyClass();
 *
 * myClass.blorg(); // => 'blorg!'
 */
function Mixin() {
    var srcs = [];
    for (var _i = 0; _i < arguments.length; _i++) {
        srcs[_i] = arguments[_i];
    }
    return function (target) {
        assign.apply(void 0, [target.prototype].concat(srcs));
        return target;
    };
}
exports.Mixin = Mixin;
exports.mixin = Mixin;
exports.default = Mixin;
//# sourceMappingURL=mixin.js.map