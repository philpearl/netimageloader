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
      Handler uiHandler)
  {
    this(context, new HandlerThread(TAG), uiHandler);
  }

  public NetImageLoader(Context context, HandlerThread handlerThread,
      Handler uiHandler)
  {
    super(context, handlerThread, uiHandler);
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
