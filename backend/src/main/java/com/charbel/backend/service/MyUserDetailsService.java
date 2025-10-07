package com.charbel.backend.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charbel.backend.model.UserPrincipal;
import com.charbel.backend.repo.UserRepo;

@Service
@Transactional(readOnly = true)
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepo repo;

    public MyUserDetailsService(UserRepo repo){ this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
        return repo.findByEmailIgnoreCase(email.trim().toLowerCase()).map(UserPrincipal::new).orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));
    }
}