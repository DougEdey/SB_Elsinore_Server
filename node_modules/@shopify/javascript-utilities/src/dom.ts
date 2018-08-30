export function nodeContainsDescendant(
  rootNode: HTMLElement,
  descendant: HTMLElement,
): boolean {
  if (rootNode === descendant) { return true; }

  let parent = descendant.parentNode;

  while (parent != null) {
    if (parent === rootNode) { return true; }
    parent = parent.parentNode;
  }

  return false;
}

/* tslint:disable */
// Polyfill for .matches()
// https://developer.mozilla.org/en/docs/Web/API/Element/matches
export function matches(node: HTMLElement, selector: string) {
  if (node.matches) {
    return node.matches(selector);
  }

  const matches = (node.ownerDocument || document).querySelectorAll(selector);
  let i = matches.length;
  while (--i >= 0 && matches.item(i) !== node) {}
  return i > -1;
};

// Polyfill for .closest()
// https://developer.mozilla.org/en-US/docs/Web/API/Element/closest
export function closest(node: HTMLElement, selector: string) {
  if (node.closest) {
    return node.closest(selector);
  }

  const matches = document.querySelectorAll(selector);
  let i;
  let el: HTMLElement | null | undefined = node;
  do {
    el = el.parentElement;
    i = matches.length;
    while (--i >= 0 && matches.item(i) !== el) {
      continue;
    };
  } while ((i < 0) && (el));
  return el;
}
/* tslint:enable */
