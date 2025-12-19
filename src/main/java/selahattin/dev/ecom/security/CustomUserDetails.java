package selahattin.dev.ecom.security;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.entity.auth.UserEntity;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final transient UserEntity user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // UserEntity içindeki RoleEntity setini gezip Spring Security'nin anlayacağı
        // dile çeviriyorum
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        // OTP sisteminde password yoktur. Interface gereği null dönüyoruz.
        return null;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}