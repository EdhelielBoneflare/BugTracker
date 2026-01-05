package uni.bugtracker.backend.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import uni.bugtracker.backend.security.model.Role;

@JsonPropertyOrder({"username", "role", "type", "token"})
public class JwtResponse {
    @JsonProperty("token")
    private String token;
    @JsonProperty("type")
    private String type = "Bearer";
    @JsonProperty("username")
    private String username;
    @JsonProperty("role")
    private Role role;

    public JwtResponse(String token, String username, Role role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }
}
