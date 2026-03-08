package client.chat.model;

public final class ChatMessage {
    private final String id;
    private final String senderId;
    private final String conversationId;
    private final MessageType type;
    private final String textContent;
    private final ChatAttachment attachment;
    private final long timestampMs;
    private final boolean encrypted;
    private String decryptedContent;

    public ChatMessage(String id, String senderId, String conversationId,
                       MessageType type, String textContent, ChatAttachment attachment,
                       long timestampMs, boolean encrypted) {
        this.id = id;
        this.senderId = senderId;
        this.conversationId = conversationId;
        this.type = type;
        this.textContent = textContent;
        this.attachment = attachment;
        this.timestampMs = timestampMs;
        this.encrypted = encrypted;
    }

    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getConversationId() { return conversationId; }
    public MessageType getType() { return type; }
    public String getTextContent() { return textContent; }
    public ChatAttachment getAttachment() { return attachment; }
    public long getTimestampMs() { return timestampMs; }
    public boolean isEncrypted() { return encrypted; }
    public String getDecryptedContent() { return decryptedContent; }
    public void setDecryptedContent(String content) { this.decryptedContent = content; }

    public String getDisplayText() {
        if (encrypted && decryptedContent != null) return decryptedContent;
        return textContent;
    }

    public static ChatMessage text(String id, String senderId, String convId, String text, long ts) {
        return new ChatMessage(id, senderId, convId, MessageType.TEXT, text, null, ts, false);
    }

    public static ChatMessage image(String id, String senderId, String convId, ChatAttachment att, long ts) {
        return new ChatMessage(id, senderId, convId, MessageType.IMAGE, null, att, ts, false);
    }
}
