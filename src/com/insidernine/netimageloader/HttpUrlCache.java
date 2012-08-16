package com.insidernine.netimageloader;

public class HttpUrlCache
{
  public static String UrlToCacheFileName(String url)
  {
    String filename = url.replaceAll("[:/\\?&]", "__");
    if (filename.length() > 255)
    {
      filename = filename.substring(0, 254);
    }
    return filename;
  }
}
