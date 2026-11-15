package com.changeflow.web;

import com.changeflow.identity.infrastructure.DatabaseIdentityAdapter;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods=false)
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() {return PasswordEncoderFactories.createDelegatingPasswordEncoder();}
    @Bean CookieCsrfTokenRepository csrfTokenRepository() {
        var repository=new CookieCsrfTokenRepository();
        repository.setCookieCustomizer(cookie->cookie.httpOnly(true).sameSite("Strict"));
        return repository;
    }
    @Bean AuthenticationManager authenticationManager(DatabaseIdentityAdapter identity,PasswordEncoder encoder) {
        var provider=new DaoAuthenticationProvider(identity);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,AuthenticationManager manager,ObjectMapper mapper,CookieCsrfTokenRepository csrf) throws Exception {
        AuthenticationEntryPoint entry=(request,response,ex)-> {
            if(ex instanceof AuthenticationServiceException) {
                ProblemResponses.write(response,mapper,HttpStatus.SERVICE_UNAVAILABLE,"DEPENDENCY_UNAVAILABLE");
            } else {
                response.setHeader("WWW-Authenticate","Basic realm=\"change-flow\"");
                ProblemResponses.write(response,mapper,HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED");
            }
        };
        var basic=new BasicAuthenticationFilter(manager,entry);
        basic.setSecurityContextRepository(new NullSecurityContextRepository());
        // Preserve the token returned by /csrf as the header token. Cookie Secure follows request TLS.
        var tokenHandler=new CsrfTokenRequestAttributeHandler() {
            @Override public String resolveCsrfTokenValue(jakarta.servlet.http.HttpServletRequest request,CsrfToken token) {
                return request.getHeader(token.getHeaderName());
            }
        };
        var cors=new org.springframework.web.filter.CorsFilter(request->new CorsConfiguration());
        cors.setCorsProcessor(new org.springframework.web.cors.DefaultCorsProcessor() {
            @Override protected void rejectRequest(org.springframework.http.server.ServerHttpResponse response) throws java.io.IOException {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON);
                response.getHeaders().setCacheControl("no-store");
                mapper.writeValue(response.getBody(),ProblemResponses.create(HttpStatus.FORBIDDEN,"FORBIDDEN"));
                response.flush();
            }
        });
        http.authenticationManager(manager)
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(s->s.securityContextRepository(new NullSecurityContextRepository()))
            .requestCache(s->s.requestCache(new NullRequestCache()))
            .cors(c->c.disable())
            .formLogin(f->f.disable()).logout(l->l.disable())
            .csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(tokenHandler))
            .exceptionHandling(e->e.authenticationEntryPoint(entry).accessDeniedHandler((request,response,ex)-> {
                var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if(auth==null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
                    entry.commence(request,response,new InsufficientAuthenticationException("Authentication required"));
                else ProblemResponses.write(response,mapper,HttpStatus.FORBIDDEN,
                    ex instanceof CsrfException ? "CSRF_INVALID" : "FORBIDDEN");
            }))
            .authorizeHttpRequests(a->a.anyRequest().authenticated())
            // Authenticate before CSRF so valid anonymous mutations receive the contractual 401.
            .addFilterBefore(basic,CsrfFilter.class)
            .addFilterBefore(cors,CsrfFilter.class);
        return http.build();
    }
}
