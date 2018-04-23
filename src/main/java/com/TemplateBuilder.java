package com;

import com.domain.UserComicsEntity;
import com.domain.UserEntity;
import com.domain.UserRequestEntity;
import com.marvelDTO.*;
import com.botscrew.messengercdk.model.outgoing.builder.GenericTemplate;
import com.botscrew.messengercdk.model.outgoing.builder.QuickReplies;
import com.botscrew.messengercdk.model.outgoing.element.TemplateElement;
import com.botscrew.messengercdk.model.outgoing.element.button.PostbackButton;
import com.botscrew.messengercdk.model.outgoing.element.button.WebButton;
import com.botscrew.messengercdk.model.outgoing.request.Request;
import com.repository.UserRepository;
import com.repository.UserRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class TemplateBuilder implements Replies {

    @Autowired
    private UserRequestRepository userRequestRepository;

    @Autowired
    private UserRepository userRepository;

    public Request createGenericTemplateForCharacter(ChatUserImpl chatUser, MarvelCharacterResponse marvelCharacterResponse) {

        //get current user from database
        UserEntity user = userRepository.getByChatID(chatUser.getChatId());

        //mapping marvel results to list
        List<CharacterResults> results = marvelCharacterResponse.getData().getResults();
        List<TemplateElement> templateElements = new ArrayList<>();
        GenericTemplate.Builder builder = GenericTemplate.builder();

        //iterating over each resultListElement
        iterateThroughCharacters(results, chatUser, templateElements, user);

        //Adding each generic element to template
        templateElements.forEach((builder::addElement));
        return builder.user(chatUser).build();
    }

    public Request createGenericTemplateForComics(ChatUserImpl chatUser,
                                                  MarvelComicsResponse marvelComicsResponse,
                                                  List<UserComicsEntity> comicsEntities) {
        List<ComicsResults> results = marvelComicsResponse.getData().getResults();
        List<TemplateElement> elements = new ArrayList<>();
        GenericTemplate.Builder builder = GenericTemplate.builder();

        iterateThroughComics(results, comicsEntities, elements);

        elements.forEach(builder::addElement);

        return builder.user(chatUser).build();
    }

    public Request createGenericTemplateForUserComics(ChatUserImpl chatUser) {
        List<UserComicsEntity> comics = userRepository.getByChatID(chatUser.getChatId()).getComics();
        List<UserComicsEntity> shortenedComics;
        List<TemplateElement> elements = new ArrayList<>();
        GenericTemplate.Builder builder = GenericTemplate.builder();


        //if user is subscribed to more than ComicsAtOnce number of comics shorten comicst list to ComicsAtOnce length
        if (comics.size() > comicsAtOnce) {
            shortenedComics = comics.subList(comics.size() - comicsAtOnce, comics.size());

            iterateThroughUserComics(shortenedComics, elements);
        } else {
            iterateThroughUserComics(comics, elements);
        }
        elements.forEach(builder::addElement);

        return builder.user(chatUser).build();
    }

    private Boolean checkIfSubscribed(List<UserComicsEntity> comicsEntities, Long id) {
        for (UserComicsEntity comicsEntity : comicsEntities) {
            if (comicsEntity.getComicsId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public Request createRatingQuickReply(ChatUserImpl chatUser, String heroName) {
        return QuickReplies.builder()
                .text("Do you like " + heroName + "?")
                .postback(likeTitle, "rating?rating=" + like + "?heroName=" + heroName)
                .postback(dislikeTitle, "rating?rating=" + dislike + "?heroName=" + heroName)
                .user(chatUser)
                .build();
    }

    private TemplateElement buildElementForComics(String comicsInfo, String title, String description, String imageUrl, Long id, Boolean subscription) {
        String subscriptionAction;
        String subscriptionTitle;
        if (subscription) {
            subscriptionAction = unsubscribePostbackValue;
            subscriptionTitle = unsubscribePostbackTitle;
        } else {
            subscriptionAction = subscribePostbackValue;
            subscriptionTitle = subscribePostbackTitle;

        }
        return buildGenericTemplateForComics(comicsInfo, title, imageUrl, subscriptionAction, subscriptionTitle,
                description, id);
    }

    private TemplateElement buildGenericTemplateForComics(String comicsInfo, String title, String imageUrl,
                                                          String subscriptionAction, String subscriptionTitle,
                                                          String description, Long id) {
        if (!comicsInfo.isEmpty()) {
            return TemplateElement.builder()
                    .title(title)
                    .subtitle(description)
                    .imageUrl(imageUrl)
                    .button(new WebButton("Info", comicsInfo))
                    .button(new PostbackButton(subscriptionTitle, subscriptionAction + "?id=" + id + "?comicsTitle=" + title +
                            "?comicsInfo=" + comicsInfo + "?imageUrl=" + imageUrl))
                    .build();
        } else {
            return TemplateElement.builder()
                    .title(title)
                    .subtitle(description)
                    .imageUrl(imageUrl)
                    .button(new PostbackButton(subscriptionTitle, subscriptionAction + "?id=" + id + "?comicsTitle=" + title +
                            "?imageUrl=" + imageUrl))
                    .build();
        }
    }


    private TemplateElement buildElementForUserComics(String comicsInfo, String title, String imageUrl, Long id) {
        String subscriptionAction;
        String subscriptionTitle;
        subscriptionAction = unsubscribePostbackValue;
        subscriptionTitle = unsubscribePostbackTitle;

        return buildGenericTemplateForUserComics(comicsInfo, title, imageUrl, subscriptionAction, subscriptionTitle, id);
    }

    private TemplateElement buildGenericTemplateForUserComics(String comicsInfo, String title, String imageUrl,
                                                              String subscriptionAction, String subscriptionTitle, Long id) {
        if (!comicsInfo.isEmpty()) {
            return TemplateElement.builder()
                    .title(title)
                    .imageUrl(imageUrl)
                    .button(new WebButton("Info", comicsInfo))
                    .button(new PostbackButton(subscriptionTitle, subscriptionAction + "?id=" + id + "?comicsTitle=" + title +
                            "?comicsInfo=" + comicsInfo + "?imageUrl=" + imageUrl))
                    .build();
        } else {
            return TemplateElement.builder()
                    .title(title)
                    .imageUrl(imageUrl)
                    .button(new PostbackButton(subscriptionTitle, subscriptionAction + "?id=" + id + "?comicsTitle=" + title +
                            "?imageUrl=" + imageUrl))
                    .build();
        }
    }


    private void addCharacterTemplate(String wiki, String imageURL, String name, CharacterResults result, List<TemplateElement> templateElements) {
        if (wiki.isEmpty()) {
            templateElements.add(TemplateElement.builder()
                    .imageUrl(imageURL)
                    .title(name)
                    .button(new PostbackButton("Comics", "comics?characterId=" + result.getId() + "?characterName=" + name))
                    .button(new PostbackButton("Rate", "rate?characterId=" + result.getId() + "?characterName=" + name))
                    .build());
        } else {
            templateElements.add(TemplateElement.builder()
                    .imageUrl(imageURL)
                    .title(name)
                    .button(new WebButton("Wiki", wiki))
                    .button(new PostbackButton("Comics", "comics?characterId=" + result.getId() + "?characterName=" + name))
                    .button(new PostbackButton("Rate", "rate?characterId=" + result.getId() + "?characterName=" + name))
                    .build());
        }
    }

    private void iterateThroughCharacters(List<CharacterResults> results, ChatUserImpl chatUser,
                                          List<TemplateElement> templateElements, UserEntity user) {
        for (CharacterResults result : results) {
            String name = result.getName();
            String imageURL = result.getThumbnail().getPath() + "." + result.getThumbnail().getExtension();

            UserRequestEntity request = new UserRequestEntity(name, result.getId(), chatUser.getChatId());
            request.setUser(user);
            userRequestRepository.save(request);

            String wiki = "";
            for (Urls url : result.getUrls()) {
                if (url.getType().equals("wiki")) {
                    wiki = url.getUrl();
                }
            }
            addCharacterTemplate(wiki, imageURL, name, result, templateElements);
        }
    }

    private void iterateThroughComics(List<ComicsResults> results, List<UserComicsEntity> comicsEntities,
                                      List<TemplateElement> templateElements) {
        for (ComicsResults result : results) {
            Long id = result.getId();
            Boolean subscription = checkIfSubscribed(comicsEntities, id);
            String title = result.getTitle();
            String description = result.getDecription();
            String imageUrl = result.getThumbnail().getPath() + "." + result.getThumbnail().getExtension();

            String comicsInfo = "";

            List<Urls> urls = result.getUrls();
            for (Urls url : urls) {
                if (url.getType().equals("detail")) {
                    comicsInfo = url.getUrl();
                }
            }

            templateElements.add(buildElementForComics(comicsInfo, title, description, imageUrl, id, subscription));
        }
    }


    private void iterateThroughUserComics(List<UserComicsEntity> comics, List<TemplateElement> templateElements) {
        for (UserComicsEntity comicsEntity : comics) {
            Long id = comicsEntity.getComicsId();
            String title = comicsEntity.getComicsName();
            String imageUrl = comicsEntity.getImageUrl();

            String comicsInfo = comicsEntity.getComicsInfoUrl();

            templateElements.add(buildElementForUserComics(comicsInfo, title, imageUrl, id));
        }
    }
}
