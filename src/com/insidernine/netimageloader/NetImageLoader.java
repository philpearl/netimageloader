package com.insidernine.netimageloader;

import java.io.IOException;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;

public class NetImageLoader extends AbstractImageLoader<String>
{
  private static final String TAG = "NetImageLoader";
  private final HttpLayer mHttpLayer;

  public NetImageLoader(Context context,
      Handler uiHandler, Drawable unknownPicture)
  {
    this(context, new HandlerThread(TAG), uiHandler, unknownPicture);
  }

  /**
   * Create a NetImageLoader
   * @param context
   * @param handlerThread Handler thread for background operations
   * @param uiHandler Handler for the UI thread
   * <p>
   * The NetImageLoader will load images on a background thread then install the
   * image in the UI on the UI thread. See {@link AbstractImageLoader#getImage(Object, android.widget.ImageView)},
   * where you should use a URL string for the id field
   *
   */
  public NetImageLoader(Context context, HandlerThread handlerThread,
      Handler uiHandler, Drawable unknownPicture)
  {
    super(context, handlerThread, uiHandler, unknownPicture);
    mHttpLayer = new HttpLayer(context, "NetImageLoader");
  }

  @Override
  public void onDestroy()
  {
    mHttpLayer.onDestroy();
    super.onDestroy();
  }

  @Override
  protected Drawable obtainImage(String id) throws IOException
  {
    return mHttpLayer.getImageCached(id);
  }
}
