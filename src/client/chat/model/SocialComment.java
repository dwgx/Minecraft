package client.chat.model;

/**
 * Social post comment data.
 */
public final class SocialComment
{
    private final long id;
    private final String authorNick;
    private final String content;
    private final long createdMs;

    public SocialComment(long id, String authorNick, String content, long createdMs)
    {
        this.id = id;
        this.authorNick = authorNick;
        this.content = content;
        this.createdMs = createdMs;
    }

    public long getId() { return this.id; }
    public String getAuthorNick() { return this.authorNick; }
    public String getContent() { return this.content; }
    public long getCreatedMs() { return this.createdMs; }
}
