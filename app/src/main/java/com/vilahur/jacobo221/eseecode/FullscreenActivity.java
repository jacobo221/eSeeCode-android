package com.vilahur.jacobo221.eseecode;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE = 0x11;
    private static final int REQUEST_WRITE_CONFIRM = 0x12;
    private static WebView myBrowser = null;
    private String openFilePath = null;

    @Override
    public void onStart() {
        super.onStart();
        final android.content.Intent intent = getIntent ();

        if (intent != null)
        {
            final android.net.Uri data = intent.getData ();

            if (data != null)
            {
                openFilePath = data.getPath();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
/*
        // Fullscreen
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
 */
        setContentView(R.layout.activity_fullscreen);
        myBrowser = (WebView) findViewById(R.id.webView);
        myBrowser.getSettings().setDefaultTextEncodingName("utf-8");
        myBrowser.getSettings().setJavaScriptEnabled(true);
        myBrowser.getSettings().setDisplayZoomControls(false);
        myBrowser.getSettings().setBuiltInZoomControls(true);
        myBrowser.getSettings().setLoadWithOverviewMode(true);
        myBrowser.getSettings().setUseWideViewPort(true);
        myBrowser.getSettings().setAllowFileAccess(true);
        myBrowser.loadUrl("file:///android_asset" + File.separator + "eseecode-master" + File.separator + "index.html");

        // Inject code to open files
        myBrowser.setWebChromeClient(new WebChromeClient() {
            //The undocumented magic method override

            // for Lollipop, all in one
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                return true;
            }

            // openFileChooser for Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            }

            // openFileChooser for Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            }

            // openFileChooser for other Android versions
            /* may not work on KitKat due to lack of implementation of openFileChooser() or onShowFileChooser()
               https://code.google.com/p/android/issues/detail?id=62220
               however newer versions of KitKat fixed it on some devices */
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            }
        });

        // Inject code to save files
        JavaScriptInterface jsInterface = new JavaScriptInterface(this);
        myBrowser.addJavascriptInterface(jsInterface, "JSInterface");
        myBrowser.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Inject Android-Browser framework
                byte[] buffer = ("function $e_injectSaveData() {" +
                        "$e_saveFile = function(data, filename, mimetype) {" +
                        "   window.JSInterface.eseecodeSaveData(data, filename, mimetype);" +
                        "   setTimeout(function() {" +
                        "       var result = window.JSInterface.eseecodeSavedFile(filename);" +
                        "       if (result === 'exists') {" +
                        "           if (window.$e_usingFile !== filename) {" +
                        "               $e_msgBox(_('File \\'%s\\' already exists. Do you want to overwrite it?',[filename]), { acceptAction: function(){$e_androidConfirmSave(filename);}, cancel: true });" +
                        "           } else {" +
                        "               $e_androidConfirmSave(filename);" +
                        "           }" +
                        "       } else if (result === 'success') {" +
                        "           API_updateSavedTime();" +
                        "           window.$e_usingFile = filename;" +
                        "       }" +
                        "   }, 100);" +
                        "   var $e_androidConfirmSave = function(inFilename) {" +
                        "       if (inFilename !== undefined) {" +
                        "           filename = inFilename;" +
                        "       }" +
                        "       window.JSInterface.eseecodeSaveDataConfirm(filename);" +
                        "       $e_msgBoxClose();" +
                        "       setTimeout(function() {" +
                        "           if (window.JSInterface.eseecodeSavedFile(filename) === 'success') {" +
                        "               API_updateSavedTime();" +
                        "               window.$e_usingFile = filename;" +
                        "           }" +
                        "       }, 100);" +
                        "   };" +
                        "};" +
                        "window.$e_openCodeFileHandler = function(event) {" +
                        "   var target = JSON.parse(window.JSInterface.eseecodeOpenFile());" +
                        "   var listFilesHTML = '<u>'+target[0]+'</u>:<br />';" +
                        "   for (var i=1; i<target.length; i++) {" +
                        "       listFilesHTML += '<div>';" +
                        "       listFilesHTML +=    '<a class=\"tab-button\" style=\"float:left\" onclick=\"$e_openReadFile(\\''+target[i]+'\\')\">'+target[i]+'</a>';" +
                        "       listFilesHTML += '</div>';" +
                        "   }" +
                        "   $e_msgBox(listFilesHTML, { acceptName: _('Cancel') });" +
                        "};" +
                        "window.$e_openReadFile = function(filename) {" +
                        "   var target = JSON.parse(window.JSInterface.eseecodeReadFile(filename));" +
                        "   var code = unescape(target.data);" +
                        "   var filename = target.filename;" +
                        "   $e_loadFile(code, filename, $e_loadCodeFile);" +
                        "   window.$e_usingFile = filename;" +
                        "   $e_msgBoxClose();" +
                        "};" +
                        "$_eseecode.i18n.available.ca['File \\'%s\\' already exists. Do you want to overwrite it?'] = 'El archivo \\'%s\\' ya existe. ¿Quieres sobreescribirlo?';" +
                        "$_eseecode.i18n.available.es['File \\'%s\\' already exists. Do you want to overwrite it?'] = 'L\\'arxiu \\'%s\\' ja existeix. Vols sobreescriure\\'l?';" +
                    "}").getBytes(Charset.forName("UTF-8"));
                // String-ify the script byte-array using BASE64 encoding
                String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
                view.loadUrl("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var script = document.createElement('script');" +
                        "script.type = 'text/javascript';" +
                        // Tell the browser to BASE64-decode the string into your script
                        "script.innerHTML = window.atob('" + encoded + "');" +
                        "parent.appendChild(script);" +
                        "})()");
                view.loadUrl("javascript:setTimeout($e_injectSaveData(), 500)");

                // Setup
                String language = Locale.getDefault().getLanguage();
                view.loadUrl("javascript:$e_loadURLParams('fullscreenmenu=no&lang=" + language + "')");
                view.loadUrl("javascript:document.body.onload = function() {" +
                            "   API_showFilemenu(false, true);" +
                            "   API_switchLanguage('"+language+"', true);" +
                            "}");
                if (openFilePath != null) {
                    try {
                        FileInputStream fis = new FileInputStream(new File(openFilePath));
                        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line+"\n");
                        }
                        reader.close();
                        fis.close();
                        String data = sb.toString();
                        String code = Uri.encode(data);
                        myBrowser.loadUrl("javascript:(function(){var code=unescape('"+code+"');API_uploadCode(code);})()");
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    public byte[] data = null;
    public String filename = null;
    public String mimeType = null;
    public String writeFileStatus = null;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE || requestCode == REQUEST_WRITE_CONFIRM) {
            writeFileStatus = "failed";
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String dirPath = getAppPath(mimeType);
                File eseecodeDir = new File(dirPath);
                boolean eseecodeDirExists = true;
                if (!eseecodeDir.exists()) {
                    eseecodeDirExists = eseecodeDir.mkdir();
                }
                if (eseecodeDirExists) {
                    try {
                        String filePath = dirPath + File.separator + filename;
                        File myFile = new File(filePath);
                        boolean fileExists = myFile.exists();
                        if (fileExists && !myFile.isFile()) {
                            throw new Exception("Invalid path!");
                        } else if (fileExists && requestCode == REQUEST_WRITE) {
                            writeFileStatus = "exists";
                        } else {
                            myFile.createNewFile();
                            FileOutputStream fOut = new FileOutputStream(myFile);
                            DataOutputStream myOutStream = new DataOutputStream(fOut);
                            myOutStream.write(data);
                            myOutStream.flush();
                            myOutStream.close();
                            fOut.close();
                            // Open image
                            if (mimeType.indexOf("image/") == 0) {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(myFile), "image/*");
                                startActivity(intent);
                            }
                            Toast.makeText(getBaseContext(), String.format(getResources().getString(R.string.downloaded_info), filename), Toast.LENGTH_SHORT).show();
                            writeFileStatus = "success";
                            data = null;
                        }
                    } catch (Exception e) {
                        Toast.makeText(getBaseContext(), "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getBaseContext(), "Unable to create "+dirPath, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "PERMISSION_DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public class JavaScriptInterface {
        private Activity activity;

        public JavaScriptInterface(Activity activity) {
            this.activity = activity;
        }

        // List files in directory
        @JavascriptInterface
        public String eseecodeOpenFile() {
            return eseecodeOpenFile(getAppPath(""));
        }

        // List files in directory
        @JavascriptInterface
        public String eseecodeOpenFile(String path) {
            String object = "[ \"" + path + "\", ";
            try {
                File f = new File(path);
                File files[] = f.listFiles();
                Arrays.sort(files);
                String data = "";
                for (File file : files) {
                    if (file.isFile()) {
                        data += "\"" + file.getName() + "\", ";
                    }
                }
                object += data.substring(0, data.length() - 2); // Remove last ", "
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_SHORT).show();
            }
            object += " ]";
            return object;
        }

        @JavascriptInterface
        public String eseecodeReadFile(String filename) {
            if (filename.length() == 0) {
                filename = "code.esee";
            }
            if (filename.contains(File.separator)) {
                Toast.makeText(getApplicationContext(), "Invalid path!", Toast.LENGTH_LONG).show();
                return "undefined";
            }
            String filePath = getAppPath("") + File.separator + filename;
            String data = null;
            try {
                FileInputStream fis = new FileInputStream(new File(filePath));
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line+"\n");
                }
                reader.close();
                fis.close();
                data = sb.toString();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return "{ " +
                    "\"filename\": \"" + filename + "\", " +
                    "\"data\": \"" + Uri.encode(data) + "\"" +
                    " }";
        }

        @JavascriptInterface
        public void eseecodeSaveData(String inData, String inFilename, String mimetype) {
            if (inFilename.contains(File.separator)) {
                filename = inFilename;
                writeFileStatus = "invalid path";
                Toast.makeText(getApplicationContext(), "Invalid path!", Toast.LENGTH_LONG).show();
                return;
            }
            mimeType = mimetype;
            if (mimetype.indexOf("text/") == 0) {
                data = inData.getBytes();
            } else {
                data = Base64.decode(inData, Base64.NO_WRAP);
            }
            filename = inFilename;
            String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity, permissions, REQUEST_WRITE);
            } else {
                onRequestPermissionsResult(REQUEST_WRITE, permissions, new int[]{PackageManager.PERMISSION_GRANTED});
            }
        }

        @JavascriptInterface
        public String eseecodeSavedFile(String inFilename) {
            if (filename.equals(inFilename)) {
                return writeFileStatus;
            } else {
                return null;
            }
        }

        @JavascriptInterface
        public void eseecodeSaveDataConfirm(String inFilename) {
            if (!filename.equals(inFilename)) {
                return;
            }
            // Code is still in 'data' global variable
            String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity, permissions, REQUEST_WRITE_CONFIRM);
            } else {
                onRequestPermissionsResult(REQUEST_WRITE_CONFIRM, permissions, new int[]{PackageManager.PERMISSION_GRANTED});
            }
        }
    }

    private String getAppPath(String mimetype) {
        if (mimetype.indexOf("image/") == 0) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + filename;
                } else {
                    return Environment.getExternalStorageDirectory().getPath() + File.separator + "eSeeCode" + File.separator + "Pictures";
                }
            } else {
                return getFilesDir().getPath() + File.separator + "Pictures" + File.separator + filename;
            }
        } else {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    return Environment.getExternalStorageDirectory().getPath() + File.separator + "eSeeCode";
                } else {
                    return getExternalFilesDir(null).getPath();
                }
            } else {
                return getFilesDir().getPath();
            }
        }
    }

}