"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var DecoratorConfig = (function () {
    function DecoratorConfig(execute, applicator, options) {
        if (options === void 0) { options = {}; }
        this.execute = execute;
        this.applicator = applicator;
        this.options = options;
    }
    Object.defineProperty(DecoratorConfig.prototype, "bound", {
        get: function () {
            return this.options.bound != null ? this.options.bound : false;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DecoratorConfig.prototype, "setter", {
        get: function () {
            return this.options.setter != null ? this.options.setter : false;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DecoratorConfig.prototype, "getter", {
        get: function () {
            return this.options.getter != null ? this.options.getter : false;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DecoratorConfig.prototype, "property", {
        get: function () {
            return this.options.property != null ? this.options.property : false;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DecoratorConfig.prototype, "method", {
        get: function () {
            return this.options.method != null ? this.options.method : true;
        },
        enumerable: true,
        configurable: true
    });
    return DecoratorConfig;
}());
exports.DecoratorConfig = DecoratorConfig;
//# sourceMappingURL=DecoratorConfig.js.map