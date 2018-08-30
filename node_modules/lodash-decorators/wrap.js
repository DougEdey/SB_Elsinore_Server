"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var wrap = require("lodash/wrap");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(wrap, new applicators_1.WrapApplicator()));
function Wrap(fnToWrap) {
    return decorator(fnToWrap);
}
exports.Wrap = Wrap;
exports.wrap = Wrap;
exports.default = decorator;
//# sourceMappingURL=wrap.js.map