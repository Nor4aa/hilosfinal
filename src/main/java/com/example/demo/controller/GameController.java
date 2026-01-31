package com.example.demo.controller;

import com.example.demo.engine.ActiveRoom;
import com.example.demo.engine.GameEngine;
import com.example.demo.model.Question;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/game")
public class GameController {

    @Autowired
    private GameEngine gameEngine;

    // HOST ACTIONS

    @PostMapping("/{pin}/start")
    @ResponseBody
    public String startGame(@PathVariable String pin) {
        gameEngine.startGame(pin);
        return "OK";
    }

    @GetMapping("/host/data")
    @ResponseBody
    public Map<String, Object> getHostData(@RequestParam String pin) {
        ActiveRoom room = gameEngine.getRoom(pin);
        if (room == null)
            return Map.of("status", "FINISHED");

        Question q = room.getCurrentQuestion();

        return Map.of(
                "questionIndex", room.getCurrentQuestionIndex(),
                "question", q != null ? q : "End of Game",
                "ranking", room.getRanking(),
                "isOpen", room.isQuestionOpen());
    }

    // PLAYER ACTIONS

    @GetMapping("/player")
    public String playerView(HttpSession session, Model model) {
        String pin = (String) session.getAttribute("pin");
        String playerName = (String) session.getAttribute("playerName");

        if (pin == null || playerName == null)
            return "redirect:/";

        model.addAttribute("pin", pin);
        model.addAttribute("playerName", playerName);
        return "game-player";
    }

    @PostMapping("/submit")
    @ResponseBody
    public String submitAnswer(@RequestParam int option, HttpSession session) {
        String pin = (String) session.getAttribute("pin");
        String playerName = (String) session.getAttribute("playerName");

        if (pin != null && playerName != null) {
            ActiveRoom room = gameEngine.getRoom(pin);
            if (room != null) {
                room.submitAnswer(playerName, option);
                return "OK";
            }
        }
        return "ERROR";
    }

    @GetMapping("/player/poll")
    @ResponseBody
    public Map<String, Object> pollPlayer(@RequestParam String pin) {
        ActiveRoom room = gameEngine.getRoom(pin);
        if (room == null)
            return Map.of("status", "FINISHED");

        boolean isOpen = room.isQuestionOpen();
        Question q = room.getCurrentQuestion();

        return Map.of(
                "questionIndex", room.getCurrentQuestionIndex(),
                "isOpen", isOpen,
                "questionText", (isOpen && q != null) ? q.getEnunciado() : "Wait...",
                "options", (isOpen && q != null) ? List.of(q.getOp1(), q.getOp2(), q.getOp3(), q.getOp4()) : List.of());
    }
}
