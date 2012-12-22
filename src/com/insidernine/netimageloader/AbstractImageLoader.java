package com.insidernine.netimageloader;

import java.io.IOException;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

public abstract class AbstractImageLoader<T>
{
  private static final String TAG = "AbstractImageLoader";
  private static final int CACHE_SIZE = 20;

  private final ImageCache<T> mImageCache = new ImageCache<T>(CACHE_SIZE);
  private final Handler mUiHandler;
  private final Drawable mUnknownPicture;
  protected final Context mContext;
  private final ImageLoadHandler mImageLoadHandler;

  /**
   * The imageView must have a tag that follows this interface.
   * @param <T> type of the Image ID
   */
  public interface ImageTag<T>
  {
    public T getImageId();
    public void onNoPictureFound();
  }

  public AbstractImageLoader(Context context, Handler uiHandler, Drawable unknownPicture)
  {
    this(context, new HandlerThread(TAG), uiHandler, unknownPicture);
  }

  public AbstractImageLoader(Context context, HandlerThread handlerThread, Handler uiHandler, Drawable unknownPicture)
  {
    mUiHandler = uiHandler;
    mUnknownPicture = unknownPicture;
    mContext = context;

    mImageCache.setUnknownPicture(mUnknownPicture);

    if (!handlerThread.isAlive())
    {
      handlerThread.start();
    }
    mImageLoadHandler = new ImageLoadHandler(handlerThread.getLooper());
  }

  public void onDestroy()
  {
    // Remove any unprocessed messages
    mImageLoadHandler.removeMessages(0);
    // Clear the cache
    mImageCache.clear();
  }

  /**
   * Return an image for this contact.  If the image is not cached then return
   * a temporary image and fill in the correct image in the imageView later on
   * the UI thread
   * @param imageView with tag set up as an ImageTag
   * @return an image to use now, or null if we're not sure.
   */
  public Drawable getImage(ImageView imageView)
  {
    // We must keep the imageView up-to-date with the right ID in its tag
    @SuppressWarnings("unchecked")
    ImageTag<T> imageTag = (ImageTag<T>)imageView.getTag();
    Drawable image = mImageCache.get(imageTag.getImageId());
    if (image != null)
    {
      return image;
    }

    // Not cached, request the real image
    mImageLoadHandler.requestImage(imageView);

    // return null for now.  We only use the unknown picture when we're sure the
    // image is unknown.  This saves flickering
    return null;
  }

  public Drawable getUnknownPicture()
  {
    return mUnknownPicture;
  }

  /**
   * Open an input stream for the item identified by ID
   * @param id
   * @return An input stream for the image, or null if none is available
   */
  protected abstract Drawable obtainImage(T id) throws IOException;

  /*
   * Handler that runs on a background thread and loads up images from the DB.
   * All requests are serialised on this thread.  The imageView is updated in
   * the foreground via mUiHandler
   */
  private class ImageLoadHandler extends Handler
  {
    private final LinkedList<ImageView> mImageRequestQueue;

    public ImageLoadHandler(Looper looper)
    {
      super(looper);
      // We don't care about what order we process the images as they are
      // quickly re-used for other images.  We just want to make sure the
      // queue does not get unbounded.  Since the ListView will re-use a screen
      // full of images this puts a natural bound on this set
      mImageRequestQueue = new LinkedList<ImageView>();
    }

    /**
     * The imageView must have its tag field set to the contact ID required.
     * Note this can change while the view is queued!
     * @param imageView
     */
    public void requestImage(ImageView imageView)
    {
      // No need to process if the imageView is in the queue already
      synchronized (this)
      {
        if (!mImageRequestQueue.contains(imageView))
        {
          mImageRequestQueue.add(imageView);
          sendEmptyMessage(0);
        }
      }
    }

    @Override
    public void handleMessage(Message msg)
    {
      // The msg is just a kick - process any image views we have in the set
      while (true)
      {
        ImageView imageView = null;
        synchronized (this)
        {
          imageView = mImageRequestQueue.poll();
        }

        if (imageView != null)
        {
          processImageView(imageView);
        }
        else
        {
          break;
        }
      }
    }

    private void processImageView(final ImageView imageView)
    {
      if (imageView == null)
      {
        // No request to process
        return;
      }

      @SuppressWarnings("unchecked")
      ImageTag<T> tag = (ImageTag<T>) imageView.getTag();
      final T id = tag.getImageId();
      // Image may have been cached in the meantime since we're on a handler
      // thread.
      Drawable image = mImageCache.get(id);

      if (image == null)
      {
        try
        {
          image = obtainImage(id);
        }
        catch (OutOfMemoryError e)
        {
          Log.e(TAG, "Could not decode picture for " + id, e);
        }
        catch (Exception e)
        {
          Log.e(TAG, "Exception obtaining image for " +id, e);
        }
        if (image == null)
        {
          Log.i(TAG, "no image, using unknown");
          image = mUnknownPicture;
        }

        mImageCache.trackingPut(id, image);
      }

      final Drawable imageResult = image;

      // Return the image to the main thread.  If test here for testing
      if (mUiHandler != null)
      {
        mUiHandler.post(new Runnable()
        {
          @Override
          public void run()
          {
            // ImageView has almost certainly been re-used
            @SuppressWarnings("unchecked")
            final ImageTag<T> actualTag = (ImageTag<T>)imageView.getTag();
            Log.d(TAG, "Have image to set " + actualTag.getImageId() + " compares to " + id);
            if (actualTag.getImageId().equals(id))
            {
              Log.d(TAG, "Same!");
              imageView.setImageDrawable(imageResult);

              if (imageResult == mUnknownPicture)
              {
                actualTag.onNoPictureFound();
              }
            }
          }
        });
      }
    }
  }
}
