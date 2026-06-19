package remo.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import remo.backend.entity.Account;
import remo.backend.entity.ProfileStatus;
import remo.backend.entity.UserProfile;
import remo.backend.exceptions.InvalidProfileStateException;
import remo.backend.exceptions.ProfileNotFoundException;
import remo.backend.repository.AccountRepository;
import remo.backend.repository.UserProfileRepository;

@Service
public class UserProfileService {
    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    @Autowired
    public UserProfileService(AccountRepository accountRepository, UserProfileRepository userProfileRepository) {
        this.accountRepository = accountRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile getOwnProfile(String username) {
        return accountRepository.findAccountByUsername(username)
                .map(Account::getUserProfile)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));
    }

    public UserProfile updateOwnProfile(String username, UserProfile request) {
        Account account = accountRepository.findAccountByUsername(username)
                .orElse(null);
        if (account == null) {
            throw new ProfileNotFoundException("Profile not found");
        }
        Long userProfileId = account.getUserProfile().getId();
        return userProfileRepository.save(new UserProfile(
                userProfileId,
                request.getFirstName(),
                request.getLastName(),
                request.getAddress(),
                request.getProfileImgUrl(),
                request.getProfileStatus()
        ));
    }

    public UserProfile updateOwnProfile(Long profileId, UserProfile request) {
        Account account = accountRepository.findById(profileId)
                .orElse(null);
        if (account == null) {
            throw new ProfileNotFoundException("Profile not found");
        }
        Long userProfileId = account.getUserProfile().getId();
        return userProfileRepository.save(new UserProfile(
                userProfileId,
                request.getFirstName(),
                request.getLastName(),
                request.getAddress(),
                request.getProfileImgUrl(),
                request.getProfileStatus()
        ));
    }

    public UserProfile setProfileStatus(Long profileId, ProfileStatus status) {
        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));

        if (status == profile.getProfileStatus()) {
            throw new InvalidProfileStateException("Profile status is already set to the requested value");
        } else if (status == ProfileStatus.UNVERIFIED && profile.getProfileStatus() == ProfileStatus.VERIFIED) {
            throw new InvalidProfileStateException("Profile can not be unlocked in a verified state");
        }

        profile.setProfileStatus(status);

        return userProfileRepository.save(profile);
    }
}
