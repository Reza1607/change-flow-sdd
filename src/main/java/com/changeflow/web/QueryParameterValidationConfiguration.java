package com.changeflow.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** A-007: reject present empty query values before MVC substitutes defaults. */
@Configuration(proxyBeanMethods=false)
public class QueryParameterValidationConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler)
                    throws ServletRequestBindingException {
                if(handler instanceof HandlerMethod) {
                    for(String name:List.of("page","size","status","expectedVersion")) {
                        String[] values=request.getParameterValues(name);
                        if(values!=null) for(String value:values) {
                            if(value.isEmpty())throw new ServletRequestBindingException("Empty query parameter");
                        }
                    }
                }
                return true;
            }
        }).addPathPatterns("/api/v1/change-requests","/api/v1/change-requests/**");
    }
}
