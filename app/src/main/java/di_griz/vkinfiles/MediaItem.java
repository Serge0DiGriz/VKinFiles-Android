package di_griz.vkinfiles;

class MediaItem {
    String type, title, url;
    int messageId;

    public MediaItem(String type, String title, String url, int messageId) {
        this.type = type;
        this.title = title;
        this.url = url;
        this.messageId = messageId;
    }
}
