package com.sb.common;

import java.util.Collections;
import java.util.List;

public class CollectionsUtil {

  public static <T extends Comparable<T>> int addInOrder(final List<T> list, final T item) {
    final int insertAt;
    // The index of the search key, if it is contained in the list; otherwise, (-(insertion point) - 1).
    final int index = Collections.binarySearch(list, item);
    if (index < 0) {
      insertAt = -(index + 1);
    } else {
      insertAt = index + 1;
    }

    list.add(insertAt, item);
    return insertAt;
  }

  private CollectionsUtil() {
  }
}