package com.service;

import com.ChatUserImpl;
import com.Replies;
import com.botscrew.botframework.domain.param.SimpleStringParametersDetector;
import com.botscrew.botframework.domain.param.StringParametersDetector;
import com.botscrew.messengercdk.model.MessengerUser;
import com.botscrew.messengercdk.model.outgoing.builder.TextMessage;
import com.botscrew.messengercdk.model.outgoing.request.Request;
import com.botscrew.messengercdk.service.Sender;
import com.botscrew.messengercdk.service.UserProvider;
import com.domain.ComicsRequestEntity;
import com.domain.UserComicsEntity;
import com.domain.UserEntity;
import com.marvelModel.CharacterResults;
import com.marvelModel.MarvelCharacterResponse;
import com.marvelModel.MarvelComicsResponse;
import com.repository.ComicsRequestRepository;
import com.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class UserService implements UserProvider, Replies {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComicsRequestRepository comicsRequestRepository;

    @Autowired
    private ApiCaller apiCaller;

    @Autowired
    private TemplateBuilder templateBuilder;

    @Autowired
    private Sender sender;

    private StringParametersDetector stringParametersDetector = new SimpleStringParametersDetector();

    public MessengerUser getByChatIdAndPageId(Long chatId, Long pageId) {
        UserEntity user = userRepository.getByChatID(chatId);
        if (user == null) {
            return new ChatUserImpl(chatId);
        } else {
            return new ChatUserImpl(chatId, user.getState());
        }
    }

    public Request sendRequestForRecentComicsCall(ChatUserImpl chatUser, List<ComicsRequestEntity> comicsRequests) {
        ComicsRequestEntity comicsRequest = comicsRequests.get(0);
        Request request;
        UserEntity user = userRepository.getByChatID(chatUser.getChatId());

        String characterName = comicsRequest.getCharacterName();
        String characterId = comicsRequest.getCharacterId();
        String lastRequestDate = comicsRequest.getLastRequest();

        comicsRequest.setLastRequest(LocalDate.now().toString());
        comicsRequestRepository.save(comicsRequest);

        MarvelComicsResponse recentComics = apiCaller.callMarvelApiForRecentComics(characterId, lastRequestDate);
        if (recentComics.getData().getResults().isEmpty()) {
            request = TextMessage.builder()
                    .user(chatUser)
                    .text(noRecentComicsReply + characterName)
                    .build();
        } else {

            request = templateBuilder.createGenericTemplateForComics(chatUser, recentComics, user.getComics(), characterId, 0L);

        }
        setUserState(chatUser, userStateDefault);
        return request;
    }

    public void setUserState(ChatUserImpl chatUser, String state) {
        UserEntity user = userRepository.getByChatID(chatUser.getChatId());
        user.setState(state);
        userRepository.save(user);
    }

    public void handleMarvelNotEmptyResponse(ChatUserImpl chatUser, MarvelCharacterResponse character) {
        List<CharacterResults> results = character.getData().getResults();
        List<ComicsRequestEntity> comicsRequests = comicsRequestRepository.getByChatId(chatUser.getChatId());
        results.forEach((result) -> {
            String characterId = result.getId().toString();
            String characterName = result.getName();
            saveRequestToDatabase(chatUser, comicsRequests, characterName, characterId);
        });

        String characterId = character.getData().getResults().get(0).getId().toString();
        MarvelComicsResponse comics = apiCaller.callMarvelForComics(characterId, 0L);
        List<UserComicsEntity> comicsEntities = userRepository.getByChatID(chatUser.getChatId()).getComics();
        Request request = templateBuilder.createGenericTemplateForComics(chatUser, comics, comicsEntities, characterId, 0L);

        sender.send(request);
    }


    //if array of comicsRequests by user with current charId has comics with characterName
    //setting current date as the day of last request and saving it to database
    //else saving new entity to database
    public void saveRequestToDatabase(ChatUserImpl chatUser, List<ComicsRequestEntity> comicsRequestEntities,
                                      String characterName, String characterId) {
        String currentTime = LocalDate.now().toString();
        Boolean needToSave = true;
        for (ComicsRequestEntity comics : comicsRequestEntities) {
            if (comics.getCharacterName().equals(characterName)) {
                Long id = comics.getId();
                String nameOfCharacter = comics.getCharacterName();
                String idOfCharacter = comics.getCharacterId();
                ComicsRequestEntity comicsRequestEntity = new ComicsRequestEntity(chatUser.getChatId(), nameOfCharacter,
                        idOfCharacter, currentTime);
                comicsRequestEntity.setId(id);
                comicsRequestRepository.save(comicsRequestEntity);
                needToSave = false;
            }

        }
        if (needToSave) {
            System.out.println("extra save");
            comicsRequestRepository.save(new ComicsRequestEntity(chatUser.getChatId(), characterName, characterId, currentTime));
        }
    }
}
