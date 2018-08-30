"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var rest = require("lodash/rest");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(rest, new applicators_1.PreValueApplicator()));
function Rest(start) {
    return decorator(start);
}
exports.Rest = Rest;
exports.rest = Rest;
exports.default = decorator;
//# sourceMappingURL=rest.js.map