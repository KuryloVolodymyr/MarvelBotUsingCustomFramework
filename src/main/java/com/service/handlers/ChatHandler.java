package com.service.handlers;

import com.botscrew.botframework.container.TextContainer;
import com.botscrew.botframework.domain.argument.ArgumentType;
import com.botscrew.botframework.domain.argument.kit.ArgumentKit;
import com.botscrew.botframework.domain.argument.kit.SimpleArgumentKit;
import com.botscrew.botframework.domain.argument.wrapper.SimpleArgumentWrapper;
import com.botscrew.messengercdk.model.outgoing.profile.menu.PersistentMenu;
import com.botscrew.messengercdk.model.outgoing.profile.menu.PostbackMenuItem;
import com.service.ApiCaller;
import com.ChatUserImpl;
import com.Replies;
import com.botscrew.messengercdk.domain.MessengerInterceptor;
import com.botscrew.messengercdk.domain.action.GetEvent;
import com.botscrew.messengercdk.model.incomming.Profile;
import com.botscrew.messengercdk.model.outgoing.builder.SenderAction;
import com.botscrew.messengercdk.service.Messenger;
import com.domain.ComicsRequestEntity;
import com.domain.HeroesRatingEntity;
import com.domain.UserComicsEntity;
import com.domain.UserEntity;
import com.marvelModel.MarvelCharacterResponse;
import com.marvelModel.MarvelComicsResponse;
import com.service.TemplateBuilder;
import com.botscrew.botframework.annotation.ChatEventsProcessor;
import com.botscrew.botframework.annotation.Param;
import com.botscrew.botframework.annotation.Postback;
import com.botscrew.botframework.annotation.Text;
import com.botscrew.messengercdk.model.outgoing.request.Request;
import com.botscrew.messengercdk.service.Sender;
import com.botscrew.nlpclient.provider.NlpClient;
import com.repository.ComicsRequestRepository;
import com.repository.HeroesRatingRepository;
import com.repository.UserComicsRepository;
import com.repository.UserRepository;
import com.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@ChatEventsProcessor
public class ChatHandler implements MessengerInterceptor<GetEvent>, Replies {

    @Autowired
    private Sender sender;

    @Autowired
    private TemplateBuilder templateBuilder;

    @Autowired
    private NlpClient nlpClient;

    @Autowired
    private Messenger messenger;

    @Autowired
    private ApiCaller apiCaller;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HeroesRatingRepository heroesRatingRepository;

    @Autowired
    private UserComicsRepository userComicsRepository;

    @Autowired
    private ComicsRequestRepository comicsRequestRepository;

    @Autowired
    private TextContainer container;

    @Autowired
    private UserService userService;

    @Text
    //Method handles all text request from user with default state
    public void handleTextDefault(ChatUserImpl chatUserImpl, @Text String text) {
        System.out.println(text);
        MarvelCharacterResponse marvelCharacterResponse = apiCaller.callMarvelForCharacterWithFullName(text);
        if (!marvelCharacterResponse.getData().getResults().isEmpty()) {
            Request request = templateBuilder.createGenericTemplateForCharacter(chatUserImpl, marvelCharacterResponse);
            sender.send(request);
        } else {
            nlpClient.query(chatUserImpl, text);
        }
    }

    @Text(states = {userStateComics})
    //Method handles text request when user is in "Comics" state
    public void handleComicsTextRequest(ChatUserImpl chatUser, @Text String text) {
        getComicsThroughText(chatUser, text);
        setUserState(chatUser, userStateDefault);
    }

    @Text(states = {userStateRecentComics})
    public void handleRecentComicsRequest(ChatUserImpl chatUser, @Text String text) {
        List<ComicsRequestEntity> comicsRequests = comicsRequestRepository.getByChatId(chatUser.getChatId());
        comicsRequests.removeIf((comicsRequest) -> !comicsRequest.getCharacterName().toLowerCase().equals(text));
        if (!comicsRequests.isEmpty()) {
            Request request = userService.sendRequestForRecentComicsCall(chatUser, comicsRequests);
        } else {
            nlpClient.query(chatUser, text);
        }
    }

    @Postback
    //Default postback handler
    public void handlePostbackDefault(ChatUserImpl chatUserImpl) {
        sender.send(chatUserImpl, defaultPostbackReply);
    }

    @Postback(value = comicsPostbackValue)
    //method handles postback with comics payload
    public void handleComicsPostback(ChatUserImpl chatUserImpl, @Param("characterId") String characterId,
                                     @Param("characterName") String characterName, @Param("offset") Long offset) {
        MarvelComicsResponse marvelComicsResponse = apiCaller.callMarvelForComics(characterId, offset);

        if (!marvelComicsResponse.getData().getResults().isEmpty()) {

            List<ComicsRequestEntity> comicsRequestEntities = comicsRequestRepository.getByChatId(chatUserImpl.getChatId());

            userService.saveRequestToDatabase(chatUserImpl, comicsRequestEntities, characterName, characterId);
            List<UserComicsEntity> comicsEntities = userRepository.getByChatID(chatUserImpl.getChatId()).getComics();
            Request request = templateBuilder.createGenericTemplateForComics(chatUserImpl, marvelComicsResponse,
                    comicsEntities, characterId, offset);
            sender.send(request);

        } else {
            sender.send(chatUserImpl, emptyResultsReply);
        }

    }

    @Postback(value = ratePostbackValue)
    //Method sends quick reply with like/dislike to rate character
    public void handleRatingPostback(ChatUserImpl chatUser,
                                     @Param("characterId") String characterId,
                                     @Param("characterName") String characterName) {
        Request request = templateBuilder.createRatingQuickReply(chatUser, characterName);
        sender.send(request);
    }

    @Postback(value = ratingPostbackValue)
    //method gets heroName, rating chosen in quickReply, and saves it to database
    public void handleRating(ChatUserImpl chatUser, @Param("rating") String rating, @Param("heroName") String heroName) {
        rateHero(rating, heroName, chatUser);
        sender.send(chatUser, heroName + ratingPostbackReply);
    }

    @Postback(value = characterQuickReply)
    public void handleHeroQuickReply(ChatUserImpl chatUser, @Param("characterName") String characterName) {

        MarvelCharacterResponse characterResponse = apiCaller.callMarvelForCharacter(characterName);
        Request request = templateBuilder.createGenericTemplateForCharacter(chatUser, characterResponse);
        sender.send(request);
    }

    @Postback(value = getStartetButtonPostbackValue)
    //method start of the conversation by getStarted button, and if user is not saved in database yet saves him
    public void handleStart(ChatUserImpl chatUser) {
        Profile userProfile = messenger.getProfile(chatUser.getChatId());
        if (userRepository.getByChatID(chatUser.getChatId()) == null) {
            userRepository.save(new UserEntity(chatUser.getChatId(), userProfile.getFirstName(), userProfile.getLastName()));
        }
        sender.send(chatUser, "Hi " + userProfile.getFirstName() + greetingReply);
    }

    @Postback(value = subscribePostbackValue)
    //method handles comics subscription
    //if user is not subscribed to comics, create new Entity with comics and userId and save it to database
    public void handleSubscriptionToComics(ChatUserImpl chatUser, @Param("id") Long comicsId, @Param("comicsInfo") String comicsInfo,
                                           @Param("comicsTitle") String comicsName, @Param("imageUrl") String imageUrl) {
        List<UserComicsEntity> comics = userRepository.getByChatID(chatUser.getChatId()).getComics();
        if (!checkIfSubscribed(comics, comicsId)) {
            UserComicsEntity comicsToSave = new UserComicsEntity(comicsId, comicsName, comicsInfo, imageUrl, chatUser.getChatId());
            comicsToSave.setUser(userRepository.findByChatID(chatUser.getChatId()));
            userComicsRepository.save(comicsToSave);
            sender.send(chatUser, subscribeReplySuccessfull);
        } else {
            sender.send(chatUser, subscribeReplyUnsuccessfull);
        }
    }

    @Postback(value = unsubscribePostbackValue)
    //handles unsubscription from comics
    //gets list of all comics user is subscribed to
    //if Id to unsubscribe matches id of subscribed comics delete it
    public void handleUnsubscribeToComics(ChatUserImpl chatUser, @Param("id") Long comicsId) {
        UserEntity user = userRepository.getByChatID(chatUser.getChatId());
        List<UserComicsEntity> comicsEntities = user.getComics();

        comicsEntities.forEach((comics) -> {
            if (comics.getComicsId().equals(comicsId)) {
                UserComicsEntity comicsToDelete = userComicsRepository.getByChatIdAndComicsId(chatUser.getChatId(), comicsId);
                userComicsRepository.deleteComics(comicsToDelete.getId());
            }
        });

        sender.send(chatUser, unsubscribeReply);
    }

    @Postback(value = getSubscribedComicsValue)
    //method return comics on which user had subscribed
    public void handleGetSubscribedComics(ChatUserImpl chatUser) {
        UserEntity user = userRepository.getByChatID(chatUser.getChatId());
        if (user.getComics().isEmpty()) {
            sender.send(chatUser, notSubscribedToComicsReply);
        } else {
            Request request = templateBuilder.createGenericTemplateForUserComics(chatUser, 0);
            sender.send(request);
        }
    }

    @Postback(value = getRecentComicsValue)
    public void handleRecentComics(ChatUserImpl chatUser) {
        setUserState(chatUser, userStateRecentComics);
        sender.send(chatUser, recentComicsReply);
    }

    @Postback(value = moreComicsValue)
    public void handleMoreComicsPostback(ChatUserImpl chatUser, @Param("characterId") String characterId, @Param("offset") Long offset) {
        if (!(offset < 0L)) {
            MarvelComicsResponse comics = apiCaller.callMarvelForComics(characterId, offset);
            List<UserComicsEntity> userComics = userRepository.getByChatID(chatUser.getChatId()).getComics();
            Request request = templateBuilder.createGenericTemplateForComics(chatUser, comics, userComics, characterId, offset);
            sender.send(request);
        } else {
            sender.send(chatUser, negativeOffsetReply);
        }
    }

    @Postback(value = moreUserComicsValue)
    public void handleMoreUserComicsPostback(ChatUserImpl chatUser, @Param("offset") Integer offset) {
        UserEntity user = userRepository.getByChatID(chatUser.getChatId());
        if (user.getComics().isEmpty()) {
            sender.send(chatUser, notSubscribedToComicsReply);
        } else {
            if (offset < 0) {
                sender.send(chatUser, negativeOffsetReply);
            } else {
                Request request = templateBuilder.createGenericTemplateForUserComics(chatUser, offset);
                sender.send(request);
            }
        }
    }


    @Override
    //Send typingOn when handling requests
    public boolean onAction(GetEvent getEvent) {
        sender.send(SenderAction.typingOn(getEvent.getMessengerUser()));
        return true;
    }

//    @PostConstruct
//    public void initMessengerProfile() {
//        PersistentMenu menu = new PersistentMenu(
//                Arrays.asList(
//                        new PostbackMenuItem(getSubscribedComicsTitle, getSubscribedComicsValue),
//                        new PostbackMenuItem(getRecentComicsTitle, getRecentComicsValue)
//                )
//        );
//
//        messenger.setPersistentMenu(menu);
//    }

    private void rateHero(String rating, String heroName, ChatUserImpl chatUser) {
        Boolean rate = rating.equals(like);
        HeroesRatingEntity currentRating = heroesRatingRepository.getByHeroNameAndChatID(heroName, chatUser.getChatId());

        //if there is no rating create a new one
        //else rewrite the old one
        if (currentRating == null) {
            currentRating = new HeroesRatingEntity(heroName, chatUser.getChatId(), rate);
            currentRating.setUser(userRepository.findByChatID(chatUser.getChatId()));
            heroesRatingRepository.save(currentRating);
        } else {
            currentRating.setRating(rate);
            heroesRatingRepository.save(currentRating);
        }
    }

    private Boolean checkIfSubscribed(List<UserComicsEntity> comics, Long comicsId) {
        for (UserComicsEntity singleComics : comics) {
            if (singleComics.getComicsId().equals(comicsId)) {
                return true;
            }
        }
        return false;
    }

    //method returns comics with character that matches name written by user
    //there are no comics, perform an NLP query to get characterName from text
    private void getComicsThroughText(ChatUserImpl chatUser, String text) {
        //calling marvel API with plain text written by user
        MarvelCharacterResponse character = apiCaller.callMarvelForCharacter(text);

        if (!character.getData().getResults().isEmpty()) {
            userService.handleMarvelNotEmptyResponse(chatUser, character);
        } else {
            nlpClient.query(chatUser, text);
        }
    }

    private void setUserState(ChatUserImpl chatUser, String state) {
        UserEntity user = userRepository.getByChatID(chatUser.getChatId());
        user.setState(state);
        userRepository.save(user);
    }

}
