package aws.retrospective.service;

import static org.assertj.core.api.Assertions.assertThat;

import aws.retrospective.dto.CreateCommentDto;
import aws.retrospective.dto.GetNotificationResponseDto;
import aws.retrospective.entity.Notification;
import aws.retrospective.entity.NotificationType;
import aws.retrospective.entity.Retrospective;
import aws.retrospective.entity.RetrospectiveTemplate;
import aws.retrospective.entity.Section;
import aws.retrospective.entity.Team;
import aws.retrospective.entity.User;
import aws.retrospective.entity.NotificationRedis;
import aws.retrospective.entity.UserTeam;
import aws.retrospective.repository.NotificationRedisRepository;
import aws.retrospective.repository.NotificationRepository;
import aws.retrospective.repository.RetrospectiveRepository;
import aws.retrospective.repository.RetrospectiveTemplateRepository;
import aws.retrospective.repository.SectionRepository;
import aws.retrospective.repository.TeamRepository;
import aws.retrospective.repository.UserRepository;
import aws.retrospective.repository.UserTeamRepository;
import aws.retrospective.util.TestUtil;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    NotificationService notificationService;
    @Autowired
    CommentService commentService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    SectionRepository sectionRepository;
    @Autowired
    RetrospectiveRepository retrospectiveRepository;
    @Autowired
    RetrospectiveTemplateRepository templateRepository;
    @Autowired
    NotificationRedisRepository notificationRedisRepository;
    @Autowired
    SectionService sectionService;
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserTeamRepository userTeamRepository;
    @Autowired
    private RetrospectiveService retrospectiveService;

    @AfterEach
    public void tearDown() {
        notificationRedisRepository.deleteAll();
    }

    @Test
    @DisplayName("회고 보드에 댓글이 작성되면 알림을 생성 및 조회할 수 있다.")
    void createNotificationFromComment() {
        //given
        User senderUser = User.builder().username("sender").phone("010-1234-1234").email("test@naver.com")
            .build();
        User savedSenderUser = userRepository.save(senderUser);
        ReflectionTestUtils.setField(senderUser, "thumbnail", "121212-ababab");

        User receiverUser = TestUtil.createUser();
        User savedReceiverUser = userRepository.save(receiverUser);

        RetrospectiveTemplate template = RetrospectiveTemplate.builder().name("KPT").build();
        templateRepository.save(template);

        Retrospective retrospective = Retrospective.builder().title("title").user(savedReceiverUser)
            .template(template).build();
        retrospectiveRepository.save(retrospective);

        Section section = Section.builder().user(savedReceiverUser).retrospective(retrospective)
            .build();
        Section savedSection = sectionRepository.save(section);

        CreateCommentDto request = TestUtil.createCommentDto(section.getId());

        NotificationRedis redis = NotificationRedis.of("notification",
            LocalDateTime.now());// 마지막으로 알림이 전송된 시간을 레디스에 저장
        notificationRedisRepository.save(redis);

        //when
        commentService.createComment(senderUser, request); // 댓글 작성
        // 레디스에 저장된 시간 이후의 알림 조회
        List<GetNotificationResponseDto> notifications = notificationService.getNotifications();

        //then
        GetNotificationResponseDto notification = notifications.get(0);
        assertThat(notifications.size()).isEqualTo(1);
        assertThat(notification.getSectionId()).isEqualTo(savedSection.getId());
        assertThat(notification.getRetrospectiveTitle()).isEqualTo(
            retrospective.getTitle());
        assertThat(notification.getReceiverId()).isEqualTo(savedReceiverUser.getId());
        assertThat(notification.getReceiverId()).isNotEqualTo(savedSenderUser.getId());
        assertThat(notification.getSenderName()).isEqualTo(savedSenderUser.getUsername());
        assertThat(notification.getSenderName()).isNotEqualTo(savedReceiverUser.getUsername());
        assertThat(notification.getThumbnail()).isEqualTo(savedSenderUser.getThumbnail());
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.COMMENT);
    }

    @Test
    @DisplayName("회고 보드에 좋아요 이벤트가 발생하면 알림을 생성 및 조회할 수 있다.")
    void createNotificationFromLike() {
        //given
        User user = User.builder().username("test").phone("010-1234-1234").email("test@naver.com")
            .build();
        User savedUser = userRepository.save(user);

        RetrospectiveTemplate template = RetrospectiveTemplate.builder().name("KPT").build();
        templateRepository.save(template);

        Retrospective retrospective = Retrospective.builder().title("title").user(user)
            .template(template).build();
        retrospectiveRepository.save(retrospective);

        Section section = Section.builder().user(user).retrospective(retrospective)
            .build();
        Section savedSection = sectionRepository.save(section);

        NotificationRedis redis = NotificationRedis.of("notification",
            LocalDateTime.now());// 마지막으로 알림이 전송된 시간을 레디스에 저장
        notificationRedisRepository.save(redis);

        //when
        sectionService.increaseSectionLikes(savedSection.getId(), user); // 회고 보드에 좋아요 클릭
        // 레디스에 저장된 시간 이후의 알림 조회
        List<GetNotificationResponseDto> notifications = notificationService.getNotifications();

        //then
        GetNotificationResponseDto notification = notifications.get(0);
        assertThat(notifications.size()).isEqualTo(1);
        assertThat(notification.getSectionId()).isEqualTo(savedSection.getId());
        assertThat(notification.getRetrospectiveTitle()).isEqualTo(
            retrospective.getTitle());
        assertThat(notification.getReceiverId()).isEqualTo(savedUser.getId());
        assertThat(notification.getSenderName()).isEqualTo(savedUser.getUsername());
        assertThat(notification.getThumbnail()).isEqualTo(savedUser.getThumbnail());
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.LIKE);
    }

    @Test
    @DisplayName("알림을 읽으면 읽음 상태로 변경된다.")
    void readNotification() {
        //given
        User user = User.builder().username("test").phone("010-1234-1234").email("test@naver.com")
            .build();
        User savedUser = userRepository.save(user);

        Team team = TestUtil.createTeam();
        Team savedTeam = teamRepository.save(team);

        UserTeam userTeam = UserTeam.builder().user(savedUser).team(savedTeam).build();
        UserTeam savedUserTeam = userTeamRepository.save(userTeam);

        RetrospectiveTemplate template = RetrospectiveTemplate.builder().name("KPT").build();
        templateRepository.save(template);

        Retrospective retrospective = Retrospective.builder().title("title").user(user)
            .template(template).team(savedTeam).build();
        retrospectiveRepository.save(retrospective);

        Section section = Section.builder().user(user).retrospective(retrospective)
            .build();
        Section savedSection = sectionRepository.save(section);

        NotificationRedis redis = NotificationRedis.of("notification",
            LocalDateTime.now());// 마지막으로 알림이 전송된 시간을 레디스에 저장
        notificationRedisRepository.save(redis);

        CreateCommentDto request = TestUtil.createCommentDto(section.getId());
        commentService.createComment(savedUser, request); // 댓글 작성

        sectionService.increaseSectionLikes(savedSection.getId(), user); // 회고 보드에 좋아요 클릭

        //when
        List<GetNotificationResponseDto> unreadNotifications = notificationService.getNotifications();
        GetNotificationResponseDto unreadResponse = unreadNotifications.get(0);

        Long notificationId = unreadResponse.getNotificationId();
        Notification findNotification = notificationRepository.findById(notificationId)
            .orElse(null);
        findNotification.readNotification(); // 알림 읽음 처리

        // 알림 읽은 후 다시 조회
        List<GetNotificationResponseDto> readNotifications = notificationService.getNotifications();

        //then
        assertThat(unreadNotifications.size()).isEqualTo(2);

        GetNotificationResponseDto notification = readNotifications.get(0);
        assertThat(readNotifications.size()).isEqualTo(1);
        assertThat(notification.getSectionId()).isEqualTo(savedSection.getId());
        assertThat(notification.getRetrospectiveTitle()).isEqualTo(
            retrospective.getTitle());
        assertThat(notification.getReceiverId()).isEqualTo(savedUser.getId());
        assertThat(notification.getSenderName()).isEqualTo(savedUser.getUsername());
        assertThat(notification.getThumbnail()).isEqualTo(savedUser.getThumbnail());
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.LIKE);
        assertThat(notification.getTeamId()).isEqualTo(savedTeam.getId());
        assertThat(notification.getRetrospectiveId()).isEqualTo(retrospective.getId());
    }

    @Test
    @DisplayName("삭제된 회고 보드는 알림 조회에 포함하지 않는다.")
    void deleteRetrospectiveNotification() {
        //given
        User user = User.builder().username("test").phone("010-1234-1234").email("test@naver.com")
            .build();
        User savedUser = userRepository.save(user);

        RetrospectiveTemplate template = RetrospectiveTemplate.builder().name("KPT").build();
        templateRepository.save(template);

        Retrospective retrospective = Retrospective.builder().title("title").user(user)
            .template(template).build();
        retrospectiveRepository.save(retrospective);

        Section section = Section.builder().user(user).retrospective(retrospective)
            .build();
        Section savedSection = sectionRepository.save(section);

        NotificationRedis redis = NotificationRedis.of("notification",
            LocalDateTime.now());// 마지막으로 알림이 전송된 시간을 레디스에 저장
        notificationRedisRepository.save(redis);

        //when
        sectionService.increaseSectionLikes(savedSection.getId(), user); // 회고 보드에 좋아요 클릭

        retrospectiveService.deleteRetrospective(retrospective.getId(), user);

        // 레디스에 저장된 시간 이후의 알림 조회
        List<GetNotificationResponseDto> notifications = notificationService.getNotifications();

        //then
        assertThat(notifications.size()).isEqualTo(0);
    }
}