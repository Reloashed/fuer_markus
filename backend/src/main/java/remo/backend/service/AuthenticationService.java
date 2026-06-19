package remo.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import remo.backend.dto.AuthenticationDto;
import remo.backend.dto.RegisterDto;
import remo.backend.dto.TokenDto;
import remo.backend.entity.Account;
import remo.backend.entity.PendingRegistration;
import remo.backend.entity.ProfileStatus;
import remo.backend.entity.UserProfile;
import remo.backend.repository.AccountRepository;
import remo.backend.repository.UserProfileRepository;
import remo.backend.security.JwtService;

import javax.swing.text.html.Option;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static remo.backend.entity.ProfileStatus.UNVERIFIED;
import static remo.backend.security.Role.USER;

@Service
public class AuthenticationService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationService registrationService;
    private final MailService mailService;
    private final AccountService accountService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthenticationService(AccountRepository accountRepository, PasswordEncoder passwordEncoder, RegistrationService registrationService, MailService mailService, AccountService accountService, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationService = registrationService;
        this.mailService = mailService;
        this.accountService = accountService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public Optional<TokenDto> authenticateUser(AuthenticationDto authDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authDto.username(),
                        authDto.passwordHash()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtService.generateToken(authentication);
        return Optional.of(new TokenDto(token));
    }

    public HttpStatus registrateUser(RegisterDto registerDto) {
        if (accountRepository.findAccountByEmail(registerDto.email()).isEmpty() && !registrationService.mailExists(registerDto.email())) {
            String passwordHash = passwordEncoder.encode(registerDto.password());
            UUID registrationToken = registrationService.startRegistration(registerDto, passwordHash);
            mailService.sendRegistrationVerification(registerDto.email(), registrationToken);
        } else {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.CREATED;
    }

    public HttpStatus confirmRegistration(UUID token) {
        Optional<PendingRegistration> pendingRegistrationOptional = registrationService.getPendingRegistration(token);
        if (pendingRegistrationOptional.isPresent()) {
            PendingRegistration pendingRegistration = pendingRegistrationOptional.get();
            if (pendingRegistration.getCreatedAt().isAfter(Instant.now().minus(Duration.ofDays(1)))) {
                UserProfile userProfile = new UserProfile(
                        null,
                        pendingRegistration.getFirstname(),
                        pendingRegistration.getLastname(),
                        "address",
                        "img",
                        UNVERIFIED
                );
                accountService.createAccount(Account.builder()
                                .firstName(pendingRegistration.getFirstname())
                                .lastName(pendingRegistration.getLastname())
                                .username(pendingRegistration.getEmail())
                                .email(pendingRegistration.getEmail())
                                .passwordHash(pendingRegistration.getPasswordHash())
                                .role(USER)
                                .userProfile(userProfile)
                        .build());
                registrationService.removePendingRegistration(token);
                return HttpStatus.OK;
            } else {
                return HttpStatus.GONE;
            }
        } else {
            return HttpStatus.NOT_FOUND;
        }
    }
}
