package com.werewolfengine.room;



import com.werewolfengine.game.engine.GameStateMachine;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;



import java.util.LinkedHashMap;

import java.util.List;

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

        CreateRoomCommand cmd = command != null ? command : new CreateRoomCommand(null, null, 0);

        int aiCount = cmd.aiCount() != null ? cmd.aiCount() : 0;

        try {

            RoomService.RoomSnapshot snap = roomService.createRoom(cmd.roomId(), cmd.hostUserId(), aiCount);

            return roomResponse(snap);

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());

        }

    }



    @PostMapping("/{roomId}/join")

    public Map<String, Object> join(

            @PathVariable String roomId,

            @RequestBody JoinRoomCommand command

    ) {

        try {

            RoomService.SeatSnapshot seat = roomService.joinRoom(roomId, command.seatId(), command.userId());

            return seatResponse(seat);

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (IllegalStateException e) {

            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());

        }

    }



    @PostMapping("/{roomId}/ready")

    public Map<String, Object> ready(

            @PathVariable String roomId,

            @RequestBody ReadyCommand command

    ) {

        try {

            RoomService.SeatSnapshot seat = roomService.setReady(roomId, command.seatId(), command.ready());

            return seatResponse(seat);

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (IllegalStateException e) {

            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());

        }

    }



    @PostMapping("/{roomId}/leave")

    public Map<String, Object> leave(

            @PathVariable String roomId,

            @RequestBody LeaveRoomCommand command

    ) {

        try {

            RoomService.SeatSnapshot seat = roomService.leaveRoom(roomId, command.seatId(), command.userId());

            return seatResponse(seat);

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (IllegalStateException e) {

            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());

        }

    }



    @DeleteMapping("/{roomId}")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void dissolve(

            @PathVariable String roomId,

            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,

            @RequestBody(required = false) DissolveRoomCommand command

    ) {

        Long requester = command != null && command.userId() != null ? command.userId() : headerUserId;

        try {

            roomService.dissolveRoom(roomId, requester);

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (IllegalStateException e) {

            throw mapIllegalState(e);

        }

    }



    @PostMapping("/{roomId}/phase-tick")

    public Map<String, Object> phaseTick(@PathVariable String roomId) {

        return roomService.tickPhase(roomId);

    }



    @PostMapping("/{roomId}/start")

    public Map<String, Object> start(

            @PathVariable String roomId,

            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,

            @RequestBody(required = false) StartRoomCommand command

    ) {

        Long requester = command != null && command.userId() != null ? command.userId() : headerUserId;

        try {

            GameStateMachine.StartGameResult result = roomService.startRoom(roomId, requester);

            if (!result.success()) {

                return Map.of(

                        "success", false,

                        "code", result.errorCode() != null ? result.errorCode().name() : "ERROR",

                        "message", result.message()

                );

            }

            return Map.of(

                    "success", true,

                    "phase", result.phase(),

                    "phaseSyncs", result.phaseSyncs()

            );

        } catch (IllegalStateException e) {

            throw mapIllegalState(e);

        }

    }



    @GetMapping("/{roomId}")

    public Map<String, Object> snapshot(@PathVariable String roomId) {

        try {

            return roomResponse(roomService.snapshot(roomId));

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());

        }

    }



    private static ResponseStatusException mapIllegalState(IllegalStateException e) {

        String msg = e.getMessage() != null ? e.getMessage() : "conflict";

        if (msg.contains("not in WAITING") || msg.contains("not waiting")) {

            return new ResponseStatusException(HttpStatus.CONFLICT, msg);

        }

        if (msg.contains("Only host")) {

            return new ResponseStatusException(HttpStatus.FORBIDDEN, msg);

        }

        return new ResponseStatusException(HttpStatus.CONFLICT, msg);

    }



    private static Map<String, Object> seatResponse(RoomService.SeatSnapshot seat) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("roomId", seat.roomId());

        body.put("seatId", seat.seatId());

        body.put("playerId", seat.seatId());

        body.put("userId", seat.userId());

        body.put("ready", seat.ready());

        body.put("phase", seat.phase().name());

        body.put("aiSeat", seat.aiSeat());

        return body;

    }



    private static Map<String, Object> roomResponse(RoomService.RoomSnapshot snap) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("roomId", snap.roomId());

        body.put("status", snap.status());

        body.put("phase", snap.phase());

        body.put("round", snap.round());

        body.put("maxPlayers", snap.maxPlayers());

        body.put("aiCount", snap.aiCount());

        body.put("hostUserId", snap.hostUserId());

        body.put("readyCount", snap.readyCount());

        body.put("humanCount", snap.humanCount());

        body.put("seats", snap.seats().stream().map(RoomController::seatResponse).toList());

        return body;

    }



    public record CreateRoomCommand(String roomId, Long hostUserId, Integer aiCount) {

    }



    public record JoinRoomCommand(Integer seatId, Long userId) {

    }



    public record ReadyCommand(int seatId, boolean ready) {

    }



    public record LeaveRoomCommand(int seatId, Long userId) {

    }



    public record DissolveRoomCommand(Long userId) {

    }



    public record StartRoomCommand(Long userId) {

    }

}


