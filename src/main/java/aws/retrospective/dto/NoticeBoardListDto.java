package aws.retrospective.dto;

import aws.retrospective.entity.NoticeBoardWriting;
import aws.retrospective.entity.SaveStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeBoardListDto {
    private Long id;
    private String title;
    private String content;
    private SaveStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private int views;
    private UUID thumbnail;

    public NoticeBoardListDto(NoticeBoardWriting entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.content = entity.getContent();
        this.status = entity.getStatus();
        this.createdDate = entity.getCreatedDate();
        this.modifiedDate = entity.getModifiedDate();
        this.views = 0; // 기본 조회수 초기화
        this.thumbnail = entity.getThumbnail();
    }

    public NoticeBoardListDto(NoticeBoardWriting entity, int views) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.content = entity.getContent();
        this.status = entity.getStatus();
        this.createdDate = entity.getCreatedDate();
        this.modifiedDate = entity.getModifiedDate();
        this.views = views;
        this.thumbnail = entity.getThumbnail();
    }
}
