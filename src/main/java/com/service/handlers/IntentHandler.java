package com.service.handlers;

import ai.api.model.AIResponse;
import com.ApiCaller;
import com.ChatUserImpl;
import com.Replies;
import com.botscrew.botframework.annotation.*;
import com.domain.ComicsRequestEntity;
import com.domain.UserComicsEntity;
import com.domain.UserEntity;
import com.marvelDTO.CharacterResults;
import com.marvelDTO.MarvelCharacterResponse;
import com.marvelDTO.MarvelComicsResponse;
import com.repository.ComicsRequestRepository;
import com.repository.UserRepository;
import com.service.UserService;
import com.TemplateBuilder;
import com.botscrew.messengercdk.service.Sender;
import com.botscrew.messengercdk.model.outgoing.request.Request;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
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
        //todo
        sender.send(chatUser, topIntentReply);
    }

    @Intent(dcIntent)
    public void handleDC(ChatUserImpl chatUser) {
        //todo
        sender.send(chatUser, dcReply);
    }

    @Intent(comicsIntent)
    public void handleComicsTextRequest(ChatUserImpl chatUser) {
        setUserState(chatUser, userStateComics);
        sender.send(chatUser, askUserForComics);
    }

    @Intent(states = {userStateComics}, value = marvelSpecialHeroesIntent)
    public void handleComicsTextRequestSC(ChatUserImpl chatUser, @Param("heroName") String heroName) {
        setUserState(chatUser, userStateComics);
        getComicsThroughText(chatUser, heroName);
    }

    @Intent(states = {userStateComics}, value = chooseHeroIntent)
    public void handleComicsTextRequest(ChatUserImpl chatUser, @Param("heroName") String heroName) {
        getComicsThroughText(chatUser, heroName);
        setUserState(chatUser, userStateDefault);
    }

    private void getComicsThroughText(ChatUserImpl chatUser, String heroName) {
        MarvelCharacterResponse character = apiCaller.callMarvelForCharacter(heroName);
        if (!character.getData().getResults().isEmpty()) {
            List<CharacterResults> results = character.getData().getResults();

            results.forEach((result) -> {
                String characterName = result.getName();
                String lastRequest = LocalDate.now().toString();
                comicsRequestRepository.save(new ComicsRequestEntity(chatUser.getChatId(), characterName, lastRequest));
            });

            String characterId = character.getData().getResults().get(0).getId().toString();
            MarvelComicsResponse comics = apiCaller.callMarvelForComics(characterId);
            List<UserComicsEntity> comicsEntities = userRepository.getByChatID(chatUser.getChatId()).getComics();
            Request request = templateBuilder.createGenericTemplateForComics(chatUser, comics, comicsEntities);

            sender.send(request);
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

    private void setUserState(ChatUserImpl chatUser, String state) {
        UserEntity user = userRepository.getByChatID(chatUser.getChatId());
        user.setState(state);
        userRepository.save(user);
    }


}
