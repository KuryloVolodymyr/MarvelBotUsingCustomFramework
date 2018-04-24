package com.service.handlers;

import com.service.ApiCaller;
import com.ChatUserImpl;
import com.Replies;
import com.botscrew.botframework.annotation.*;
import com.domain.ComicsRequestEntity;
import com.marvelModel.MarvelCharacterResponse;
import com.repository.ComicsRequestRepository;
import com.repository.UserRepository;
import com.service.UserService;
import com.service.TemplateBuilder;
import com.botscrew.messengercdk.service.Sender;
import com.botscrew.messengercdk.model.outgoing.request.Request;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@IntentProcessor
public class IntentHandler implements Replies {

    @Autowired
    private Sender sender;

    @Autowired
    private ApiCaller apiCaller;

    @Autowired
    private TemplateBuilder templateBuilder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComicsRequestRepository comicsRequestRepository;

    @Autowired
    private UserService userService;


    @Intent
    public void handleIntent(ChatUserImpl chatUser) {
        sender.send(chatUser, defaultIntentReply);
    }

    @Intent(chooseHeroIntent)
    public void handleChooseHero(ChatUserImpl chatUser, @Param("heroName") String heroName) {
        marvelCharacterResponseSender(chatUser, heroName);
    }

    @Intent(marvelSpecialHeroesIntent)
    public void handleSpecialCase(ChatUserImpl chatUser, @Param("heroName") String heroName) {
        marvelCharacterResponseSender(chatUser, heroName);
    }

    @Intent(states = {userStateRecentComics}, value = chooseHeroIntent)
    public void handleChooseHeroWithRecentComicsState(ChatUserImpl chatUser, @Param("heroName") String heroName) {
        sendRecentComicsRequestReply(chatUser, heroName);
        userService.setUserState(chatUser, userStateDefault);
    }

    @Intent(states = {userStateRecentComics}, value = marvelSpecialHeroesIntent)
    public void handleSpecialCaseWithRecentComicsState(ChatUserImpl chatUser, @Param("heroName") String heroName) {
        sendRecentComicsRequestReply(chatUser, heroName);
        userService.setUserState(chatUser, userStateDefault);
    }

    @Intent(helpIntent)
    public void handleHelp(ChatUserImpl chatUser) {
        //todo
        sender.send(chatUser, helpIntentReply);
    }

    @Intent(thankYouIntent)
    public void handleThankYou(ChatUserImpl chatUser) {
        //todo
        //nlp response
        sender.send(chatUser, thankYouReply);
    }

    @Intent(topIntent)
    public void handleTop(ChatUserImpl chatUser) {
        Request request = templateBuilder.buildTopHeroesQuickReply(chatUser);
        sender.send(request);
    }

    @Intent(dcIntent)
    public void handleDC(ChatUserImpl chatUser) {
        //todo
        sender.send(chatUser, dcReply);
    }

    @Intent(comicsIntent)
    public void handleComicsTextRequest(ChatUserImpl chatUser) {
        userService.setUserState(chatUser, userStateComics);
        sender.send(chatUser, askUserForComics);
    }

    @Intent(states = {userStateComics}, value = marvelSpecialHeroesIntent)
    public void handleComicsTextRequestSC(ChatUserImpl chatUser, @Param("heroName") String heroName) {
        userService.setUserState(chatUser, userStateComics);
        getComicsThroughText(chatUser, heroName);
    }

    @Intent(states = {userStateComics}, value = chooseHeroIntent)
    public void handleComicsTextRequest(ChatUserImpl chatUser, @Param("heroName") String heroName) {
        getComicsThroughText(chatUser, heroName);
        userService.setUserState(chatUser, userStateDefault);
    }

    private void getComicsThroughText(ChatUserImpl chatUser, String heroName) {
        MarvelCharacterResponse character = apiCaller.callMarvelForCharacter(heroName);
        if (!character.getData().getResults().isEmpty()) {
            userService.handleMarvelNotEmptyResponse(chatUser, character);
        } else {
            sender.send(chatUser, cantFindCharacterReply);
        }
    }

    private void marvelCharacterResponseSender(ChatUserImpl chatUser, String heroName) {
        MarvelCharacterResponse marvelCharacterResponse = apiCaller.callMarvelForCharacter(heroName);

        if (!marvelCharacterResponse.getData().getResults().isEmpty()) {

            Request request = templateBuilder.createGenericTemplateForCharacter(chatUser, marvelCharacterResponse);
            sender.send(request);
        } else {

            sender.send(chatUser, cantFindCharacterReply);
        }
    }


    private void sendRecentComicsRequestReply(ChatUserImpl chatUser, String characterName) {
        List<ComicsRequestEntity> comicsRequests = comicsRequestRepository.getByChatId(chatUser.getChatId());
        comicsRequests.removeIf((comicsRequest) -> !comicsRequest.getCharacterName().toLowerCase().equals(characterName));
        if (!comicsRequests.isEmpty()) {
            Request request = userService.sendRequestForRecentComicsCall(chatUser, comicsRequests);
            sender.send(request);
        } else {
            sender.send(chatUser, haventSearchPreviouslyReply);
        }
    }


}


