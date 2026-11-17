package com.slotbook;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotbook.domain.Role;
import com.slotbook.dto.auth.LoginRequest;
import com.slotbook.dto.auth.RegisterRequest;
import com.slotbook.dto.availability.AvailabilityRequest;
import com.slotbook.dto.availability.WorkingWindowRequest;
import com.slotbook.dto.booking.CreateBookingRequest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SlotBookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unauthenticatedProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/bookings/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void clientCannotSetAvailability() throws Exception {
        String token = registerAndGetToken("Client Only", Role.CLIENT);
        AvailabilityRequest request = sampleAvailability();

        mockMvc.perform(put("/api/providers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void fullBookingFlow() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String providerEmail = "flow-provider-" + unique + "@slotbook.dev";
        String clientEmail = "flow-client-" + unique + "@slotbook.dev";

        MvcResult providerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Flow Provider", providerEmail, "Password123!", Role.PROVIDER))))
                .andExpect(status().isCreated())
                .andReturn();
        String providerToken = readToken(providerResult);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Flow Client", clientEmail, "Password123!", Role.CLIENT))))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/providers/me/availability")
                        .header("Authorization", "Bearer " + providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAvailability())))
                .andExpect(status().isOk());

        long providerId = readUserId(providerResult);
        LocalDate date = nextWeekday();
        MvcResult slotsResult = mockMvc.perform(get("/api/providers/{id}/slots", providerId)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(org.hamcrest.Matchers.greaterThan(0))))
                .andReturn();

        String slotStart = objectMapper.readTree(slotsResult.getResponse().getContentAsString())
                .get("data").get(0).get("start").asText();

        MvcResult clientLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(clientEmail, "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        String clientToken = readToken(clientLogin);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(providerId, LocalDateTime.parse(slotStart)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BOOKED"));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(providerId, LocalDateTime.parse(slotStart)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        MvcResult bookingList = mockMvc.perform(get("/api/bookings/me")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andReturn();
        long bookingId = objectMapper.readTree(bookingList.getResponse().getContentAsString())
                .get("data").get("content").get(0).get("id").asLong();

        mockMvc.perform(patch("/api/bookings/{id}/cancel", bookingId)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/providers/{id}/slots", providerId)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.start == '" + slotStart + "')]").exists());
    }

    @Test
    void concurrentBookingResultsInSingleSuccess() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        RegisterRequest providerReg = new RegisterRequest(
                "Concurrent Provider " + unique,
                "provider-" + unique + "@slotbook.dev",
                "Password123!",
                Role.PROVIDER);
        RegisterRequest clientReg = new RegisterRequest(
                "Concurrent Client A " + unique,
                "client-a-" + unique + "@slotbook.dev",
                "Password123!",
                Role.CLIENT);
        RegisterRequest clientBReg = new RegisterRequest(
                "Concurrent Client B " + unique,
                "client-b-" + unique + "@slotbook.dev",
                "Password123!",
                Role.CLIENT);

        MvcResult providerMvc = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(providerReg)))
                .andExpect(status().isCreated())
                .andReturn();
        String providerToken = readToken(providerMvc);
        long providerId = readUserId(providerMvc);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientReg)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientBReg)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/providers/me/availability")
                        .header("Authorization", "Bearer " + providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAvailability())))
                .andExpect(status().isOk());

        LocalDate date = nextWeekday();
        MvcResult slotsResult = mockMvc.perform(get("/api/providers/{id}/slots", providerId)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andReturn();
        String slotStart = objectMapper.readTree(slotsResult.getResponse().getContentAsString())
                .get("data").get(0).get("start").asText();
        CreateBookingRequest bookingRequest = new CreateBookingRequest(providerId, LocalDateTime.parse(slotStart));
        String bookingJson = objectMapper.writeValueAsString(bookingRequest);

        String clientAToken = loginToken("client-a-" + unique + "@slotbook.dev");
        String clientBToken = loginToken("client-b-" + unique + "@slotbook.dev");

        int threads = 12;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            String token = i % 2 == 0 ? clientAToken : clientBToken;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/bookings")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(bookingJson))
                            .andReturn();
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        success.incrementAndGet();
                    } else if (status == 409) {
                        conflict.incrementAndGet();
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        org.assertj.core.api.Assertions.assertThat(success.get()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(conflict.get()).isEqualTo(threads - 1);
    }

    private String registerAndGetToken(String name, Role role) throws Exception {
        String email = name.toLowerCase().replace(' ', '-') + "@slotbook.dev";
        RegisterRequest request = new RegisterRequest(name, email, "Password123!", role);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return readToken(result);
    }

    private String loginToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        return readToken(result);
    }

    private String readToken(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("token").asText();
    }

    private long readUserId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("userId").asLong();
    }

    private AvailabilityRequest sampleAvailability() {
        return new AvailabilityRequest(30, List.of(
                new WorkingWindowRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                new WorkingWindowRequest(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                new WorkingWindowRequest(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                new WorkingWindowRequest(DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                new WorkingWindowRequest(DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))));
    }

    private LocalDate nextWeekday() {
        LocalDate date = LocalDate.now().plusDays(3);
        while (date.getDayOfWeek().getValue() > 5) {
            date = date.plusDays(1);
        }
        return date;
    }
}