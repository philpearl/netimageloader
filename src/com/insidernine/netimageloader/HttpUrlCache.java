package com.insidernine.netimageloader;

public class HttpUrlCache
{
  public static String UrlToCacheFileName(String url)
  {
    return url.replace("/", "__");
  }
}
