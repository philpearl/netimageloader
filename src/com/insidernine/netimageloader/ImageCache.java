package com.insidernine.netimageloader;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Debug;
import android.support.v4.util.LruCache;
import android.util.Log;

/**
 * Image cache that attempts to free up bitmap memory more quickly than if you
 * leave things to the gc.  Seems to be more important on some phones (e.g.
 * Aria) than others - could just be a 2.1 thing
 * <p>
 * Note there's a chance this will be worse than the default behaviour if
 * bitmap recycling is ever properly cleared up.
 */
public class ImageCache<T> extends LruCache<T, Drawable>
{
  private static final String TAG = "ContactImageCache";
  private static final HashSet<BitmapReference> sRefs = new HashSet<BitmapReference>();
  private static final ReferenceQueue<BitmapDrawable> sReferenceQueue = new ReferenceQueue<BitmapDrawable>();

  private static final boolean DEBUG = false;

  private Drawable mUnknownPicture;

  public ImageCache(int maxSize)
  {
    super(maxSize);
  }

  /**
   * The cache needs to know this picture as it is used several times and should
   * not be recycled.
   * @param unknownPicture
   */
  public void setUnknownPicture(Drawable unknownPicture)
  {
    mUnknownPicture = unknownPicture;
  }

  /**
   * Clear the cache.  Will also recycle any bitmaps the cache has pointed to,
   * so only call when those bitmaps will not be redrawn
   */
  public void clear()
  {
    Log.d(TAG, "clear with heap at " + Debug.getNativeHeapAllocatedSize());
    // Dump any bitmaps currently in the cache
    Log.d(TAG, "cache size " + size());
    Log.d(TAG, "sRefs size " + sRefs.size());
    for (Drawable image: snapshot().values())
    {
      if ((image instanceof BitmapDrawable) && (image != mUnknownPicture))
      {
        ((BitmapDrawable)image).getBitmap().recycle();
      }
    }
    // Empty the cache
    evictAll();
    // recycle bitmaps of any drawables that are no longer referenced.
    drainReferenceQueue();
    Log.d(TAG, "After clear native heap " + Debug.getNativeHeapAllocatedSize());

    for (BitmapReference ref : sRefs)
    {
      Log.d(TAG, "ref value " + ref.get());
      Log.d(TAG, "ref enqueued" + ref.isEnqueued());
    }
  }

  /*
   * Check if any of the BitmapDrawables we've ever seen are no longer referenced.
   * <p>
   * Note we have to be a little careful here as we may actually delay recycling
   * the bitmaps since we keep references to them here in this reference queue
   */
  private void drainReferenceQueue()
  {
    BitmapReference imageReference;
    while ((imageReference = (BitmapReference) sReferenceQueue.poll()) != null)
    {
      // image is not referenced elsewhere - can recycle it
      if (DEBUG) Log.d(TAG, "entry in reference queue");
      imageReference.recycle();

      // sRefs keeps a reference to our weak references - so they last long
      // enough to get put in the reference queue.
      sRefs.remove(imageReference);
    }
  }

  public Drawable trackingPut(T id, Drawable image)
  {
    // Clear up anything that we have had in this cache but is now neither in
    // the cache or used anywhere else
    drainReferenceQueue();

    if ((image instanceof BitmapDrawable) && (image != mUnknownPicture))
    {
      // We track bitmap drawables so when they're used nowhere we can free them.
      // The unknown picture is cached in resources and used several times so we
      // exclude it
      sRefs.add(new BitmapReference((BitmapDrawable) image, sReferenceQueue));
    }

    if (DEBUG) Log.d(TAG, "native heap " + Debug.getNativeHeapAllocatedSize());

    return put(id, image);
  }

  private static class BitmapReference extends WeakReference<BitmapDrawable>
  {
    // The weak reference will be cleared and this put on the reference queue
    // when the last strong reference goes.  We track the bitmap within so we
    // can recycle it.  The theory is that this will be faster than waiting for
    // cycles of gc to run finalizers and clear the native memory
    private final Bitmap mBitmap;

    public BitmapReference(BitmapDrawable r,
        ReferenceQueue<? super BitmapDrawable> q)
    {
      super(r, q);
      mBitmap = r.getBitmap();
    }

    public void recycle()
    {
      mBitmap.recycle();
    }

    @Override
    public boolean equals(Object o)
    {
      return mBitmap.equals(((BitmapReference)o).mBitmap);
    }

    @Override
    public int hashCode()
    {
      return mBitmap.hashCode();
    }
  }
}
