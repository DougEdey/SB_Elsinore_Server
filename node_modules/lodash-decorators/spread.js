"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var spread = require("lodash/spread");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(spread, new applicators_1.PreValueApplicator()));
function Spread(start) {
    return decorator(start);
}
exports.Spread = Spread;
exports.spread = Spread;
exports.default = decorator;
//# sourceMappingURL=spread.js.map