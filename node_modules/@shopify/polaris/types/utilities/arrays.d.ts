export interface ArrayComparator<T> {
    (firstArray: T, SecondArray: T): boolean;
}
export declare function arraysAreEqual<T>(firstArray: T[], secondArray: T[], comparator?: ArrayComparator<T>): boolean;
