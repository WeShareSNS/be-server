package com.weshare.api.v1.controller.schedule;

import com.weshare.api.v1.common.CookieTokenHandler;
import com.weshare.api.v1.domain.user.User;
import com.weshare.api.v1.service.schedule.ScheduleService;
import com.weshare.api.v1.token.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/trip")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    private final JwtService jwtService;
    private final ScheduleService scheduleService;
    private final CookieTokenHandler cookieTokenHandler;

    /**
     *
     * cookieTokenHandler에 jwtService 로직이 있으면 좋겠다는 생각했습니다.
     *
     * AuthService에서 잘 쓰고 있어서 jwtService를 유틸클래스로 사용하기에는 애매하다고 생각이 들었고
     * public String extractEmail(String token)만 정적 메서드로 사용하는건 문제가 있을까요?
     */
//    @PostMapping("/schedule")
    public ResponseEntity saveSchedule(@Valid @RequestBody CreateScheduleRequest createScheduleRequest,
                                       HttpServletRequest request) {
        String accessToken = cookieTokenHandler.getBearerToken(request);
        String userEmail = jwtService.extractEmail(accessToken);

        scheduleService.saveSchedule(createScheduleRequest, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/schedule")
    public ResponseEntity saveSchedule(@Valid @RequestBody CreateScheduleRequest createScheduleRequest,
                                       HttpServletRequest request,
                                       @AuthenticationPrincipal User user) {
        String accessToken = cookieTokenHandler.getBearerToken(request);
        String userEmail = jwtService.extractEmail(accessToken);
        if (!Objects.equals(userEmail, user.getEmail())) {
            throw new IllegalArgumentException();
        }
        // createScheduleRequest.setUser로 넣어줘서 service에서 검증할 수 있을거 같고, 파라미터로 User를 파라미터로 넘길 수 있을거 같습니다.
        scheduleService.saveSchedule(createScheduleRequest, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
