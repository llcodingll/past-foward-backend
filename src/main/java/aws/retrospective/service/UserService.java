package aws.retrospective.service;

import aws.retrospective.dto.GetUserInfoDto;
import aws.retrospective.dto.UpdateUserProfileRequestDto;
import aws.retrospective.dto.UpdateUserProfileResponseDto;
import aws.retrospective.entity.User;
import aws.retrospective.repository.UserRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UpdateUserProfileResponseDto updateProfile(User user,
        UpdateUserProfileRequestDto request) {
        User findUser = getUser(user.getId());
        findUser.updateUserInfo(request.getThumbnail(), request.getUsername());
        return new UpdateUserProfileResponseDto(findUser.getId(), findUser.getEmail(),
            findUser.getThumbnail(), findUser.getUsername());
    }

    @Transactional(readOnly = true)
    public GetUserInfoDto getUserInfo(User user) {
        User currentUser = getUser(user.getId());
        return new GetUserInfoDto(currentUser.getId(), currentUser.getUsername(),
            currentUser.getEmail(), currentUser.getThumbnail(), currentUser.getPhone(),
            currentUser.getCreatedDate(), currentUser.getUpdatedDate());
    }


    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("사용자를 조회할 수 없습니다. id = " + userId));
    }
}
