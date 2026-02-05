package selahattin.dev.ecom.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import selahattin.dev.ecom.AbstractIntegrationTest;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.repository.auth.RoleRepository;
import selahattin.dev.ecom.repository.auth.UserRepository;
import selahattin.dev.ecom.repository.order.OrderRepository;
import selahattin.dev.ecom.repository.payment.PaymentRepository;
import selahattin.dev.ecom.security.jwt.JwtTokenProvider;
import selahattin.dev.ecom.utils.enums.OrderStatus;
import selahattin.dev.ecom.utils.enums.PaymentProvider;
import selahattin.dev.ecom.utils.enums.Role;

class PaymentControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String userToken;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        // 1. User
        RoleEntity role = roleRepository.findByName(Role.USER.name())
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name(Role.USER.name()).build()));

        UserEntity user = new UserEntity();
        user.setEmail("payer@test.com");
        user.setFirstName("Payer");
        user.setLastName("User");
        user.setRoles(new HashSet<>(List.of(role)));
        userRepository.save(user);

        userToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId().toString(),
                user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toList()),
                "device-1");

        // 2. Order (Pending)
        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(150.00));
        order.setShippingAddress(Map.of("city", "Istanbul")); // Dummy jsonb
        orderRepository.save(order);
        orderId = order.getId();
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void initPayment_ShouldCreatePaymentRecord_WhenOrderIsValid() throws Exception {
        PaymentInitRequest request = new PaymentInitRequest();
        request.setOrderId(orderId);
        request.setProvider(PaymentProvider.MOCK); // Use Mock Provider in integration test

        mockMvc.perform(post("/api/v1/payments/init")
                .cookie(new Cookie("accessToken", userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").isNotEmpty());

        // Assert Payment Entity Created
        assertThat(paymentRepository.count()).isEqualTo(1);
    }
}
