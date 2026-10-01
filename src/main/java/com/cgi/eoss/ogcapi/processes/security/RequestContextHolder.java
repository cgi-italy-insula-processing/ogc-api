package com.cgi.eoss.ogcapi.processes.security;

/**
 * Class that holds the {@link RequestContext} information for the current thread.
 */

public class RequestContextHolder implements AutoCloseable {

    private static final ThreadLocal<RequestContext> CURRENT_CONTEXT = new ThreadLocal<>();

    /**
     * The class public constructor which sets the request context for the current thread.
     * @param requestContext the request context to be set in the current thread.
     */
    public RequestContextHolder(RequestContext requestContext) {
        setContext(requestContext);
    }

    /**
     * Gets the request context for the current thread.
     * @return the current request context.
     */
    public static RequestContext getCurrent() {
        return CURRENT_CONTEXT.get();
    }

    @Override
    public void close() {
        RequestContext prevContext = CURRENT_CONTEXT.get();

        if (prevContext == null) {
            throw new IllegalStateException("RequestContext was null when closing RequestContextHolder");
        }

        CURRENT_CONTEXT.remove();
    }

    private void setContext(RequestContext requestContext) {
        RequestContext prevContext = CURRENT_CONTEXT.get();

        if (prevContext != null) {
            throw new IllegalStateException("Previous RequestContext was not null: '" + prevContext + "'");
        }
        if (!requestContext.isValid()) {
            throw new IllegalStateException("RequestContext is not valid: '" + requestContext + "'");
        }

        CURRENT_CONTEXT.set(requestContext);
    }
}
