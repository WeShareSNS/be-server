package com.weshare.api.v1.controller.like.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Schema(description = "사용자가 등록한 좋아요 응답 API")
public record CreateScheduleLikeResponse(
        @Schema(title = "사용자가 좋아요를 등록한 여행 일정 id", description = "사용자가 좋아요를 등록한 여행 일정 id를 응답한다.")
        Long scheduleId,
        @Schema(title = "사용자가 등록한 좋아요 id", description = "사용자가 등록한 좋아요 id를 응답한다.")
        Long likeId,
        @Schema(title = "좋아요를 등록한 사용자 이름", description = "좋아요를 등록한 사용자의 이름을 응답한다.")
        String likerName,
        @Schema(title = "사용자가 좋아요를 등록한 시간", description = "사용자가 좋아요를 등록한 시간을 응답한다.")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm a", timezone = "Asia/Seoul", locale = "en_US")
        LocalDateTime likedTime
) {
    public CreateScheduleLikeResponse {
        Objects.requireNonNull(scheduleId);
        Objects.requireNonNull(likeId);
        if (!StringUtils.hasText(likerName)) {
            throw new IllegalStateException("사용자가 올바르지 않습니다.");
        }
        Objects.requireNonNull(likedTime);
    }
}
