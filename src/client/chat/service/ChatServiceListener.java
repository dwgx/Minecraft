package client.chat.service;

import client.chat.model.*;

public interface ChatServiceListener {
    void onMessageReceived(ChatMessage message);
    void onUserStatusChanged(String userId, UserStatus newStatus);
    void onConversationUpdated(ChatConversation conversation);
    void onGroupUpdated(ChatGroup group);
    void onConnectionStateChanged(boolean connected);
    void onVoiceCallStateChanged(boolean active, String conversationId);
    void onError(String errorMessage);
}
