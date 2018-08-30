"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var tslib_1 = require("tslib");
var partial = require("lodash/partial");
var Applicator_1 = require("./Applicator");
var PartialedApplicator = (function (_super) {
    tslib_1.__extends(PartialedApplicator, _super);
    function PartialedApplicator() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    PartialedApplicator.prototype.apply = function (_a) {
        var execute = _a.config.execute, value = _a.value, args = _a.args;
        return partial.apply(void 0, [execute, value].concat(args));
    };
    return PartialedApplicator;
}(Applicator_1.Applicator));
exports.PartialedApplicator = PartialedApplicator;
//# sourceMappingURL=PartialedApplicator.js.map