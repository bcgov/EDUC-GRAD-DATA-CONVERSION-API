package ca.bc.gov.educ.api.dataconversion.exception;

import lombok.Data;

@Data
public class ServiceException extends RuntimeException {

    private int statusCode;

    public ServiceException() {
        super();
    }

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceException(Throwable cause) {
        super(cause);
    }

    protected ServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ServiceException(String message, int value) {
        super(message);
        this.statusCode = value;
    }

    public ServiceException(String s, int value, Exception e) {
        super(s, e);
        this.statusCode = value;
    }
}
