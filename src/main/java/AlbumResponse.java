public class AlbumResponse {
  private String albumId;
  private String imageSize;

  public String getImageSize() {
    return imageSize;
  }

  // Constructor

  public AlbumResponse() {
    super();
  }

  public AlbumResponse(String albumID, String imageSize) {
    this.albumId = albumID;
    this.imageSize = imageSize;
  }

  // Getters and setters

  public String getAlbumID() {
    return albumId;
  }

  public void setAlbumID(String albumID) {
    this.albumId = albumID;
  }

  public void setImageSize(String imageSize) {
    this.imageSize = imageSize;
  }




}
