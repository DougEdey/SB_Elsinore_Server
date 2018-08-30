# Changelog
## [Unreleased]
- Updates `focus` functions to match only non-disabled nodes
- Adds `clear` function to `fastdom`. Usage:
```ts
import {write, read, clear} from '@shopify/javscript-utilities/fastdom';

const writeTask = write(...);
const readTask = read(..);

// later
clear(writeTask);
clear(readTask);
```

## [2.0.0] - 2017-07-27
### Added
- Adds a `CHANGELOG` to document changes moving forward.

### Changed
- The `autobind` decorator is now in the `decorators` directory.
    - **WARNING** any existing imports must be changed accordingly:
```ts
// old way
import autobind from '@shopify/javascript-utilities/autobind';

// new way
import {autobind} from '@shopify/javascript-utilities/decorators';
```
- Updated documentation in `README.md` and `CONTRIBUTING.md`.

### Fixed
- Multiple decorators can now be used simultaneously (ex. `autobind` with `debounce`).


[Unreleased]: https://github.com/shopify/javascript-utilities/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/shopify/javascript-utilities/compare/v1.1.6...v2.0.0