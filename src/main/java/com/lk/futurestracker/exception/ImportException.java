package com.lk.futurestracker.exception;

/** Thrown when an uploaded Excel file can't be read at all (wrong format, missing required columns, etc). */
public class ImportException extends RuntimeException {
    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
