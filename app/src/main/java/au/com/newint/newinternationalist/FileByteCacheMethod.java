package au.com.newint.newinternationalist;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;

/**
 * Created by pix on 6/02/15.
 */
public class FileByteCacheMethod extends ByteCacheMethod {

    File cacheFile;

    public FileByteCacheMethod(File cacheFile, String name) {
        super(name);
        this.cacheFile = cacheFile;
    }

    public FileByteCacheMethod(File cacheFile) {
        this(cacheFile, "file");
    }

    @Override
    public ByteCacheHit read() {
        try {
            FileInputStream fileInputStream = new FileInputStream(cacheFile);
            long length = cacheFile.length();
            // truncate length if larger than can fit in an integer
            if (length>Integer.MAX_VALUE) {
                length = Integer.MAX_VALUE;
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((int)length);
            IOUtils.copy(fileInputStream,byteArrayOutputStream);

            Log.i("FileByteCacheMethod", "creating ByteCacheHit");
            return new ByteCacheHit(byteArrayOutputStream.toByteArray(), new Date(cacheFile.lastModified()));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void write(byte[] payload) {
        try {
            FileOutputStream fos = new FileOutputStream(cacheFile);
            IOUtils.write(payload,fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
