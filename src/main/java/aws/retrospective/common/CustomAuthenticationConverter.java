package aws.retrospective.common;

import aws.retrospective.entity.User;
import aws.retrospective.repository.UserRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

@RequiredArgsConstructor
public class CustomAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;


    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        String tenantId = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("nickname");
        
        User user = getOrInsertUser(tenantId, email, username);

        return new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    }


    private User getOrInsertUser(String tenantId, String email, String username) {
        return userRepository.findByTenantId(tenantId)
            .orElseGet(() -> insertUser(tenantId, email, username));

    }

    private User insertUser(String tenantId, String email, String username) {
        User user = User.builder()
            .tenantId(tenantId)
            .email(email)
            .username(username)
            .build();

        return userRepository.save(user);
    }
}
