package com.weshare.api.v1.event.schedule.statistics;

import com.weshare.api.v1.domain.schedule.statistics.StatisticsParentCommentTotalCount;
import com.weshare.api.v1.domain.schedule.statistics.StatisticsScheduleDetails;
import com.weshare.api.v1.event.schedule.CommentCreatedEvent;
import com.weshare.api.v1.event.schedule.CommentDeletedEvent;
import com.weshare.api.v1.repository.comment.CommentTotalCountRepository;
import com.weshare.api.v1.repository.schedule.statistics.StatisticsScheduleDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsCommentEventHandler {
    private static final long DEFAULT_TOTAL_COUNT = 0L;
    private final CommentTotalCountRepository commentTotalCountRepository;
    private final StatisticsScheduleDetailsRepository scheduleDetailsRepository;

    @EventListener
    @Transactional
    @Async
    public void incrementCommentTotalCount(CommentCreatedEvent createdEvent) {
        if (createdEvent.parentCommentId() == null) {
            return;
        }

        final Long parentCommentId = createdEvent.parentCommentId();
        final StatisticsParentCommentTotalCount statisticsParentCommentTotalCount = commentTotalCountRepository.findByParentCommentId(parentCommentId)
                .orElse(new StatisticsParentCommentTotalCount(parentCommentId, DEFAULT_TOTAL_COUNT));

        statisticsParentCommentTotalCount.incrementTotalCount();
        commentTotalCountRepository.save(statisticsParentCommentTotalCount);
    }

    @EventListener
    @Transactional
    @Async
    public void incrementScheduleTotalCommentCount(CommentCreatedEvent createdEvent) {
        StatisticsScheduleDetails statisticsScheduleDetails = scheduleDetailsRepository.findByScheduleId(createdEvent.scheduleId())
                .orElseThrow(StatisticsScheduleNotFound::new);

        statisticsScheduleDetails.incrementTotalCommentCount();
    }

    @EventListener
    @Transactional
    @Async
    public void decrementCommentTotalCount(CommentDeletedEvent deletedEvent) {
        if (deletedEvent.parentCommentId() == null && deletedEvent.deletedCommentCount() > 1) {
            StatisticsParentCommentTotalCount statisticsParentCommentTotalCount = commentTotalCountRepository.findByParentCommentId(deletedEvent.commentId())
                    .orElseThrow(StatisticsCommentTotalCountNotFound::new);

            commentTotalCountRepository.delete(statisticsParentCommentTotalCount);
            return;
        }
        if (deletedEvent.parentCommentId() != null) {
            final StatisticsParentCommentTotalCount statisticsParentCommentTotalCount = commentTotalCountRepository.findByParentCommentId(deletedEvent.parentCommentId())
                    .orElseThrow(StatisticsCommentTotalCountNotFound::new);
            statisticsParentCommentTotalCount.decrementTotalCount();
        }
    }

    @EventListener
    @Transactional
    @Async
    public void decrementScheduleTotalCommentCount(CommentDeletedEvent deletedEvent) {
        final StatisticsScheduleDetails statisticsScheduleDetails = scheduleDetailsRepository.findByScheduleId(deletedEvent.scheduleId())
                .orElseThrow(StatisticsScheduleNotFound::new);

        statisticsScheduleDetails.decrementTotalCount(deletedEvent.deletedCommentCount());
    }

}