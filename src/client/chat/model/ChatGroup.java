package client.chat.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatGroup {
    private final String id;
    private final String name;
    private final GroupType type;
    private String iconPath;
    private final String ownerId;
    private final List<ChatChannel> channels;
    private final List<String> memberIds;

    public ChatGroup(String id, String name, GroupType type, String ownerId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.ownerId = ownerId;
        this.channels = new ArrayList<ChatChannel>();
        this.memberIds = new ArrayList<String>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public GroupType getType() { return type; }
    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }
    public String getOwnerId() { return ownerId; }
    public List<ChatChannel> getChannels() { return Collections.unmodifiableList(channels); }
    public List<String> getMemberIds() { return Collections.unmodifiableList(memberIds); }
    public boolean isOfficial() { return type == GroupType.OFFICIAL; }

    public void addChannel(ChatChannel channel) { channels.add(channel); }
    public void addMember(String userId) { if (!memberIds.contains(userId)) memberIds.add(userId); }
    public void removeMember(String userId) { memberIds.remove(userId); }

    public String getInitial() {
        if (name == null || name.isEmpty()) return "?";
        return name.substring(0, 1).toUpperCase();
    }
}
