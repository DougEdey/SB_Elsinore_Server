package com.sb.common;

import org.h2.util.StringUtils;

public class Result {
    public static String OK = "OK";
    public static String ERROR = "ERROR";

    public String result = OK;
    public String errorMessage = null;

    /**
     * Set the error message for this result if it's null/empty, set the Result to OK
     * otherwise sets Result to ERROR
     * @param errorMessage The error message to set for this result
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        if (StringUtils.isNullOrEmpty(errorMessage)) {
            this.result = Result.OK;
        } else {
            this.result = Result.ERROR;
        }
    }

    public boolean isOK() {
        return OK.equals(result);
    }

    public boolean isError() {
        return ERROR.equals(result);
    }
}
