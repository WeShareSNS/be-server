package com.weshare.api.v1.event.schedule.statistics;

import com.weshare.api.v1.domain.schedule.Schedule;
import com.weshare.api.v1.domain.schedule.exception.ScheduleNotFoundException;
import com.weshare.api.v1.domain.schedule.statistics.StatisticsScheduleDetails;
import com.weshare.api.v1.domain.schedule.statistics.StatisticsScheduleTotalCount;
import com.weshare.api.v1.event.schedule.ScheduleCreatedEvent;
import com.weshare.api.v1.event.schedule.ScheduleUpdatedEvent;
import com.weshare.api.v1.repository.schedule.ScheduleRepository;
import com.weshare.api.v1.repository.schedule.statistics.StatisticsScheduleDetailsRepository;
import com.weshare.api.v1.repository.schedule.statistics.StatisticsScheduleTotalCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StatisticsScheduleEventHandler {

    private final StatisticsScheduleDetailsRepository scheduleDetailsRepository;
    private final StatisticsScheduleTotalCountRepository scheduleTotalCountRepository;
    private final ScheduleRepository scheduleRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void scheduleCreatedEvent(ScheduleCreatedEvent createdEvent) {
        StatisticsScheduleDetails statisticsScheduleDetails = createStatisticsScheduleDetails(createdEvent);
        scheduleDetailsRepository.save(statisticsScheduleDetails);

        StatisticsScheduleTotalCount statisticsScheduleTotalCount = scheduleTotalCountRepository.findFirstByOrderByModifiedDate()
                .orElseGet(this::saveScheduleTotalCount);
        statisticsScheduleTotalCount.incrementTotalCount();
    }

    private StatisticsScheduleDetails createStatisticsScheduleDetails(ScheduleCreatedEvent createdEvent) {
        return StatisticsScheduleDetails.builder()
                .scheduleId(createdEvent.scheduleId())
                .totalExpense(createdEvent.totalExpense())
                .build();
    }

    private StatisticsScheduleTotalCount saveScheduleTotalCount() {
        return scheduleTotalCountRepository.save(new StatisticsScheduleTotalCount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleCreatedEvent(ScheduleUpdatedEvent updatedEvent) {
        final Long scheduleId = updatedEvent.scheduleId();
        final StatisticsScheduleDetails statisticsScheduleDetails = scheduleDetailsRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("통계테이블에서 수정한 여행일정이 없습니다."));

        final Schedule schedule = scheduleRepository.findScheduleDetailById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("수정한 여행일정을 찾지 못해 통계테이블을 업데이트할 수 없습니다."));

        statisticsScheduleDetails.updateScheduleTotalExpense(schedule.getTotalScheduleExpense());
    }
}