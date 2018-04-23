package com.service;

import com.ChatUserImpl;
import com.botscrew.botframework.domain.param.SimpleStringParametersDetector;
import com.botscrew.botframework.domain.param.StringParametersDetector;
import com.botscrew.messengercdk.model.MessengerUser;
import com.botscrew.messengercdk.service.UserProvider;
import com.domain.UserEntity;
import com.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserService implements UserProvider {

    @Autowired
    private UserRepository userRepository;

    private StringParametersDetector stringParametersDetector = new SimpleStringParametersDetector();

    public MessengerUser getByChatIdAndPageId(Long chatId, Long pageId) {
        UserEntity user = userRepository.getByChatID(chatId);
        if (user == null){
            return new ChatUserImpl(chatId);
        }
        else {
            return new ChatUserImpl(chatId, user.getState());
        }
    }
}
