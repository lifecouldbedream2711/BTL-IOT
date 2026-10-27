package BTL.IOT.Service;

import BTL.IOT.Dto.response.UserProfileDto;
import BTL.IOT.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileDto getUserProfile() {
        return UserProfileDto.builder()
                .name("Phạm Ngọc Quynh")
                .avatar("/avatar.png")
                .fields(List.of(
                        Map.of("label", "Full name", "value", "Phạm Ngọc Quynh"),
                        Map.of("label", "Email", "value", "phampt987321@gmail.com"),
                        Map.of("label", "Student ID", "value", "B23DCCN704"),
                        Map.of("label", "University", "value", "PTIT"),
                        Map.of("label", "Location", "value", "Hanoi, Vietnam"),
                        Map.of("label", "Member since", "value", "September 2023")
                ))
                .build();
    }
}
