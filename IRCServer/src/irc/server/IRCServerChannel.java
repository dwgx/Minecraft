package irc.server;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single IRC channel on the server.
 * Thread-safe: members set backed by ConcurrentHashMap.
 */
public final class IRCServerChannel
{
    private final String name;
    private final Set<String> members = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private volatile String topic = "";

    public IRCServerChannel(String name) { this.name = name; }

    public String getName() { return this.name; }
    public String getTopic() { return this.topic; }
    public void setTopic(String topic) { this.topic = topic != null ? topic : ""; }

    public Set<String> getMembers() { return Collections.unmodifiableSet(this.members); }
    public boolean hasMember(String nick) { return nick != null && this.members.contains(nick.toLowerCase()); }
    public void addMember(String nick) { if (nick != null) this.members.add(nick.toLowerCase()); }
    public void removeMember(String nick) { if (nick != null) this.members.remove(nick.toLowerCase()); }
    public int getMemberCount() { return this.members.size(); }
    public boolean isEmpty() { return this.members.isEmpty(); }

    /** Build the NAMES reply string (space-separated nicks). */
    public String getNamesString()
    {
        StringBuilder sb = new StringBuilder();
        for (String nick : this.members)
        {
            if (sb.length() > 0) sb.append(' ');
            sb.append(nick);
        }
        return sb.toString();
    }
}
