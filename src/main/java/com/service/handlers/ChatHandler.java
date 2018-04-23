package com.service.handlers;

import com.ApiCaller;
import com.ChatUserImpl;
import com.Replies;
import com.botscrew.messengercdk.domain.MessengerInterceptor;
import com.botscrew.messengercdk.domain.action.GetEvent;
import com.botscrew.messengercdk.model.incomming.Profile;
import com.botscrew.messengercdk.model.outgoing.builder.SenderAction;
import com.botscrew.messengercdk.model.outgoing.profile.menu.PersistentMenu;
import com.botscrew.messengercdk.model.outgoing.profile.menu.PostbackMenuItem;
import com.botscrew.messengercdk.service.Messenger;
import com.domain.ComicsRequestEntity;
import com.domain.HeroesRatingEntity;
import com.domain.UserComicsEntity;
import com.domain.UserEntity;
import com.marvelDTO.CharacterResults;
import com.marvelDTO.MarvelCharacterResponse;
import com.marvelDTO.MarvelComicsResponse;
import com.TemplateBuilder;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
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

    @Text
    //Method handles all text request from user with default state
    public void handleTextDefault(ChatUserImpl chatUserImpl, @Text String text) {
        nlpClient.query(chatUserImpl, text);
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
        comicsRequests.removeIf((comicsRequest) -> !comicsRequest.getHeroName().toLowerCase().equals(text));
        if (!comicsRequests.isEmpty()) {
            //todo
            //rewrite comicsRequestEntity to save characterId
            UserEntity user = userRepository.getByChatID(chatUser.getChatId());
            comicsRequests.forEach((request) -> System.out.println(request.getHeroName() + " " + request.getLastRequest()));
            MarvelCharacterResponse character = apiCaller.callMarvelForCharacter(comicsRequests.get(0).getHeroName());
            //todo
            //throws exception
            try {
                MarvelComicsResponse recentComics = apiCaller.callMarvelApiForRecentComics(character.getData().getResults().get(0).getId().toString(),
                        comicsRequests.get(0).getLastRequest());
                if (recentComics.getData().getResults().isEmpty()) {
                    sender.send(chatUser, "There are no recent comics");
                } else {
                    Request request = templateBuilder.createGenericTemplateForComics(chatUser, recentComics, user.getComics());
                    sender.send(request);
                }
            } catch (HttpClientErrorException e) {
                System.out.println(e.getResponseBodyAsString());
            }

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
                                     @Param("characterName") String characterName) {
        MarvelComicsResponse marvelComicsResponse = apiCaller.callMarvelForComics(characterId);

        if (!marvelComicsResponse.getData().getResults().isEmpty()) {

            String lastRequest = LocalDate.now().toString();
            comicsRequestRepository.save(new ComicsRequestEntity(chatUserImpl.getChatId(), characterName, lastRequest));

            List<UserComicsEntity> comicsEntities = userRepository.getByChatID(chatUserImpl.getChatId()).getComics();
            Request request = templateBuilder.createGenericTemplateForComics(chatUserImpl, marvelComicsResponse, comicsEntities);
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
    //if Id to unsibscribe matches id of subscribed comics delete it
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
            sender.send(chatUser, "you are not subscribe to any comics right now");
        } else {
            Request request = templateBuilder.createGenericTemplateForUserComics(chatUser);
            sender.send(request);
        }
    }

    @Postback(value = getRecentComicsValue)
    public void handleRecentComics(ChatUserImpl chatUser) {
        setUserState(chatUser, userStateRecentComics);
        sender.send(chatUser, recentComicsReply);
        System.out.println("Handling recent comics");
    }

    @Override
    //Send typing On when handling requests
    public boolean onAction(GetEvent getEvent) {
        sender.send(SenderAction.typingOn(getEvent.getMessengerUser()));
        return true;
    }

    @PostConstruct
    public void initMessengerProfile() {
        PersistentMenu menu = new PersistentMenu(
                Arrays.asList(
                        new PostbackMenuItem(getSubscribedComicsTitle, getSubscribedComicsValue),
                        new PostbackMenuItem(getRecentComicsTitle, getRecentComicsValue)
                )
        );

        messenger.setPersistentMenu(menu);
    }

    private void rateHero(String rating, String heroName, ChatUserImpl chatUser) {
        Boolean rate = rating.equals(like);
        HeroesRatingEntity currentRating = heroesRatingRepository.getByHeroNameAndChatID(heroName, chatUser.getChatId());

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
            List<CharacterResults> results = character.getData().getResults();

            //checking every element in results array
            //saving every character name and current date to database
            results.forEach((result) -> {
                String characterName = result.getName();
                String lastRequest = LocalDate.now().toString();
                comicsRequestRepository.save(new ComicsRequestEntity(chatUser.getChatId(), characterName, lastRequest));
            });

            //calling marvel Api for comics with character from request
            String characterId = character.getData().getResults().get(0).getId().toString();
            MarvelComicsResponse comics = apiCaller.callMarvelForComics(characterId);

            //getting user comics to check if user subscribed
            List<UserComicsEntity> comicsEntities = userRepository.getByChatID(chatUser.getChatId()).getComics();
            Request request = templateBuilder.createGenericTemplateForComics(chatUser, comics, comicsEntities);

            sender.send(request);
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
