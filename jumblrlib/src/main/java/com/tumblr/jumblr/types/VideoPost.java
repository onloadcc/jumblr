package com.tumblr.jumblr.types;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * This class represents a post of type "video"
 *
 * @author jc
 */
public class VideoPost extends Post {

  /**
   * Enum of valid video types.
   */
  public enum VideoType {
    TUMBLR("tumblr"), YOUTUBE("youtube");

    private final String mType;

    VideoType(String video_type) {
      this.mType = video_type;
    }

    public String getValue() {
      return this.mType;
    }
  }

  private List<Video> player;
  private String caption;
  private String embed, permalink_url;
  private File data;
  private String thumbnail_url;
  private int thumbnail_width;
  private int thumbnail_height;

  private String video_type;

  private String video_url;

  /**
   * Get the permalink URL for this video
   *
   * @return the permalink url
   */
  public String getPermalinkUrl() {
    return permalink_url;
  }

  /**
   * Get the thumbnail URL for the video
   *
   * @return possibly null URL
   */
  public String getThumbnailUrl() {
    return thumbnail_url;
  }

  /**
   * Get the thumbnail URL for the video
   *
   * @return 0 if not set
   */
  public int getThumbnailWidth() {
    return thumbnail_width;
  }

  /**
   * Get the thumbnail URL for the video
   *
   * @return 0 if not set
   */
  public int getThumbnailHeight() {
    return thumbnail_height;
  }

  /**
   * Get the videos from this post
   *
   * @return the videos
   */
  public List<Video> getVideos() {
    return this.player;
  }

  /**
   * Get the caption from this post
   *
   * @return String caption
   */
  public String getCaption() {
    return caption;
  }

  /**
   * Set the embed code for this post
   *
   * @param embed the Embed HTML for this post
   * @throws IllegalArgumentException if data is already set
   */
  public void setEmbedCode(String embed) {
    if (data != null) {
      throw new IllegalArgumentException("Cannot supply both data & embed");
    }
    this.embed = embed;
  }

  /**
   * Set the data for this post
   *
   * @param file the file to read from
   * @throws IllegalArgumentException source is already set
   */
  public void setData(File file) {
    if (embed != null) {
      throw new IllegalArgumentException("Cannot supply both embed & data");
    }
    this.data = file;
  }

  /**
   * Set the caption for this post
   *
   * @param caption the caption to set
   */
  public void setCaption(String caption) {
    this.caption = caption;
  }

  public VideoType getVideoType() {
    if (VideoType.YOUTUBE.getValue().equals(video_type)) {
      return VideoType.YOUTUBE;
    } else {
      return VideoType.TUMBLR;
    }
  }

  public String getVideoUrl() {
    return video_url;
  }

  @Override public PostType getType() {
    return PostType.VIDEO;
  }

  /**
   * Get the details for this post (in addition to the base details)
   *
   * @return details of this post
   */
  @Override public Map<String, Object> detail() {
    final Map<String, Object> details = super.detail();
    details.put("caption", caption);
    details.put("embed", embed);
    details.put("data", data);
    return details;
  }
}
