package com.werewolfengine.room;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/room")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody(required = false) CreateRoomCommand command) {
        String roomId = command != null ? command.roomId() : null;
        RoomService.RoomSnapshot snap = roomService.createRoom(roomId);
        return Map.of(
                "roomId", snap.roomId(),
                "status", snap.status(),
                "phase", snap.phase(),
                "round", snap.round()
        );
    }

    @PostMapping("/{roomId}/join")
    public Map<String, Object> join(
            @PathVariable String roomId,
            @RequestBody JoinRoomCommand command
    ) {
        RoomService.SeatSnapshot seat = roomService.joinRoom(roomId, command.seatId(), command.userId());
        return Map.of(
                "roomId", seat.roomId(),
                "seatId", seat.seatId(),
                "userId", seat.userId(),
                "ready", seat.ready(),
                "phase", seat.phase()
        );
    }

    @PostMapping("/{roomId}/ready")
    public Map<String, Object> ready(
            @PathVariable String roomId,
            @RequestBody ReadyCommand command
    ) {
        RoomService.SeatSnapshot seat = roomService.setReady(roomId, command.seatId(), command.ready());
        return Map.of(
                "roomId", seat.roomId(),
                "seatId", seat.seatId(),
                "userId", seat.userId(),
                "ready", seat.ready(),
                "phase", seat.phase()
        );
    }

    @PostMapping("/{roomId}/start")
    public Map<String, Object> start(@PathVariable String roomId) {
        var result = roomService.startRoom(roomId);
        if (!result.success()) {
            return Map.of(
                    "success", false,
                    "code", result.errorCode(),
                    "message", result.message()
            );
        }
        return Map.of(
                "success", true,
                "phase", result.phase(),
                "phaseSyncs", result.phaseSyncs()
        );
    }

    @GetMapping("/{roomId}")
    public Map<String, Object> snapshot(@PathVariable String roomId) {
        RoomService.RoomSnapshot snap = roomService.snapshot(roomId);
        return Map.of(
                "roomId", snap.roomId(),
                "status", snap.status(),
                "phase", snap.phase(),
                "round", snap.round()
        );
    }

    public record CreateRoomCommand(String roomId) {
    }

    public record JoinRoomCommand(int seatId, Long userId) {
    }

    public record ReadyCommand(int seatId, boolean ready) {
    }
}
