package com.weshare.api.v1.controller.schedule.query;

import com.weshare.api.v1.repository.schedule.query.dto.ScheduleDetailDto;
import com.weshare.api.v1.repository.schedule.query.dto.SchedulePageDto;
import com.weshare.api.v1.service.schedule.query.ScheduleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trip")
@RequiredArgsConstructor
public class ScheduleQueryController {

    private final ScheduleQueryService scheduleQueryService;

    @GetMapping("/schedule")
    public Page<SchedulePageDto> getSchedule(
            @PageableDefault(size = 12, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return scheduleQueryService.getSchedulePage(pageable);
    }

    @GetMapping("/schedule/{scheduleId}")
    public List<ScheduleDetailDto> getScheduleDetails(@PathVariable Long scheduleId) {
        return scheduleQueryService.getScheduleDetails(scheduleId);
    }
}