package com.sb.elsinore;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "com.sb.elsinore.nls.messages"; // $NON-NLS-1$

    public static String PH_MODEL;
    public static String PH_SENSORS;
    public static String NEW_PHSENSOR;
    public static String UPDATE_VALUE;
    public static String TEMPERATURE;
    public static String TEMP;
    public static String PUMPS;
    public static String TIMERS;
    public static String SHOW_GRAPH;
    public static String HIDE_GRAPH;
    public static String CONTROLLER;
    public static String SERVER_MESSAGES;
    public static String NEW_PUMP;
    public static String NEW_TIMER;
    public static String NEW_VOLUME;
    public static String UPDATE_CHECK;
    public static String AUTO;
    public static String HYSTERIA;
    public static String MANUAL;
    public static String PID_ON;
    public static String PID_OFF;
    public static String PUMP_ON;
    public static String PUMP_OFF;
    public static String SET_POINT;
    public static String DUTY_CYCLE;
    public static String DUTY_TIME;
    public static String SECS;
    public static String MIN;
    public static String MAX;
    public static String TIME;
    public static String MINUTES;
    public static String SEND_COMMAND;
    public static String AUX_ON;
    public static String AUX_OFF;
    public static String START;
    public static String RESET;
    public static String CLEAR;
    public static String DISABLE;
    public static String ACTIVATE;
    public static String SYSTEM;
    public static String NO_VOLUME;
    public static String MASH_STEP;
    public static String ADD;
    public static String DELETE;
    public static String UPDATE;
    public static String CANCEL;
    public static String ANALOGUE_PIN;
    public static String DS2450_ADDRESS;
    public static String DS2450_OFFSET;
    public static String LITRES;
    public static String UK_GALLONS;
    public static String US_GALLONS;
    public static String CUTOFF_TEMP;
    public static String INVALID_GPIO;
    public static String NAME;

    public static String PUMPNAMEBLANK;

    public static String GPIO_BLANK;
    public static String TIMERNAMEBLANK;
    public static String TEMP_UNIT;
    public static String METHOD;
    public static String TYPE;
    public static String BREWERY_QUESTION;
    public static String DELETE_PUMP;
    public static String DELETE_TIMER;
    public static String EDIT;

    public static String EDIT_VOLUME;
    public static String LOCK;
    public static String DUTYPERC;
    public static String HEAT;
    public static String COOL;
    public static String DELAY;
    public static String CALIBRATE;
    public static String TRIGGERS;

    public static String ADD_TRIGGER;
    public static String MINS;
    public static String HOURS;
    public static String DAYS;
    public static String WEEKS;

    public static String DRY_ADDITIONS;
    public static String BOIL_ADDITIONS;
    public static String MASH_PROFILE;
    public static String FERMENT_PROFILE;

    public static String SET_MASH_PROFILE;
    public static String SET_FERM_PROFILE;
    public static String SET_BOIL_HOP_PROFILE;
    public static String SET_DRY_HOP_PROFILE;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
