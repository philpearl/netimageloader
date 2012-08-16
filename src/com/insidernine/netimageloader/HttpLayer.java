package com.insidernine.netimageloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.util.Log;

public class HttpLayer
{
  private static final String TAG = "HttpLayer";

  public static final int CONNECT_TIMEOUT = 60000;
  public static final int READ_TIMEOUT = 60000;

  private final Context mContext;
  private final AndroidHttpClient mHttpClient;

  public HttpLayer(Context context, String uaString)
  {
    mContext = context;
    mHttpClient = AndroidHttpClient.newInstance(uaString);
  }

  public void onDestroy()
  {
    mHttpClient.close();
  }


  public Drawable getImage(String url) throws ClientProtocolException, IOException
  {
    Log.i(TAG, "get " + url);
    HttpGet get = new HttpGet(url);

    ResponseHandler<Drawable> responseHandler = new ResponseHandler<Drawable>()
    {
      @Override
      public Drawable handleResponse(HttpResponse response)
          throws ClientProtocolException, IOException
      {
        StatusLine statusLine = response.getStatusLine();
        Log.d(TAG, "Have response " + statusLine);
        if (statusLine.getStatusCode() > 299)
        {
          throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }
        return BitmapDrawable.createFromStream(response.getEntity().getContent(), "dummy.png");
      }
    };

    return mHttpClient.execute(get, responseHandler);
  }

  public Drawable getImageCached(final String url) throws ClientProtocolException, IOException
  {
    Log.i(TAG, "get " + url);

    File cacheDir = mContext.getExternalCacheDir();
    if (cacheDir == null)
    {
      cacheDir = mContext.getCacheDir();
    }

    final File cacheFile = new File(cacheDir, HttpUrlCache.UrlToCacheFileName(url));
    if (cacheFile.exists())
    {
      Log.d(TAG, "load from " + cacheFile.getAbsolutePath());
      return BitmapDrawable.createFromPath(cacheFile.getAbsolutePath());
    }

    HttpGet get = new HttpGet(url);

    ResponseHandler<Drawable> responseHandler = new ResponseHandler<Drawable>()
    {
      @Override
      public Drawable handleResponse(HttpResponse response)
          throws ClientProtocolException, IOException
      {
        StatusLine statusLine = response.getStatusLine();
        Log.d(TAG, "Have response " + statusLine);
        if (statusLine.getStatusCode() > 299)
        {
          throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }

        OutputStream os = new FileOutputStream(cacheFile);
        try
        {
          response.getEntity().writeTo(os);
        }
        finally
        {
          os.close();
        }

        return BitmapDrawable.createFromPath(cacheFile.getAbsolutePath());
      }
    };

    return mHttpClient.execute(get, responseHandler);
  }

}
