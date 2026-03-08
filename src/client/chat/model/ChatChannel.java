package client.chat.model;

public final class ChatChannel {
    private final String id;
    private final String name;
    private final ChannelType type;
    private final String groupId;

    public ChatChannel(String id, String name, ChannelType type, String groupId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.groupId = groupId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public ChannelType getType() { return type; }
    public String getGroupId() { return groupId; }
    public boolean isText() { return type == ChannelType.TEXT; }
    public boolean isVoice() { return type == ChannelType.VOICE; }
}
