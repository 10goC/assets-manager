package cc.elysium.capacitor.plugin.asssets;

import android.Manifest;
import android.os.Environment;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import androidx.core.content.ContextCompat;
import hendrawd.storageutil.library.StorageUtil;

@CapacitorPlugin(
        name = "AssetsManager",
        permissions = { @Permission(strings = { Manifest.permission.WRITE_EXTERNAL_STORAGE }, alias="storage") }
)
public class AssetsManagerPlugin extends Plugin {

    public static final String TAG = "Capacitor/AssetsManager";
    public static final String DATABASE_FILE_DIR = "data_rotel";
    public static final String DATABASE_NAME = "db.db";
    public static final String ZIP_FILENAME = "rotel-fr.zip";
    public static final String PERMISSION_STORAGE_ALIAS = "storage";

    private AssetsManager implementation = new AssetsManager();

    @PermissionCallback
    private void writePermissionCallback(PluginCall call) {
        if (getPermissionState(PERMISSION_STORAGE_ALIAS) == PermissionState.GRANTED) {
            Log.d(TAG, "Storage permission granted");
            writeFileToStorage(call);
        } else {
            Log.w(TAG, "Storage permission denied");
            call.reject("Write permission denied");
        }
    }

    @PluginMethod
    public void getExternalStorageDir(PluginCall call) {
        if (getPermissionState(PERMISSION_STORAGE_ALIAS) != PermissionState.GRANTED) {
            Log.d(TAG, "Requesting storage permission");
            requestPermissionForAlias(PERMISSION_STORAGE_ALIAS, call, "writePermissionCallback");
        } else {
            Log.d(TAG, "Storage permission already granted");
            writeFileToStorage(call);
        }
    }

    private void writeFileToStorage(PluginCall call) {
        File[] dirs = ContextCompat.getExternalFilesDirs(getContext(), null);
        File location = dirs[0];
        String dir = location.getPath() + File.separator + DATABASE_FILE_DIR;

        // Create destination directory
        File d = new File(dir);
        if (!d.exists()) {
            if (!d.mkdir()) {
                // Reject plugin call, no writing permissions
                call.reject("Can't write to " + d.getPath());
                return;
            }
        }

        // Check for SD card
        File externalZipFile;
        String[] externalStoragePaths = StorageUtil.getStorageDirectories(getContext());
        Log.d(TAG, "Looping through " + externalStoragePaths.length + " external paths");
        for (String sdPath: externalStoragePaths) {
            if (sdPath.substring(sdPath.length() - 1) != File.separator) {
                sdPath += File.separator;
            }
            externalZipFile = new File(sdPath + ZIP_FILENAME);
            if (externalZipFile.exists()) {
                try {
                    Log.d(TAG, "Extracting zip from SD card " + sdPath + ZIP_FILENAME);
                    InputStream zipstream = new FileInputStream(externalZipFile);
                    extractZip(zipstream, d, call);
                    JSObject ret = new JSObject();
                    ret.put("path", d.getPath());
                    ret.put("uri", d.toURI());
                    call.resolve(ret);
                    return;
                } catch (IOException e) {
                    call.reject(e.getMessage());
                    return;
                }
            } else {
                Log.d(TAG, sdPath + ZIP_FILENAME + " not found");
            }
        }

        // Check for db file
        File f = new File(d, DATABASE_NAME);
        if (f.exists()) {
            Log.d(TAG, "Found DB File " + d.getPath());
            JSObject ret = new JSObject();
            ret.put("path", d.getPath());
            ret.put("uri", d.toURI());

            // Resolve plugin call, return directory path
            call.resolve(ret);
            return;
        } else {
            Log.d(TAG, "DB File not found");
        }

        // Extract DB and photos from assets
        try {
            Log.d(TAG, "Extracting zip from assets");
            String zipPath = "public/assets/" + ZIP_FILENAME;

            InputStream zipstream = getContext().getAssets().open(zipPath);

            extractZip(zipstream, d, call);

            JSObject ret = new JSObject();
            ret.put("path", d.getPath());
            ret.put("uri", d.toURI());
            call.resolve(ret);
            return;
        } catch (IOException e) {
            call.reject(e.getMessage());
            return;
        }
    }

    private void extractZip(InputStream zipstream, File d, PluginCall call) {
        ZipInputStream z = new ZipInputStream(zipstream);
        try {
            ZipEntry ze = z.getNextEntry();
            while (ze != null) {
                File file = new File(d, ze.getName());

                if (ze.isDirectory()) {
                    if (file.exists() && !file.isDirectory()) {
                        file.delete();
                    }
                    if (!file.exists()) {
                        file.mkdir();
                    }
                } else {
                    FileOutputStream fileout = new FileOutputStream(file);
                    byte[] buffer = new byte[2048];
                    BufferedOutputStream out = new BufferedOutputStream(fileout, buffer.length);
                    int len;
                    while ((len = z.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }

                    Log.d(TAG, "File extracted " + ze.getName());

                    out.flush();
                    out.close();
                    fileout.close();
                }
                z.closeEntry();
                ze = z.getNextEntry();
            }
            zipstream.close();
            z.close();
        } catch (IOException e) {
            call.reject(e.getMessage());
        }
    }
}
