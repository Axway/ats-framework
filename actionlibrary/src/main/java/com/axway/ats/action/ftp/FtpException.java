package com.axway.ats.action.ftp;

import com.axway.ats.common.PublicAtsApi;

@PublicAtsApi
public class FtpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @PublicAtsApi
    public FtpException(String message, Throwable cause) {
        super(message, cause);
    }

    @PublicAtsApi
    public FtpException(Throwable cause) {
        super(cause);
    }

    @PublicAtsApi
    public FtpException(String message) {
        super(message);
    }
}
