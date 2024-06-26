package com.weshare.api.v1.domain.schedule.statistics;

import com.weshare.api.v1.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatisticsScheduleDetails extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long scheduleId;
    @Column(columnDefinition = "integer default 0", nullable = false)
    private int totalViewCount;
    @Column(columnDefinition = "integer default 0", nullable = false)
    private int totalCommentCount;
    @Column(columnDefinition = "integer default 0", nullable = false)
    private int totalLikeCount;
    @Column(columnDefinition = "bigint default 0", nullable = false)
    private long totalExpense;

    @Builder
    private StatisticsScheduleDetails(Long scheduleId, int totalViewCount, int totalCommentCount, int totalLikeCount, long totalExpense) {
        this.scheduleId = scheduleId;
        this.totalViewCount = totalViewCount;
        this.totalCommentCount = totalCommentCount;
        this.totalLikeCount = totalLikeCount;
        this.totalExpense = totalExpense;
    }

    public StatisticsScheduleDetails(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public StatisticsScheduleDetails(int totalViewCount, int totalCommentCount, int totalLikeCount, long totalExpense) {
        this.totalViewCount = totalViewCount;
        this.totalCommentCount = totalCommentCount;
        this.totalLikeCount = totalLikeCount;
        this.totalExpense = totalExpense;
    }

    public void updateScheduleTotalExpense(long totalExpense) {
        if (totalExpense == 0) {
            throw new IllegalStateException("통계테이블에 업데이트할 총 금액이 존재하지 않습니다.");
        }
        this.totalExpense = totalExpense;
    }

    public void incrementTotalCommentCount() {
        this.totalCommentCount += 1;
    }

    public void decrementTotalCount(int deletedCount) {
        if (totalCommentCount - deletedCount <= 0) {
            throw new IllegalStateException("총 카운트 수는 음수일 수 없습니다.");
        }
        totalCommentCount -= deletedCount;
    }

    public void incrementTotalLikeCount() {
        this.totalLikeCount += 1;
    }

    public void decrementTotalLikeCount() {
        if (totalCommentCount - 1 <= 0) {
            throw new IllegalStateException("총 카운트 수는 음수일 수 없습니다.");
        }
        totalCommentCount -= 1;
    }
}
