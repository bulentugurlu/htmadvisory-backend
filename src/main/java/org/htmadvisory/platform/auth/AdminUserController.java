package org.htmadvisory.platform.auth;

import org.htmadvisory.platform.auth.dto.UserProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Admin-only member account management. Every route here requires a valid
 * JWT AND an {@code ADMIN} role — both enforced by {@code
 * JwtAuthInterceptor} before the request ever reaches this controller (see
 * its registration for {@code /api/admin/**} in {@code WebMvcConfig}).
 *
 * <p>{@code listUsers} isn't in the original spec but is added alongside
 * {@code approve} — an admin can't approve an account they have no way of
 * seeing. Defaults to {@code PENDING} since that's the queue an admin
 * actually works from; pass {@code ?status=} (empty) or any status value to
 * see others.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserProfileResponse> listUsers(
            @RequestParam(required = false, defaultValue = "PENDING") String status) {
        Optional<UserStatus> filter = status.isBlank()
                ? Optional.empty()
                : Optional.of(UserStatus.valueOf(status.toUpperCase()));
        return userService.listUsers(filter);
    }

    @PostMapping("/{id}/approve")
    public UserProfileResponse approve(@PathVariable String id) {
        return userService.approve(id);
    }
}
