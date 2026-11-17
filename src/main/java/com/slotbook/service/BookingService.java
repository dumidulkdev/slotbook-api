package com.slotbook.service;

import com.slotbook.domain.BookingEntity;
import com.slotbook.domain.BookingStatus;
import com.slotbook.domain.Role;
import com.slotbook.domain.UserEntity;
import com.slotbook.dto.booking.BookingResponse;
import com.slotbook.dto.booking.CreateBookingRequest;
import com.slotbook.exception.ConflictException;
import com.slotbook.exception.ForbiddenException;
import com.slotbook.exception.ResourceNotFoundException;
import com.slotbook.mapper.BookingMapper;
import com.slotbook.repository.BookingRepository;
import com.slotbook.repository.ProviderAvailabilityRepository;
import com.slotbook.repository.UserRepository;
import com.slotbook.security.UserPrincipal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final SlotGenerationService slotGenerationService;
    private final BookingRules bookingRules;
    private final BookingMapper bookingMapper;
    private final Clock clock;
    private final ZoneId zoneId;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          ProviderAvailabilityRepository availabilityRepository,
                          SlotGenerationService slotGenerationService,
                          BookingRules bookingRules,
                          BookingMapper bookingMapper,
                          Clock clock,
                          com.slotbook.config.AppProperties appProperties) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.availabilityRepository = availabilityRepository;
        this.slotGenerationService = slotGenerationService;
        this.bookingRules = bookingRules;
        this.bookingMapper = bookingMapper;
        this.clock = clock;
        this.zoneId = appProperties.zoneId();
    }

    @Transactional
    public BookingResponse createBooking(UserPrincipal principal, CreateBookingRequest request) {
        if (principal.getRole() != Role.CLIENT) {
            throw new ForbiddenException("Only clients can create bookings");
        }
        UserEntity client = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        UserEntity provider = userRepository.findById(request.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        if (provider.getRole() != Role.PROVIDER) {
            throw new ResourceNotFoundException("Provider not found");
        }
        var availability = availabilityRepository.findByProviderId(provider.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider availability not configured"));

        LocalDateTime now = LocalDateTime.now(clock.withZone(zoneId));
        if (!request.slotStart().isAfter(now)) {
            throw new ConflictException("Slot must be in the future");
        }
        if (!slotGenerationService.matchesAvailability(availability, request.slotStart())) {
            throw new ConflictException("Selected slot does not match provider availability");
        }
        if (bookingRepository.findByProviderIdAndStartTimeAndStatus(
                provider.getId(), request.slotStart(), BookingStatus.BOOKED).isPresent()) {
            throw new ConflictException("This slot is no longer available");
        }

        LocalDateTime end = request.slotStart().plusMinutes(availability.getSlotLengthMinutes());
        BookingEntity booking = new BookingEntity(provider, client, request.slotStart(), end);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> myBookings(UserPrincipal principal, Pageable pageable) {
        return bookingRepository.findMine(principal.getId(), pageable).map(bookingMapper::toResponse);
    }

    @Transactional
    public BookingResponse cancel(UserPrincipal principal, Long bookingId) {
        BookingEntity booking = loadBooking(bookingId);
        assertParticipant(principal, booking);
        bookingRules.assertCancellable(booking);
        bookingRules.transition(booking, BookingStatus.CANCELLED);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse complete(UserPrincipal principal, Long bookingId) {
        BookingEntity booking = loadBooking(bookingId);
        assertProviderOwner(principal, booking);
        bookingRules.assertProviderOutcomeAllowed(booking);
        bookingRules.transition(booking, BookingStatus.COMPLETED);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse noShow(UserPrincipal principal, Long bookingId) {
        BookingEntity booking = loadBooking(bookingId);
        assertProviderOwner(principal, booking);
        bookingRules.assertProviderOutcomeAllowed(booking);
        bookingRules.transition(booking, BookingStatus.NO_SHOW);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    private BookingEntity loadBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private void assertParticipant(UserPrincipal principal, BookingEntity booking) {
        boolean isClient = booking.getClient().getId().equals(principal.getId());
        boolean isProvider = booking.getProvider().getId().equals(principal.getId());
        if (!isClient && !isProvider) {
            throw new ForbiddenException("You can only manage your own bookings");
        }
    }

    private void assertProviderOwner(UserPrincipal principal, BookingEntity booking) {
        if (principal.getRole() != Role.PROVIDER
                || !booking.getProvider().getId().equals(principal.getId())) {
            throw new ForbiddenException("Only the owning provider can update this booking");
        }
    }
}