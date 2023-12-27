package meli.bootcamp.sprint1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class UserWFollowedDto {
    private Integer user_id;
    private String user_name;
    private List<UserDto> followed;
}
