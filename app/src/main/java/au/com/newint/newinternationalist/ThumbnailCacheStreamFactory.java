package au.com.newint.newinternationalist;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by pix on 19/03/15.
 */
public class ThumbnailCacheStreamFactory extends CacheStreamFactory {

    File cacheFile;
    CacheStreamFactory source;
    int width;
    int height = -1;

    public ThumbnailCacheStreamFactory(int width, File CacheFileBase, CacheStreamFactory source) {
        super(null,"thumbnail");

        this.source = source;
        this.width = width;

        String[] pathComponents = CacheFileBase.getPath().split("/");
        String baseImageFilename = pathComponents[pathComponents.length -1];
        String fileExtension = null;
        if (baseImageFilename.contains(".")) {
            fileExtension = baseImageFilename.substring(baseImageFilename.lastIndexOf("."));
        } else {
            fileExtension = "";
        }
        String imageForSizeFilename = FilenameUtils.removeExtension(baseImageFilename) + "_" + width + "_" + fileExtension;
        cacheFile = new File(CacheFileBase.getParent(),imageForSizeFilename);

    }

    public ThumbnailCacheStreamFactory(int width, int height, File CacheFileBase, CacheStreamFactory source) {
        super(null,"thumbnail");

        this.source = source;
        this.width = width;
        this.height = height;

        String[] pathComponents = CacheFileBase.getPath().split("/");
        String baseImageFilename = pathComponents[pathComponents.length -1];
        String fileExtension = null;
        if (baseImageFilename.contains(".")) {
            fileExtension = baseImageFilename.substring(baseImageFilename.lastIndexOf("."));
        } else {
            fileExtension = "";
        }
        String imageForSizeFilename = FilenameUtils.removeExtension(baseImageFilename) + "_" + width + "x" + height + "_" + fileExtension;
        cacheFile = new File(CacheFileBase.getParent(),imageForSizeFilename);
    }

    @Override
    protected InputStream createCacheInputStream() {
        // does the cache file exist?
        // if no, make it
        Helpers.debugLog("ThumbnailCSF", "createCacheInputStream ["+cacheFile.getName()+"]");
        if(!cacheFile.exists()) {
            Helpers.debugLog("ThumbnailCSF", "cache miss, creating thumbnail");
            // Scale image for size requested
            //Bitmap fullsizeImageBitmap = BitmapFactory.decodeStream(source.createInputStream());

            // this will block if we haven't seen the full file yet
            // we should be off the main thread here though.
            byte[] data = source.read();
            Bitmap fullsizeImageBitmap = Helpers.scaledBitmapDecode(data,width,height);

            Helpers.debugLog("ThumbnailCSF", "bitmap decoded");
            if (fullsizeImageBitmap != null) {
                // TODO: Work out why this creates jagged images. Is the image size wrong??
                //                    Bitmap scaledCover = Bitmap.createScaledBitmap(fullsizeImageBitmap, width, height, true);

                // Scale image with fixed width and aspect ratio, crop if need be
                // we don't know height yet

                float originalWidth = fullsizeImageBitmap.getWidth(), originalHeight = fullsizeImageBitmap.getHeight();
                float scale = width / originalWidth;
                float height;
                if (this.height < 0) {
                    height = fullsizeImageBitmap.getHeight()*scale;
                } else {
                    height = this.height;
                }


                float xTranslation = 0.0f, yTranslation = (height - originalHeight * scale) / 2.0f;
                Matrix transformation = new Matrix();
                transformation.postTranslate(xTranslation, yTranslation);
                transformation.preScale(scale, scale);
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setDither(true);
                paint.setFilterBitmap(true);
                Bitmap scaledImage = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(scaledImage);
                canvas.drawBitmap(fullsizeImageBitmap, transformation, paint);

                // Save to filesystem
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(cacheFile);
                    // with JPEG, 100% generates files larger than uncompressed
                    scaledImage.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // The image file is corrups!
                Log.e("ThumbnailCSF", "source stream is corrupt!");
                source.invalidate();
                return null;
            }

            Helpers.debugLog("ThumbnailCSF", "thumbnail created");
        } else {
            Helpers.debugLog("ThumbnailCSF", "cache hit");
        }

        // try to serve up fileinputstream
        try {
            return new FileInputStream(cacheFile);
        } catch (FileNotFoundException e) {
            Log.e("ThumbnailCSF", "Thumbnail generation completed but no file created");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected OutputStream createCacheOutputStream() {
        return null;
    }

    @Override
    protected void invalidateCache() {
        cacheFile.delete();
        source.invalidate();
    }
}
