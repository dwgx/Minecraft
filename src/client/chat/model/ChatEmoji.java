package client.chat.model;

public final class ChatEmoji {
    private final String id;
    private final String shortcode;
    private final String imagePath;
    private final String ownerId;
    private final boolean sticker;

    public ChatEmoji(String id, String shortcode, String imagePath, String ownerId, boolean sticker) {
        this.id = id;
        this.shortcode = shortcode;
        this.imagePath = imagePath;
        this.ownerId = ownerId;
        this.sticker = sticker;
    }

    public String getId() { return id; }
    public String getShortcode() { return shortcode; }
    public String getImagePath() { return imagePath; }
    public String getOwnerId() { return ownerId; }
    public boolean isSticker() { return sticker; }
    public boolean isBuiltin() { return ownerId == null; }
}
