package com.sb.elsinore.annotations;

import com.sb.elsinore.NanoHTTPD;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * used for a rest endpoint.
 * Created by Douglas on 2016-03-31.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RestEndpoint {
    String path();
    NanoHTTPD.Method method() default NanoHTTPD.Method.GET;
    String help();
    Parameter[] parameters() default @Parameter();
}
