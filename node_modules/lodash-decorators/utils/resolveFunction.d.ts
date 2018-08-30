/**
  * Resolves a function on the current target object. It first will
  * try and resolve on the context object, then the target object,
  * then an error will be thrown if the method can not be resolved.
  * @private
  * @param {Function|string} method The method or method name.
  * @param {Object} [context] The context object to resolve from.
  * @param {Object} [target] The target object to resolve from.
  * @returns {Function} The resolved function.
  */
export declare function resolveFunction(method?: string | Function, context?: any, target?: any, throwNotFound?: boolean): any;
