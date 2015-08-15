package com.sb.elsinore;

/**
 * Volume Units here are to be used in the future for conversion.
 */
final class VolumeUnits {
    /**
     * The US Gallon Measurement, 3.8 Litres.
     */
    public static final String US_GALLONS = "US Gallons";
    /**
     * The UK/Imperial Gallon, 4.2 litres.
     */
    public static final String UK_GALLONS = "UK Gallons";
    /**
     * The Litres, SI standard, 1000ml.
     */
    public static final String LITRES = "Litres";
    public static final String VOLUME_UNITS = "volume-unit";
    public static final String VOLUME_PIN = "volume-ain";
    public static final String VOLUME_ADDRESS = "volume-address";
    public static final String VOLUME_OFFSET = "volume-offset";

    /**
     * Constructor.
     */
    private VolumeUnits() {
        // This prevents instantiation
    }
}
