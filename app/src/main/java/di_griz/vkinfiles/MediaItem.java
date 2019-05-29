package di_griz.vkinfiles;

class MediaItem {
    String type, tytle, url;
    int messageId;

    public MediaItem(String type, String tytle, String url, int messageId) {
        this.type = type;
        this.tytle = tytle;
        this.url = url;
        this.messageId = messageId;
    }
}
