package com.service;

import com.ChatUserImpl;
import com.Replies;
import com.botscrew.messengercdk.model.outgoing.builder.TextMessage;
import com.botscrew.messengercdk.model.outgoing.element.quickreply.PostbackQuickReply;
import com.domain.UserComicsEntity;
import com.domain.UserEntity;
import com.domain.UserRequestEntity;
import com.marvelModel.*;
import com.botscrew.messengercdk.model.outgoing.builder.GenericTemplate;
import com.botscrew.messengercdk.model.outgoing.builder.QuickReplies;
import com.botscrew.messengercdk.model.outgoing.element.TemplateElement;
import com.botscrew.messengercdk.model.outgoing.element.button.PostbackButton;
import com.botscrew.messengercdk.model.outgoing.element.button.WebButton;
import com.botscrew.messengercdk.model.outgoing.request.Request;
import com.repository.HeroesRatingRepository;
import com.repository.UserRepository;
import com.repository.UserRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TemplateBuilder implements Replies {

    @Autowired
    private UserRequestRepository userRequestRepository;

    @Autowired
    private HeroesRatingRepository heroesRatingRepository;

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
                                                  List<UserComicsEntity> comicsEntities, String characterId, Long offset) {
        List<ComicsResults> results = marvelComicsResponse.getData().getResults();
        List<TemplateElement> elements = new ArrayList<>();
        GenericTemplate.Builder builder = GenericTemplate.builder();
        Long moveComicsOffset = offset + 5L;
        Long backOffset = offset - 5L;

        iterateThroughComics(results, comicsEntities, elements);
        elements.add(TemplateElement.builder()
                .title(moreComics)
                .imageUrl("http://www.banklawyersblog.com/.a/6a00d8341c652b53ef014e8bdd983a970d-800wi")
                .button(new PostbackButton(moreComics, moreComicsValue + "?characterId=" + characterId + "?offset=" + moveComicsOffset))
                .button(new PostbackButton(comicsBack, moreComicsValue + "?characterId=" + characterId + "?offset=" + backOffset))
                .build());

        elements.forEach(builder::addElement);

        return builder.user(chatUser).build();
    }

    public Request createGenericTemplateForUserComics(ChatUserImpl chatUser, Integer currentIndex) {
        List<UserComicsEntity> comics = userRepository.getByChatID(chatUser.getChatId()).getComics();
        List<UserComicsEntity> shortenedComics;
        List<TemplateElement> elements = new ArrayList<>();
        GenericTemplate.Builder builder = GenericTemplate.builder();

        Integer size = comics.size() - currentIndex;
        Integer more = currentIndex + comicsAtOnce;
        Integer back = currentIndex - comicsAtOnce;


        //if user is subscribed to more than ComicsAtOnce number of comics shorten comics list to ComicsAtOnce length
        if (size > comicsAtOnce) {
            shortenedComics = comics.subList(currentIndex, currentIndex + comicsAtOnce);
            iterateThroughUserComics(shortenedComics, elements);
            elements.add(TemplateElement.builder()
                    .title(moreComics)
                    .imageUrl("http://www.banklawyersblog.com/.a/6a00d8341c652b53ef014e8bdd983a970d-800wi")
                    .button(new PostbackButton(moreComics, moreUserComicsValue + "?offset=" + more))
                    .button(new PostbackButton(comicsBack, moreUserComicsValue + "?offset=" + back))
                    .build());
        } else {
            shortenedComics = comics.subList(currentIndex, comics.size());
            iterateThroughUserComics(shortenedComics, elements);
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
        Long likes = heroesRatingRepository.getLikesForHero(heroName);
        Long dislikes = heroesRatingRepository.getDisLikesForHero(heroName);
        return QuickReplies.builder()
                .text("Do you like " + heroName + "?")
                .postback(likeTitle + " " + likes, "rating?rating=" + like + "?heroName=" + heroName)
                .postback(dislikeTitle + " " + dislikes, "rating?rating=" + dislike + "?heroName=" + heroName)
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


    private TemplateElement buildGenericTemplateForUserComics(String comicsInfo, String title, String imageUrl,
                                                              Long id) {
        String subscriptionAction = unsubscribePostbackValue;
        String subscriptionTitle = unsubscribePostbackTitle;
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
                    .button(new PostbackButton("Comics", "comics?characterId=" + result.getId() + "?characterName=" + name + "?offset=" + 0))
                    .button(new PostbackButton("Rate", "rate?characterId=" + result.getId() + "?characterName=" + name))
                    .build());
        } else {
            templateElements.add(TemplateElement.builder()
                    .imageUrl(imageURL)
                    .title(name)
                    .button(new WebButton("Wiki", wiki))
                    .button(new PostbackButton("Comics", "comics?characterId=" + result.getId() + "?characterName=" + name + "?offset=" + 0))
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

            templateElements.add(buildGenericTemplateForUserComics(comicsInfo, title, imageUrl, id));
        }
    }

    public Request buildTopHeroesQuickReply(ChatUserImpl chatUser) {
        Set<String> characters = heroesRatingRepository.getTopHeroesForQuickReply();
        checkTopHeroesSize(characters, 0);
        characters.forEach(System.out::println);
        QuickReplies.Builder quickReplyes = QuickReplies.builder();
        quickReplyes.user(chatUser)
                .text(topIntentReply);
        characters.forEach((character) -> {
            quickReplyes.addQuickReply(new PostbackQuickReply(character, characterQuickReply + "?characterName=" + character));
        });
        return quickReplyes.build();
    }

    private void checkTopHeroesSize(Set<String> topHeroes, Integer offset) {
        List<String> standartTopHeroes = getStandartTopHeroes();
        if (topHeroes.size() < topHeroesNumber) {
            topHeroes.add(standartTopHeroes.get(offset));
            checkTopHeroesSize(topHeroes, ++offset);
        }
//        return topHeroes;
    }

    private List<String> getStandartTopHeroes() {
        List<String> top = new ArrayList<>();
        top.add("Iron Man");
        top.add("Hulk");
        top.add("Spider-Man");
        top.add("Sandman");
        top.add("Captain America");
        top.add("Thor");
        top.add("Vision");
        top.add("Star-Lord");
        top.add("Doctor Strange");
        top.add("Deadpool");
        return top;
    }


}
