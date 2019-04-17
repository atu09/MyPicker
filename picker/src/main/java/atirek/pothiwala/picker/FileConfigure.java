package atirek.pothiwala.picker;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

public class FileConfigure {

    private static File privateDirectory(@NonNull Context context) {
        File cacheDir = context.getCacheDir();
        if (isExternalStorageWritable()) {
            cacheDir = context.getExternalCacheDir();
        }
        File privateDir = new File(cacheDir, getFolderName(context));
        if (!privateDir.exists()) privateDir.mkdirs();
        return privateDir;
    }

    public static File getFile(@NonNull Context context, @NonNull String url) {
        File directory = privateDirectory(context);
        String fileName = String.format(Locale.getDefault(), "%s.%s", generateRandomString(), MimeTypeMap.getFileExtensionFromUrl(url));
        return new File(directory, fileName);
    }

    private static String generateRandomString() {
        return UUID.randomUUID().toString();
    }

    private static String getFolderName(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("folder_name", null);
    }

    private static String getMimeType(@NonNull Context context, @NonNull Uri uri) {
        String extension;
        //Check uri format to avoid null
        if (uri.getScheme() != null && uri.getScheme().equalsIgnoreCase(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());

        }

        return extension;
    }

    private static void writeToFile(InputStream inputStream, File file) {
        try {
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File pickedExistingFile(Context context, Uri uri) throws IOException {
        InputStream pictureInputStream = context.getContentResolver().openInputStream(uri);
        File directory = privateDirectory(context);
        File file = new File(directory, UUID.randomUUID().toString() + "." + getMimeType(context, uri));
        if (file.createNewFile()) {
            writeToFile(pictureInputStream, file);
        }
        return file;
    }

    public static void clearFiles(Context context) {
        File[] files = privateDirectory(context).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    public static File generateNewFile(@NonNull Context context, @NonNull String extension) throws IOException {
        File dir = privateDirectory(context);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return File.createTempFile(UUID.randomUUID().toString(), "." + extension, dir);
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
