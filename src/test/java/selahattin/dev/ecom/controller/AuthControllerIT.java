package selahattin.dev.ecom.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import selahattin.dev.ecom.AbstractIntegrationTest;
import selahattin.dev.ecom.dto.request.SigninRequest;
import selahattin.dev.ecom.dto.request.SignupRequest;
import selahattin.dev.ecom.dto.request.VerifyEmailRequest;
import selahattin.dev.ecom.dto.request.VerifyOtpRequest;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.repository.auth.RoleRepository;
import selahattin.dev.ecom.repository.auth.UserRepository;
import selahattin.dev.ecom.utils.constant.AuthConstant;
import selahattin.dev.ecom.utils.enums.Role;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Ensure roles exist
        if (roleRepository.findByName(Role.USER.name()).isEmpty()) {
            RoleEntity role = new RoleEntity();
            role.setName(Role.USER.name());
            roleRepository.save(role);
        }
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        // Clear Redis keys used in tests
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    void signup_ShouldReturnSuccess_WhenRequestIsValid() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setEmail("integration@test.com");
        request.setFirstName("Integration");
        request.setLastName("Test");

        mockMvc.perform(post("/api/v1/auth/public/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", containsString("Kayıt Başarılı")));

        // Check if user is saved
        assertThat(userRepository.findByEmailAndDeletedAtIsNull("integration@test.com")).isPresent();
    }

    @Test
    void signinFlow_ShouldReturnTokens_WhenOtpIsCorrect() throws Exception {
        // 1. Create User
        RoleEntity role = roleRepository.findByName(Role.USER.name()).orElseThrow();
        UserEntity user = new UserEntity();
        user.setEmail("login@test.com");
        user.setFirstName("Login");
        user.setLastName("User");
        user.setRoles(new HashSet<>(List.of(role)));
        user.setEmailVerifiedAt(OffsetDateTime.now());
        userRepository.save(user);

        // 2. Request OTP
        SigninRequest signinRequest = new SigninRequest();
        signinRequest.setEmail("login@test.com");

        mockMvc.perform(post("/api/v1/auth/public/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signinRequest)))
                .andExpect(status().isOk());

        // 3. Get OTP from Redis (Simulate user checking email)
        String otpKey = AuthConstant.OTP_KEY_TEMPLATE + "login@test.com";
        String otpCode = (String) redisTemplate.opsForValue().get(otpKey);
        assertThat(otpCode).isNotNull();

        // 4. Verify OTP
        VerifyOtpRequest verifyRequest = new VerifyOtpRequest();
        verifyRequest.setEmail("login@test.com");
        verifyRequest.setCode(otpCode);

        mockMvc.perform(post("/api/v1/auth/public/signin-verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("login@test.com"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    void signupVerify_ShouldActivateUser() throws Exception {
        // 1. Manually create unverified user
        UserEntity user = new UserEntity();
        user.setEmail("verify@test.com");
        user.setFirstName("Verify");
        user.setLastName("Me");
        userRepository.save(user);

        // 2. Manually set token in Redis
        String token = "test-token-123";
        redisTemplate.opsForValue().set(AuthConstant.SIGNUP_KEY_TEMPLATE + token, "verify@test.com");

        // 3. Call verify endpoint
        VerifyEmailRequest verifyRequest = new VerifyEmailRequest();
        verifyRequest.setToken(token);

        mockMvc.perform(post("/api/v1/auth/public/signup-verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        // 4. Assert user is active
        UserEntity updatedUser = userRepository.findByEmailAndDeletedAtIsNull("verify@test.com").orElseThrow();
        assertThat(updatedUser.getDeletedAt()).isNull();
        assertThat(updatedUser.getEmailVerifiedAt()).isNotNull();
    }
}
