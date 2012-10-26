package org.eclipse.persistence.jpa.rs.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.eclipse.persistence.jpa.rs.util.JPARSLogger;

@Provider
public class ClassNotFoundExceptionMapper implements ExceptionMapper<ClassNotFoundException> {
    public Response toResponse(ClassNotFoundException exception){
        JPARSLogger.exception("jpars_caught_exception", new Object[]{}, exception);
        return Response.status(Status.BAD_REQUEST).build();
    }
    /*
    public Response toResponse(ClassNotFoundException exception, MediaType mediaType){
        JPARSLogger.exception("jpars_caught_exception", new Object[]{}, exception);
        return Response.status(Response.Status.BAD_REQUEST).
                entity(new ErrorResponse(1234, exception.getMessage())).type(mediaType).build();
    }*/
}
