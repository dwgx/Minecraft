package client.chat.service;

import client.chat.model.*;
import java.util.List;

/**
 * Chat service facade. UI calls this interface only.
 * Initially backed by MockChatService; real networking plugs in later.
 */
public interface ChatService {

    void connect();
    void disconnect();
    boolean isConnected();

    ChatUser getLocalUser();
    void updateNickname(String nickname);
    void updateStatus(UserStatus status);
    void updateAvatar(String imagePath);
    List<ChatUser> getContacts();
    ChatUser getUserById(String userId);

    List<ChatConversation> getConversations();
    ChatConversation getConversation(String conversationId);
    ChatConversation getOrCreateDm(String userId);

    List<ChatMessage> getMessages(String conversationId, int offset, int limit);
    void sendTextMessage(String conversationId, String text);
    void sendImageMessage(String conversationId, String imagePath);
    void sendVoiceMessage(String conversationId, String audioPath, int durationMs);
    void sendEncryptedMessage(String conversationId, String text);

    List<ChatGroup> getGroups();
    ChatGroup createGroup(String name, GroupType type);
    void joinGroup(String groupId);
    void leaveGroup(String groupId);
    void inviteToGroup(String groupId, String userId);
    List<ChatUser> getGroupMembers(String groupId);

    void startVoiceCall(String conversationId);
    void endVoiceCall();
    boolean isInVoiceCall();

    List<ChatEmoji> getCustomEmojis();
    void addCustomEmoji(String shortcode, String imagePath, boolean isSticker);
    void removeCustomEmoji(String emojiId);

    void addListener(ChatServiceListener listener);
    void removeListener(ChatServiceListener listener);
}
