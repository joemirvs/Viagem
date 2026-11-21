package br.com.mapadeviagem.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Escolhe fotos da galeria e lê a localização original (GPS) gravada nelas. */
@CapacitorPlugin(
    name = "PhotoGps",
    permissions = {
        @Permission(alias = "media", strings = { Manifest.permission.READ_MEDIA_IMAGES }),
        @Permission(alias = "legacy", strings = { Manifest.permission.READ_EXTERNAL_STORAGE }),
        @Permission(alias = "location", strings = { Manifest.permission.ACCESS_MEDIA_LOCATION })
    }
)
public class PhotoGpsPlugin extends Plugin {

    @PluginMethod
    public void pick(PluginCall call) {
        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            if (getPermissionState("media") != PermissionState.GRANTED) need.add("media");
        } else if (getPermissionState("legacy") != PermissionState.GRANTED) {
            need.add("legacy");
        }
        if (Build.VERSION.SDK_INT >= 29 && getPermissionState("location") != PermissionState.GRANTED) need.add("location");
        if (!need.isEmpty()) {
            requestPermissionForAliases(need.toArray(new String[0]), call, "permsDone");
            return;
        }
        launch(call);
    }

    @PermissionCallback
    private void permsDone(PluginCall call) {
        launch(call);
    }

    private void launch(PluginCall call) {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(call, i, "picked");
    }

    @ActivityCallback
    private void picked(PluginCall call, ActivityResult result) {
        JSArray arr = new JSArray();
        JSObject ret = new JSObject();
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
            ret.put("files", arr);
            ret.put("gpsPermission", getPermissionState("location") == PermissionState.GRANTED);
            call.resolve(ret);
            return;
        }
        Intent d = result.getData();
        List<Uri> uris = new ArrayList<>();
        if (d.getClipData() != null) {
            for (int k = 0; k < d.getClipData().getItemCount(); k++) uris.add(d.getClipData().getItemAt(k).getUri());
        } else if (d.getData() != null) {
            uris.add(d.getData());
        }
        boolean gpsOk = Build.VERSION.SDK_INT < 29 || getPermissionState("location") == PermissionState.GRANTED;
        int n = 0;
        for (Uri u : uris) {
            try {
                Uri src = u;
                if (Build.VERSION.SDK_INT >= 29) {
                    try {
                        if (DocumentsContract.isDocumentUri(getContext(), u)) {
                            Uri m = MediaStore.getMediaUri(getContext(), u);
                            if (m != null) src = m;
                        }
                    } catch (Exception ignored) {}
                    if (gpsOk) {
                        try { src = MediaStore.setRequireOriginal(src); } catch (Exception ignored) {}
                    }
                }
                String mime = getContext().getContentResolver().getType(u);
                String ext = (mime != null && mime.contains("hei")) ? ".heic" : ".jpg";
                String name = "foto_" + System.currentTimeMillis() + "_" + (n++) + ext;
                File out = new File(getContext().getCacheDir(), name);
                try (InputStream in = getContext().getContentResolver().openInputStream(src); OutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[65536];
                    int r;
                    while ((r = in.read(buf)) > 0) os.write(buf, 0, r);
                }
                JSObject o = new JSObject();
                o.put("path", out.getAbsolutePath());
                o.put("name", name);
                o.put("mime", mime);
                try (InputStream in2 = getContext().getContentResolver().openInputStream(src)) {
                    ExifInterface ex = new ExifInterface(in2);
                    float[] ll = new float[2];
                    if (ex.getLatLong(ll)) { o.put("lat", (double) ll[0]); o.put("lon", (double) ll[1]); }
                    String dt = ex.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
                    if (dt == null) dt = ex.getAttribute(ExifInterface.TAG_DATETIME);
                    if (dt != null) o.put("date", dt);
                } catch (Exception ignored) {}
                arr.put(o);
            } catch (Exception e) {
                // ignora a foto que não pôde ser lida
            }
        }
        ret.put("files", arr);
        ret.put("gpsPermission", gpsOk);
        call.resolve(ret);
    }
}
