"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var partialRight = require("lodash/partialRight");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createInstanceDecorator(new factory_1.DecoratorConfig(partialRight, new applicators_1.PartialApplicator(), { property: true, method: false }));
function PartialRight() {
    var partials = [];
    for (var _i = 0; _i < arguments.length; _i++) {
        partials[_i] = arguments[_i];
    }
    return decorator.apply(void 0, partials);
}
exports.PartialRight = PartialRight;
exports.partialRight = PartialRight;
exports.default = decorator;
//# sourceMappingURL=partialRight.js.map