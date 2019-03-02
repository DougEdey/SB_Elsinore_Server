export type Maybe<T> = T | null;

export interface TemperatureModelInput {
  volumeOffset?: Maybe<string>;

  volumeAIN?: Maybe<number>;

  device?: Maybe<string>;

  name?: Maybe<string>;

  i2cChannel?: Maybe<string>;

  calibration?: Maybe<BigDecimal>;

  cutoffTemp?: Maybe<BigDecimal>;

  volumeAddress?: Maybe<string>;

  i2cNumber?: Maybe<string>;

  i2cType?: Maybe<string>;

  volumeUnit?: Maybe<string>;

  nameLowercased?: Maybe<string>;

  i2cAddress?: Maybe<string>;

  id?: Maybe<Long>;

  position?: Maybe<number>;

  scale?: Maybe<string>;

  volumeMeasurementEnabled?: Maybe<boolean>;

  hidden?: Maybe<boolean>;
}

/** Long type */
export type Long = any;

/** Built-in java.math.BigDecimal */
export type BigDecimal = any;

/** Built-in scalar for map-like structures */
export type ConcurrentHashMapBigDecimalBigDecimalScalar = any;

/** Unrepresentable type */
export type Unrepresentable = any;
