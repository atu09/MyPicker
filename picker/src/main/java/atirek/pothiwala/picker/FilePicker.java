package atirek.pothiwala.picker;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class FilePicker {

    public interface Requests {
        int REQUEST_PHOTO_CAPTURE = 7454;
        int REQUEST_AUDIO_CAPTURE = 7455;
        int REQUEST_VIDEO_CAPTURE = 7456;
        int REQUEST_PHOTO_GALLERY = 7457;
        int REQUEST_AUDIO_GALLERY = 7458;
        int REQUEST_VIDEO_GALLERY = 7459;
        int REQUEST_DOCUMENTS = 7460;
    }

    public interface Callbacks {
        void onPickerError(Exception e, FileSource source);

        void onPicked(File dataFile, FileSource source);

        void onCanceled(FileSource source);
    }

    public enum FileSource {
        PHOTO_CAPTURE, AUDIO_CAPTURE, VIDEO_CAPTURE, PHOTO_GALLERY, AUDIO_GALLERY, VIDEO_GALLERY, DOCUMENTS
    }

    private Activity activity;
    private String lastFileUri = null, lastFilePath = null;
    private int recordingLimit = 30;

    public void setRecordingLimit(int recordingLimit) {
        this.recordingLimit = recordingLimit;
    }

    public FilePicker(Activity activity, String folder) {
        this.activity = activity;
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit().putString("folder_name", folder)
                .apply();
    }

    public FilePicker(Fragment fragment, String folder) {
        this.activity = fragment.getActivity();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit().putString("folder_name", folder)
                .apply();
    }

    private Uri createSourceFile(String extension) throws IOException {
        File imagePath = FileConfigure.generateNewFile(activity, extension);
        String authority = String.format(Locale.getDefault(), "%s.%s", activity.getPackageName(), "fileprovider");
        Uri uri = FileProvider.getUriForFile(activity, authority, imagePath);
        lastFileUri = uri.toString();
        lastFilePath = imagePath.toString();
        return uri;
    }

    private Intent createDocumentsIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType("*/*");
        intent.setType("application/pdf|application/msword");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String[] mimeTypes = {"application/pdf", "application/msword"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }
        return intent;
    }

    private Intent createPhotoCaptureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            Uri capturedFileUri = createSourceFile("jpg");
            //We have to explicitly grant the write permission since Intent.setFlag works only on API Level >=20
            grantWritePermission(activity, intent, capturedFileUri);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedFileUri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return intent;
    }

    private Intent createVideoCaptureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        try {
            Uri capturedFileUri = createSourceFile("mp4");
            //We have to explicitly grant the write permission since Intent.setFlag works only on API Level >=20
            grantWritePermission(activity, intent, capturedFileUri);

            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, recordingLimit);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedFileUri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return intent;
    }

    private Intent createAudioCaptureIntent() {
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        try {
            Uri capturedFileUri = createSourceFile("mp3");
            //We have to explicitly grant the write permission since Intent.setFlag works only on API Level >=20
            grantWritePermission(activity, intent, capturedFileUri);

            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, recordingLimit);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedFileUri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return intent;
    }

    private Intent createIntent(FileSource fileSource) {
        if (fileSource == FileSource.PHOTO_CAPTURE) {
            return createPhotoCaptureIntent();
        } else if (fileSource == FileSource.AUDIO_CAPTURE) {
            return createAudioCaptureIntent();
        } else if (fileSource == FileSource.VIDEO_CAPTURE) {
            return createVideoCaptureIntent();
        } else if (fileSource == FileSource.PHOTO_GALLERY) {
            return new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        } else if (fileSource == FileSource.AUDIO_GALLERY) {
            return new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        } else if (fileSource == FileSource.VIDEO_GALLERY) {
            return new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        } else {
            return createDocumentsIntent();
        }
    }

    public void openPicker(FileSource fileSource) {
        Intent intent = createIntent(fileSource);
        activity.startActivityForResult(intent, getRequest(fileSource));
    }

    public void openPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select an action");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, R.layout.cell_picker);
        arrayAdapter.add("Capture Photo");
        arrayAdapter.add("Record Audio");
        arrayAdapter.add("Record Video");
        arrayAdapter.add("Photo Gallery");
        arrayAdapter.add("Audio Gallery");
        arrayAdapter.add("Video Gallery");
        arrayAdapter.add("Document");

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                if (position == FileSource.PHOTO_CAPTURE.ordinal()) {
                    openPicker(FileSource.PHOTO_CAPTURE);
                } if (position == FileSource.AUDIO_CAPTURE.ordinal()) {
                    openPicker(FileSource.AUDIO_CAPTURE);
                } if (position == FileSource.VIDEO_CAPTURE.ordinal()) {
                    openPicker(FileSource.VIDEO_CAPTURE);
                } else if (position == FileSource.PHOTO_GALLERY.ordinal()) {
                    openPicker(FileSource.PHOTO_GALLERY);
                } else if (position == FileSource.AUDIO_GALLERY.ordinal()) {
                    openPicker(FileSource.AUDIO_GALLERY);
                } else if (position == FileSource.VIDEO_GALLERY.ordinal()) {
                    openPicker(FileSource.VIDEO_GALLERY);
                } else {
                    openPicker(FileSource.DOCUMENTS);
                }
            }
        });
        builder.show();
    }

    private void onFilePick(Intent data, FileSource fileSource, Callbacks callbacks) {
        try {
            Uri filePath = data.getData();
            File dataFile = FileConfigure.pickedExistingFile(activity, filePath);
            callbacks.onPicked(dataFile, fileSource);
        } catch (Exception e) {
            e.printStackTrace();
            callbacks.onPickerError(e, fileSource);
        }
    }

    private void onCapture(FileSource fileSource, Callbacks callbacks) {
        try {
            if (!TextUtils.isEmpty(lastFileUri)) {
                revokeWritePermission(activity, Uri.parse(lastFileUri));
            }

            File dataFile = null;
            if (lastFilePath != null) {
                dataFile = new File(lastFilePath);
            }

            if (dataFile == null) {
                Exception exception = new IllegalStateException("Unable to capture photo / video from camera.");
                callbacks.onPickerError(exception, fileSource);
            } else {
                callbacks.onPicked(dataFile, fileSource);
            }

            lastFilePath = null;
            lastFileUri = null;

        } catch (Exception e) {
            e.printStackTrace();
            callbacks.onPickerError(e, fileSource);
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data, Callbacks callbacks) {

        boolean isRequested = false;
        switch (requestCode) {
            case Requests.REQUEST_PHOTO_CAPTURE:
            case Requests.REQUEST_AUDIO_CAPTURE:
            case Requests.REQUEST_VIDEO_CAPTURE:
            case Requests.REQUEST_PHOTO_GALLERY:
            case Requests.REQUEST_AUDIO_GALLERY:
            case Requests.REQUEST_VIDEO_GALLERY:
            case Requests.REQUEST_DOCUMENTS:
                isRequested = true;
        }

        if (isRequested) {
            FileSource fileSource = getFileSource(requestCode);
            if (resultCode == Activity.RESULT_OK) {
                if (fileSource != FileSource.PHOTO_CAPTURE && fileSource != FileSource.AUDIO_CAPTURE && fileSource != FileSource.VIDEO_CAPTURE) {
                    onFilePick(data, fileSource, callbacks);
                } else {
                    onCapture(fileSource, callbacks);
                }

            } else {
                callbacks.onCanceled(fileSource);
            }
        }
    }

    public File lastlyTakenButCanceledPhoto() {
        if (lastFilePath == null) return null;
        File file = new File(lastFilePath);
        if (file.exists()) {
            return file;
        } else {
            return null;
        }
    }

    private static void revokeWritePermission(Context context, Uri uri) {
        context.revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static void grantWritePermission(Context context, Intent intent, Uri uri) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    private static int getRequest(FileSource fileSource) {
        switch (fileSource) {
            case PHOTO_CAPTURE:
                return Requests.REQUEST_PHOTO_CAPTURE;
            case AUDIO_CAPTURE:
                return Requests.REQUEST_AUDIO_CAPTURE;
            case VIDEO_CAPTURE:
                return Requests.REQUEST_VIDEO_CAPTURE;
            case PHOTO_GALLERY:
                return Requests.REQUEST_PHOTO_GALLERY;
            case AUDIO_GALLERY:
                return Requests.REQUEST_AUDIO_GALLERY;
            case VIDEO_GALLERY:
                return Requests.REQUEST_VIDEO_GALLERY;
            case DOCUMENTS:
                return Requests.REQUEST_DOCUMENTS;
        }
        return Requests.REQUEST_PHOTO_GALLERY;
    }

    private static FileSource getFileSource(int request) {
        switch (request) {
            case Requests.REQUEST_PHOTO_CAPTURE:
                return FileSource.PHOTO_CAPTURE;
            case Requests.REQUEST_AUDIO_CAPTURE:
                return FileSource.AUDIO_CAPTURE;
            case Requests.REQUEST_VIDEO_CAPTURE:
                return FileSource.VIDEO_CAPTURE;
            case Requests.REQUEST_PHOTO_GALLERY:
                return FileSource.PHOTO_GALLERY;
            case Requests.REQUEST_AUDIO_GALLERY:
                return FileSource.AUDIO_GALLERY;
            case Requests.REQUEST_VIDEO_GALLERY:
                return FileSource.VIDEO_GALLERY;
            case Requests.REQUEST_DOCUMENTS:
                return FileSource.DOCUMENTS;
        }
        return FileSource.PHOTO_GALLERY;
    }
}
