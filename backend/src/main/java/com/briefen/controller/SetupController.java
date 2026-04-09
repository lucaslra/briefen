package com.briefen.controller;

import com.briefen.dto.SetupRequest;
import com.briefen.dto.SetupStatusResponse;
import com.briefen.dto.UserDto;
import com.briefen.service.SetupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * First-run setup endpoints. Both are unauthenticated because no user exists yet.
 * POST /api/setup is locked down the moment an admin account exists (enforced by SetupService).
 */
@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final SetupService setupService;

    public SetupController(SetupService setupService) {
        this.setupService = setupService;
    }

    @GetMapping("/status")
    public SetupStatusResponse status() {
        return new SetupStatusResponse(setupService.isSetupRequired());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto setup(@RequestBody SetupRequest request) {
        var user = setupService.createInitialAdmin(request.username(), request.password());
        return UserDto.from(user);
    }
}
