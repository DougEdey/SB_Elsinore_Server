export type Maybe<T> = T | null;

export interface TemperatureModelInput {
  name?: Maybe<string>;

  position?: Maybe<number>;

  i2cNumber?: Maybe<string>;

  i2cChannel?: Maybe<string>;

  volumeMeasurementEnabled?: Maybe<boolean>;

  calibration?: Maybe<BigDecimal>;

  volumeAIN?: Maybe<number>;

  cutoffTemp?: Maybe<BigDecimal>;

  i2cAddress?: Maybe<string>;

  hidden?: Maybe<boolean>;

  volumeUnit?: Maybe<string>;

  volumeOffset?: Maybe<string>;

  nameLowercased?: Maybe<string>;

  device?: Maybe<string>;

  id?: Maybe<Long>;

  scale?: Maybe<string>;

  volumeAddress?: Maybe<string>;

  i2cType?: Maybe<string>;
}

/** Long type */
export type Long = any;

/** Built-in java.math.BigDecimal */
export type BigDecimal = any;

/** Built-in scalar for map-like structures */
export type ConcurrentHashMapBigDecimalBigDecimalScalar = any;

/** Unrepresentable type */
export type Unrepresentable = any;
