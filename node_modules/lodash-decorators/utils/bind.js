"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var copyMetaData_1 = require("./copyMetaData");
/**
 * Binds a function and copies metadata.
 * @private
 * @export
 * @param {Function} fn
 * @param {*} context
 * @returns {Function}
 */
function bind(fn, context) {
    return copyMetaData_1.copyMetadata(fn.bind(context), fn);
}
exports.bind = bind;
//# sourceMappingURL=bind.js.map