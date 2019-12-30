package com.sb.util;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 *
 */
public final class MathUtil
{
    
    private static MathContext context = MathContext.DECIMAL32;
    
    public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator)
    {
        return numerator.divide(denominator, context);
    }
    
    public static BigDecimal divide(BigDecimal numerator, int denominator)
    {
        return divide(numerator, BigDecimal.valueOf(denominator));
    }
    
    public static BigDecimal divide(BigDecimal numerator, double denominator)
    {
        return divide(numerator, BigDecimal.valueOf(denominator));
    }
    
    public static BigDecimal divide(int numerator, int denominator)
    {
        return divide(BigDecimal.valueOf(numerator), denominator);
    }
    
    public static BigDecimal multiply(BigDecimal a, int multiplier)
    {
        return a.multiply(BigDecimal.valueOf(multiplier));
    }
    
    public static BigDecimal multiply(BigDecimal a, double multiplier)
    {
        return a.multiply(BigDecimal.valueOf(multiplier));
    }
    
    
    
    
    
}
