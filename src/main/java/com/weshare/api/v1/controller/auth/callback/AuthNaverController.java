package com.weshare.api.v1.controller.auth.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthNaverController {

    @GetMapping("/callback/naver")
    public String test(@RequestParam String code){
        log.info(code);
        return code;
    }
}