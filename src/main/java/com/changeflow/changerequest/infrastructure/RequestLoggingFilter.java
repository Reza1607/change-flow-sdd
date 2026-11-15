package com.changeflow.changerequest.infrastructure;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** Logs only server-generated correlation, method/route template, status and elapsed milliseconds. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE+1)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOG=LoggerFactory.getLogger(RequestLoggingFilter.class);
    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var correlation=UUID.randomUUID().toString();
        long start=System.nanoTime();boolean failed=true;
        try {chain.doFilter(request,response);failed=false;}
        finally {
            // Never use raw URI, query, headers, bodies, authentication or exception messages.
            var pattern=request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String operation=request.getMethod()+" "+(pattern==null ? "unmatched" : pattern.toString());
            LOG.atInfo().addKeyValue("correlationId",correlation)
                .addKeyValue("operation",operation)
                .addKeyValue("outcome",failed ? "ERROR" : Integer.toString(response.getStatus()))
                .addKeyValue("duration",(System.nanoTime()-start)/1_000_000)
                .log("HTTP request completed");
        }
    }
}
