package atirek.pothiwala.picker;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
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
        int REQUEST_CAMERA = 7456;
        int REQUEST_DOCUMENTS = 7457;
        int REQUEST_VIDEO = 7458;
        int REQUEST_PHOTO = 7459;
    }

    public interface Callbacks {
        void onPickerError(Exception e, FileSource source);

        void onPicked(File dataFile, FileSource source);

        void onCanceled(FileSource source);
    }

    public enum FileSource {
        CAMERA, PHOTO, VIDEO, DOCUMENTS
    }

    private Activity activity;
    private String lastImageUri = null;
    private String lastImagePath = null;

    private FilePicker(Activity activity, String folder) {
        this.activity = activity;
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit().putString("folder_name", folder)
                .apply();
    }

    private FilePicker(Fragment fragment, String folder) {
        this.activity = fragment.getActivity();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit().putString("folder_name", folder)
                .apply();
    }

    private Uri createCameraPictureFile() throws IOException {
        File imagePath = FileConfigure.generateNewFile(activity, "jpg");
        String authority = String.format(Locale.getDefault(), "%s.%s", activity.getPackageName(), "fileprovider");
        Uri uri = FileProvider.getUriForFile(activity, authority, imagePath);
        lastImageUri = uri.toString();
        lastImagePath = imagePath.toString();
        return uri;
    }

    private Intent createDocumentsIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        return intent;
    }

    private Intent createCameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            Uri capturedImageUri = createCameraPictureFile();
            //We have to explicitly grant the write permission since Intent.setFlag works only on API Level >=20
            grantWritePermission(activity, intent, capturedImageUri);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return intent;
    }

    private Intent createIntent(FileSource fileSource) {
        if (fileSource == FileSource.PHOTO) {
            return new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        } else if (fileSource == FileSource.VIDEO) {
            return new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        } else if (fileSource == FileSource.CAMERA) {
            return createCameraIntent();
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
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add("Camera");
        arrayAdapter.add("Photo");
        arrayAdapter.add("Video");
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
                if (position == FileSource.CAMERA.ordinal()) {
                    openPicker(FileSource.CAMERA);
                } else if (position == FileSource.PHOTO.ordinal()) {
                    openPicker(FileSource.PHOTO);
                } else if (position == FileSource.VIDEO.ordinal()) {
                    openPicker(FileSource.VIDEO);
                } else {
                    openPicker(FileSource.DOCUMENTS);
                }
            }
        });
        builder.show();
    }

    private void onFilePick(Intent data, FileSource fileSource, Callbacks callbacks) {
        try {
            Uri photoPath = data.getData();
            File photoFile = FileConfigure.pickedExistingFile(activity, photoPath);
            callbacks.onPicked(photoFile, fileSource);
        } catch (Exception e) {
            e.printStackTrace();
            callbacks.onPickerError(e, fileSource);
        }
    }

    private void onCamera(FileSource fileSource, Callbacks callbacks) {
        try {
            if (!TextUtils.isEmpty(lastImageUri)) {
                revokeWritePermission(activity, Uri.parse(lastImageUri));
            }

            File photoFile = null;
            if (lastImagePath != null) {
                photoFile = new File(lastImagePath);
            }

            if (photoFile == null) {
                Exception exception = new IllegalStateException("Unable to capture photo from camera.");
                callbacks.onPickerError(exception, fileSource);
            } else {
                callbacks.onPicked(photoFile, fileSource);
            }

            lastImagePath = null;
            lastImageUri = null;

        } catch (Exception e) {
            e.printStackTrace();
            callbacks.onPickerError(e, fileSource);
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data, Callbacks callbacks) {
        if (requestCode == Requests.REQUEST_DOCUMENTS || requestCode == Requests.REQUEST_CAMERA
                || requestCode == Requests.REQUEST_PHOTO || requestCode == Requests.REQUEST_VIDEO) {

            FileSource fileSource = getFileSource(requestCode);
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                if (fileSource != FileSource.CAMERA) {
                    onFilePick(data, fileSource, callbacks);
                } else {
                    onCamera(fileSource, callbacks);
                }
            } else {
                callbacks.onCanceled(fileSource);
            }
        }
    }

    public File lastlyTakenButCanceledPhoto() {
        if (lastImagePath == null) return null;
        File file = new File(lastImagePath);
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
            case CAMERA:
                return Requests.REQUEST_CAMERA;
            case PHOTO:
                return Requests.REQUEST_PHOTO;
            case VIDEO:
                return Requests.REQUEST_VIDEO;
            case DOCUMENTS:
                return Requests.REQUEST_DOCUMENTS;
        }
        return Requests.REQUEST_PHOTO;
    }

    private static FileSource getFileSource(int request) {
        switch (request) {
            case Requests.REQUEST_CAMERA:
                return FileSource.CAMERA;
            case Requests.REQUEST_PHOTO:
                return FileSource.PHOTO;
            case Requests.REQUEST_VIDEO:
                return FileSource.VIDEO;
            case Requests.REQUEST_DOCUMENTS:
                return FileSource.DOCUMENTS;
        }
        return FileSource.PHOTO;
    }
}
