package com.weshare.api.v1.repository.schedule;

import com.weshare.api.v1.domain.schedule.comment.Comment;
import com.weshare.api.v1.domain.schedule.like.ScheduleLike;
import com.weshare.api.v1.domain.schedule.*;
import com.weshare.api.v1.domain.user.Role;
import com.weshare.api.v1.domain.user.Social;
import com.weshare.api.v1.domain.user.User;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@SpringBootTest
@ActiveProfiles(value = {"test", "init"})
public abstract class ScheduleTestSupport {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ScheduleRepository repository;

    @Transactional
    public User createUserAndSave(String email, String name, String password) {
        User user = User.builder()
                .email(email)
                .name(name)
                .password(password)
                .profileImg("profile")
                .role(Role.USER)
                .social(Social.DEFAULT)
                .build();
        entityManager.persist(user);
        return user;
    }

    @Transactional
    public Schedule createAndSaveSchedule(String title, Destination destination, User user) {
        Schedule schedule = Schedule.builder()
                .title(title)
                .user(user)
                .destination(destination)
                .days(createDays())
                .build();
        schedule.initDays();
        return repository.save(schedule);
    }

    private Days createDays() {
        List<Day> days = List.of(
                createDay(LocalDate.of(2024, 12, 3)),
                createDay(LocalDate.of(2024, 12, 4)),
                createDay(LocalDate.of(2024, 12, 5))
        );
        return new Days(days, LocalDate.of(2024, 12, 3), LocalDate.of(2024, 12, 5));
    }

    private Day createDay(LocalDate travelDate) {
        return Day.builder()
                .travelDate(travelDate)
                .places(createPlaces())
                .build();
    }

    private List<Place> createPlaces() {
        return List.of(
                createPlace("지역1", "지역 1입니다", 1000),
                createPlace("지역2", "지역 2입니다", 1000),
                createPlace("지역3", "지역 3입니다", 1000)
        );
    }

    private Place createPlace(String title, String memo, long expense) {
        return Place.builder()
                .title(title)
                .time(LocalTime.of(12, 00))
                .memo(memo)
                .expense(new Expense(expense))
                .location(createLocation())
                .build();
    }

    private Location createLocation() {
        return new Location(152.64, 123.67);
    }

    @Transactional
    public ScheduleLike createAndSaveLike(Long scheduleId, Long userId) {
        Schedule schedule = entityManager.find(Schedule.class, scheduleId);
        User user = entityManager.find(User.class, userId);
        ScheduleLike scheduleLike = ScheduleLike.builder()
                .liker(user)
                .scheduleId(schedule.getId())
                .build();
        entityManager.persist(scheduleLike);
        return scheduleLike;
    }

    @Transactional
    public Comment createAndSaveComment(Long scheduleId, Long userId) {
        Schedule schedule = entityManager.find(Schedule.class, scheduleId);
        User user = entityManager.find(User.class, userId);
        Comment comment = Comment.builder()
                .content("메롱")
                .commenter(user)
                .scheduleId(schedule.getId())
                .build();
        entityManager.persist(comment);
        return comment;
    }

    protected ScheduleIds getIdsAndSaveSchedule() {
        Long scheduleIdFirst = null;
        Long scheduleIdLast = null;
        for (int i = 1; i <= 2; i++) {
            User user = createUserAndSave(i + "test@asd.com", "test" + i, "test");
            Destination destination = Destination.SEOUL;
            String title = "제목" + i;
            Schedule schedule = createAndSaveSchedule(title, destination, user);
            if (i == 1) {
                scheduleIdFirst = schedule.getId();
            } else {
                scheduleIdLast = schedule.getId();
            }
            createAndSaveLike(schedule.getId(), user.getId());
            createAndSaveComment(schedule.getId(), user.getId());
        }
        return new ScheduleIds(scheduleIdFirst, scheduleIdLast);
    }
    public record ScheduleIds(Long scheduleIdFirst, Long scheduleIdLast) {
    }
}
