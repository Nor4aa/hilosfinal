package com.example.demo.controller;

import com.example.demo.engine.GameEngine;
import com.example.demo.model.Block;
import com.example.demo.model.Room;
import com.example.demo.model.User;
import com.example.demo.service.BlockService;
import com.example.demo.service.RoomService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class LobbyController {

    @Autowired
    private UserService userService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private GameEngine gameEngine;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, HttpSession session) {
        // Simple login (create if not exists for demo)
        User user = userService.registerUser(username, "password"); // Simplified
        session.setAttribute("user", user);
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/";

        // Refresh user from DB to get blocks
        user = userService.findById(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("blocks", blockService.getBlocksByUser(user));
        return "dashboard";
    }

    @PostMapping("/create-room")
    public String createRoom(@RequestParam Long blockId,
            @RequestParam int numQuestions,
            @RequestParam int secondsPerQuestion,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/";

        Optional<Block> blockOpt = blockService.getBlockById(blockId);
        if (blockOpt.isEmpty())
            return "redirect:/dashboard?error=BlockNotFound";

        try {
            Room room = roomService.createRoom(user, blockOpt.get(), numQuestions, secondsPerQuestion, true);

            // Initialize in GameEngine
            gameEngine.createGame(room, blockOpt.get().getQuestions());

            return "redirect:/lobby/" + room.getPin();
        } catch (RuntimeException e) {
            return "redirect:/dashboard?error=" + e.getMessage();
        }
    }

    @GetMapping("/lobby/{pin}")
    public String lobby(@PathVariable String pin, Model model, HttpSession session) {
        model.addAttribute("pin", pin);
        return "lobby-host";
    }

    @PostMapping("/join")
    public String join(@RequestParam String pin, @RequestParam String playerName, HttpSession session) {
        if (gameEngine.getRoom(pin) == null) {
            return "redirect:/?error=RoomNotFound";
        }

        gameEngine.joinPlayer(pin, playerName);

        session.setAttribute("playerName", playerName);
        session.setAttribute("pin", pin);

        return "redirect:/game/player";
    }
}
