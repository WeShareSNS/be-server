package com.weshare.api.v1.service.schedule.query;

import com.weshare.api.v1.repository.schedule.query.ScheduleDetailQueryRepository;
import com.weshare.api.v1.repository.schedule.query.SchedulePageQueryRepository;
import com.weshare.api.v1.repository.schedule.query.dto.ScheduleDetailDto;
import com.weshare.api.v1.repository.schedule.query.dto.SchedulePageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleQueryService {

    private final SchedulePageQueryRepository pageQueryRepository;
    private final ScheduleDetailQueryRepository detailQueryRepository;

    public Page<SchedulePageDto> getSchedulePage(Pageable pageable) {
        return pageQueryRepository.findSchedulePage(pageable);
    }

    public List<ScheduleDetailDto> getScheduleDetails(Long scheduleId) {
        if (scheduleId == null) {
            throw new IllegalArgumentException("게시물에 접근할 수 없습니다.");
        }
        return detailQueryRepository.findScheduleDetail(scheduleId);
    }
}