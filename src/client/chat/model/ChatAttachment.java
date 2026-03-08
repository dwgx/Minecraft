package client.chat.model;

public final class ChatAttachment {
    private final String id;
    private final String fileName;
    private final String mimeType;
    private final long sizeBytes;
    private final String localPath;
    private final String remoteUrl;
    private final int durationMs;
    private final int imageWidth;
    private final int imageHeight;

    public ChatAttachment(String id, String fileName, String mimeType, long sizeBytes,
                          String localPath, String remoteUrl,
                          int durationMs, int imageWidth, int imageHeight) {
        this.id = id;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.localPath = localPath;
        this.remoteUrl = remoteUrl;
        this.durationMs = durationMs;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public String getId() { return id; }
    public String getFileName() { return fileName; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getLocalPath() { return localPath; }
    public String getRemoteUrl() { return remoteUrl; }
    public int getDurationMs() { return durationMs; }
    public int getImageWidth() { return imageWidth; }
    public int getImageHeight() { return imageHeight; }

    public boolean isImage() { return mimeType != null && mimeType.startsWith("image/"); }
    public boolean isVoice() { return mimeType != null && mimeType.startsWith("audio/"); }
}
