package org.luxons.sevenwonders.controllers;

import java.security.Principal;

import org.luxons.sevenwonders.actions.ChooseNameAction;
import org.luxons.sevenwonders.lobby.Player;
import org.luxons.sevenwonders.repositories.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final PlayerRepository playerRepository;

    @Autowired
    public HomeController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @MessageMapping("/chooseName")
    @SendToUser("/queue/nameChoice")
    public Player chooseName(@Validated ChooseNameAction action, Principal principal) {
        String username = principal.getName();
        Player player = playerRepository.createOrUpdate(username, action.getPlayerName());

        logger.info("Player '{}' chose the name '{}'", username, player.getDisplayName());
        return player;
    }
}
