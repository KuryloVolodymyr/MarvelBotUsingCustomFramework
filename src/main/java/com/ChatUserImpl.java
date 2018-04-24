package com;

import com.botscrew.botframework.domain.user.Bot;
import com.botscrew.botframework.domain.user.ChatUser;
import com.botscrew.messengercdk.model.MessengerUser;

public class ChatUserImpl implements MessengerUser, Replies {

    private String state;

    private Long chatId;

    public ChatUserImpl(Long chatId) {
        this.chatId = chatId;
        if (this.state == null) {
            this.state = userStateDefault;
        }
    }

    public ChatUserImpl(Long chatId, String state) {
        this.chatId = chatId;
        this.state = state;
    }

    public ChatUserImpl() {
    }

    public String getState() {
        return this.state;
    }

    public Long getChatId() {
        return this.chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public void setState(String state) {
        this.state = state;
    }

}
