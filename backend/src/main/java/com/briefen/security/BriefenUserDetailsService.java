package com.briefen.security;

import com.briefen.persistence.UserPersistence;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BriefenUserDetailsService implements UserDetailsService {

    private final UserPersistence userPersistence;

    public BriefenUserDetailsService(UserPersistence userPersistence) {
        this.userPersistence = userPersistence;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userPersistence.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());
        return new BriefenUserDetails(user.getId(), user.getUsername(), user.getPasswordHash(), List.of(authority));
    }
}
