package client.chat.model;

import java.util.List;

/**
 * Social feed post data.
 */
public final class SocialPost
{
    private final long id;
    private final String authorNick;
    private final String content;
    private final String imageUrl;
    private int likeCount;
    private boolean likedByMe;
    private List<SocialComment> comments;
    private final long createdMs;

    public SocialPost(long id, String authorNick, String content, String imageUrl,
                      int likeCount, boolean likedByMe, long createdMs)
    {
        this.id = id;
        this.authorNick = authorNick;
        this.content = content;
        this.imageUrl = imageUrl;
        this.likeCount = likeCount;
        this.likedByMe = likedByMe;
        this.createdMs = createdMs;
    }

    public long getId() { return this.id; }
    public String getAuthorNick() { return this.authorNick; }
    public String getContent() { return this.content; }
    public String getImageUrl() { return this.imageUrl; }
    public int getLikeCount() { return this.likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public boolean isLikedByMe() { return this.likedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }
    public List<SocialComment> getComments() { return this.comments; }
    public void setComments(List<SocialComment> comments) { this.comments = comments; }
    public long getCreatedMs() { return this.createdMs; }
}
