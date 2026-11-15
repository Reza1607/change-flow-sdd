package com.changeflow.identity.infrastructure;

import java.util.Collection;
import java.util.List;
import com.changeflow.identity.application.CurrentActor;
import com.changeflow.identity.domain.ActorId;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseIdentityAdapter implements UserDetailsService, CurrentActor {
    private final IdentityAccountRepository accounts;
    public DatabaseIdentityAdapter(IdentityAccountRepository accounts) { this.accounts=accounts; }

    @Override @Transactional(readOnly=true)
    public UserDetails loadUserByUsername(String username) {
        var a=accounts.findByUsername(username).orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));
        return new IdentityPrincipal(new ActorId(a.actorId),a.username,a.passwordHash,a.enabled);
    }
    @Override
    public ActorId require() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof IdentityPrincipal p))
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        return p.actorId();
    }
    private record IdentityPrincipal(ActorId actorId,String username,String password,boolean enabled) implements UserDetails {
        @Override public String getUsername() {return username;}
        @Override public String getPassword() {return password;}
        @Override public boolean isEnabled() {return enabled;}
        @Override public Collection<? extends GrantedAuthority> getAuthorities() {return List.of();}
        @Override public String toString() {return "IdentityPrincipal";}
    }
}
