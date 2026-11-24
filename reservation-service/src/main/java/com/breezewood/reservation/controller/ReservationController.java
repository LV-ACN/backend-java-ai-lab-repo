package com.breezewood.reservation.controller;

import com.breezewood.reservation.model.ReservationRequest;
import com.breezewood.reservation.model.ReservationResponse;
import com.breezewood.reservation.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> book(@RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.book(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{reservationNumber}")
    public ReservationResponse find(@PathVariable String reservationNumber) {
        return reservationService.find(reservationNumber);
    }
}
