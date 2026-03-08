package client.chat.model;

/**
 * Mail message data.
 */
public final class MailMessage
{
    private final long id;
    private final String fromNick;
    private final String toNick;
    private final String subject;
    private final String body;
    private boolean read;
    private final long createdMs;

    public MailMessage(long id, String fromNick, String toNick, String subject,
                       String body, boolean read, long createdMs)
    {
        this.id = id;
        this.fromNick = fromNick;
        this.toNick = toNick;
        this.subject = subject;
        this.body = body;
        this.read = read;
        this.createdMs = createdMs;
    }

    public long getId() { return this.id; }
    public String getFromNick() { return this.fromNick; }
    public String getToNick() { return this.toNick; }
    public String getSubject() { return this.subject; }
    public String getBody() { return this.body; }
    public boolean isRead() { return this.read; }
    public void setRead(boolean read) { this.read = read; }
    public long getCreatedMs() { return this.createdMs; }
}
