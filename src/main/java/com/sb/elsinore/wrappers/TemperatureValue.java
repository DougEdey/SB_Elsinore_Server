package com.sb.elsinore.wrappers;

import ca.strangebrew.recipe.Quantity;
import com.sb.util.MathUtil;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class TemperatureValue {

    /**
     * Magic numbers.
     * F_TO_C_MULT: Multiplier to convert F to C.
     * C_TO_F_MULT: Multiplier to convert C to F.
     * FREEZING: The freezing point of Fahrenheit.
     */
    public static MathContext context = new MathContext(2, RoundingMode.HALF_DOWN);
    private static BigDecimal FREEZING = new BigDecimal(32);
    private Long time = null;


    public Long getTime() {
        return this.time;
    }

    public BigDecimal getValue() {
        return this.value;
    }

    /**
     * Get the current value in the specified scale
     * @param scale The scale to get the value in
     * @return The current value in the specified scale, may be null
     */
    public BigDecimal getValue(Scale scale) {
        if (this.value == null) {
            return null;
        }

        BigDecimal convertedValue = this.value;
        if (this.scale == Scale.F && scale == Scale.C) {
            convertedValue = fToC(this.value);
        } else if (this.scale == Scale.C && scale == Scale.F) {
            convertedValue = cToF(this.value);
        }
        return convertedValue;
    }

    public enum Scale {
        C, F
    }

    private Scale scale = null;

    private BigDecimal value = null;

    public void setScale(Scale scale) {
        if (this.scale != null && this.value != null) {
            if (this.scale == Scale.F && scale == Scale.C) {
                this.value = fToC(this.value);
            } else if (this.scale == Scale.C && scale == Scale.F) {
                this.value = cToF(this.value);
            }
        }
        this.scale = scale;
    }

    /**
     * Set the current value of this temperature with the scale of the new value
     * This will not change the scale, but convert the incoming value appropriately
     * @param value The new value
     * @param scale The scale of the value
     */
    public void setValue(BigDecimal value, Scale scale) {
        if (this.scale == null) {
            this.scale = scale;
        } else {
            if (this.scale == Scale.F && scale == Scale.C) {
                value = fToC(value);
            } else if (this.scale == Scale.C && scale == Scale.F) {
                value = cToF(value);
            }
        }
        this.value = value;
        this.time = System.currentTimeMillis();
    }

    /**
     * @param currentTemp temperature to convert in Fahrenheit
     * @return Temperature in celsius
     */
    private BigDecimal fToC(final BigDecimal currentTemp) {
        BigDecimal t = currentTemp.subtract(FREEZING);
        t = MathUtil.divide(MathUtil.multiply(t, 5), 9);
        return t;
    }

    /**
     * @param currentTemp temperature to convert in Celsius
     * @return Temperature in Fahrenheit
     */
    public static BigDecimal cToF(final BigDecimal currentTemp) {
        BigDecimal t = MathUtil.divide(MathUtil.multiply(currentTemp, 9), 5);
        t = t.add(FREEZING);
        return t;
    }

}
