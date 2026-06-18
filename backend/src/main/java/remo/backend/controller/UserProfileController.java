package remo.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import remo.backend.entity.ProfileStatus;
import remo.backend.entity.UserProfile;
import remo.backend.service.UserProfileService;

@RestController
@RequestMapping("/api/me/profile")
public class UserProfileController {
    private final UserProfileService userProfileService;
    @Autowired
    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public UserProfile getOwnProfile(Authentication authentication) {
        return userProfileService.getOwnProfile(authentication.getName());
    }

    @PutMapping
    @PreAuthorize("hasRole('USER')")
    public UserProfile updateOwnProfile(
            @RequestBody UserProfile request,
            Authentication authentication) {
        return userProfileService.updateOwnProfile(
                authentication.getName(), request);
    }

    @PutMapping("/{profileId}")
    @PreAuthorize("@userProfileSecurity.isOwner(authentication, #profileId)")
    public UserProfile updateOwnProfile(
            @PathVariable Long profileId,
            @RequestBody UserProfile request) {
        return userProfileService.updateOwnProfile(
                profileId, request);
    }

    @PatchMapping("/{profileId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfile verifyProfile(@PathVariable Long profileId) {
        return userProfileService.setProfileStatus(
                profileId, ProfileStatus.VERIFIED);
    }

    @PatchMapping("/{profileId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfile lockProfile(@PathVariable Long profileId) {
        return userProfileService.setProfileStatus(
                profileId, ProfileStatus.LOCKED);
    }

    @PatchMapping("/{profileId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfile unlockProfile(@PathVariable Long profileId) {
        return userProfileService.setProfileStatus(
                profileId, ProfileStatus.UNVERIFIED);
    }
}

