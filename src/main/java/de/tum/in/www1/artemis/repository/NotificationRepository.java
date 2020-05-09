package de.tum.in.www1.artemis.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.notification.Notification;

/**
 * Spring Data repository for the Notification entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select notification from Notification notification left join notification.course left join notification.recipient "
            + "where (notification.class = GroupNotification and ((notification.course.instructorGroupName in :#{#currentGroups} and notification.type = 'INSTRUCTOR') "
            + "or (notification.course.teachingAssistantGroupName in :#{#currentGroups} and notification.type = 'TA') "
            + "or (notification.course.studentGroupName in :#{#currentGroups} and notification.type = 'STUDENT')))"
            + "or notification.class = SingleUserNotification and notification.recipient.login = :#{#login}")
    Page<Notification> findAllNotificationsForRecipientWithLogin(@Param("currentGroups") Set<String> currentUserGroups, @Param("login") String login, Pageable pageable);

    @Query("select notification from Notification notification left join notification.course left join notification.recipient "
            + "where (:#{#lastNotificationRead} is null or notification.notificationDate > :#{#lastNotificationRead}) and "
            + "((notification.class = GroupNotification and ((notification.course.instructorGroupName in :#{#currentGroups} and notification.type = 'INSTRUCTOR') "
            + "or (notification.course.teachingAssistantGroupName in :#{#currentGroups} and notification.type = 'TA') "
            + "or (notification.course.studentGroupName in :#{#currentGroups} and notification.type = 'STUDENT')))"
            + "or notification.class = SingleUserNotification and notification.recipient.login = :#{#login})")
    List<Notification> findAllRecentNotificationsForRecipientWithLogin(@Param("currentGroups") Set<String> currentUserGroups, @Param("login") String login,
            @Param("lastNotificationRead") ZonedDateTime lastNotificationRead);

    @Query("select notification from Notification notification left join notification.course left join notification.recipient "
            + "where (:#{#lastNotificationRead} is null or notification.notificationDate <= :#{#lastNotificationRead}) and "
            + "((notification.class = GroupNotification and ((notification.course.instructorGroupName in :#{#currentGroups} and notification.type = 'INSTRUCTOR') "
            + "or (notification.course.teachingAssistantGroupName in :#{#currentGroups} and notification.type = 'TA') "
            + "or (notification.course.studentGroupName in :#{#currentGroups} and notification.type = 'STUDENT')))"
            + "or notification.class = SingleUserNotification and notification.recipient.login = :#{#login})")
    Page<Notification> findAllNonRecentNotificationsForRecipientWithLogin(@Param("currentGroups") Set<String> currentUserGroups, @Param("login") String login,
            @Param("lastNotificationRead") ZonedDateTime lastNotificationRead, Pageable pageable);
}
