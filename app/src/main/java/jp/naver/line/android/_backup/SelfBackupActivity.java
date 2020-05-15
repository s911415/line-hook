package jp.naver.line.android._backup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SelfBackupActivity extends Activity {
    private static final int REQUEST_CODE_WRITE_EXT_STORAGE = 101;
    private static final int CHOOSE_RESTORE_FILE = 102;
    private static final String RESTORE_INTENT_KEY = "FROM_RESTORE";
    private static final String RESTORE_FILE_PATH_KEY = "RESTORE_FROM";

    private String path;
    private File backupTargetFolder = null;
    private TextView consoleView;
    private ScrollView scrollView;
    private Button backupBtn, restoreBtn;

//    enum Mode {
//        Bak, Res, Hook
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermission();

        // setContentView(R.layout.activity_self_backup);
        // consoleView = findViewById(R.id.bakConsoleText);
        // scrollView = findViewById(R.id.bakScrollView);
        createView();
        path = getApplicationInfo().dataDir;
        try {
            exec("chmod -R 775 " + path);
            exec("ls -al " + path);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
/*      Mode mode = Mode.Hook;
        try {
            System.out.println("Base Path: " + path);
            //exec("chmod -R 777 " + path);
            exec("ls -al " + path);
            String[] backFolders = new String[]{"databases", "files", "no_backup", "shared_prefs"};
            if (true) {

            } else if (mode == Mode.Bak) {
                for (int i = 0; i < backFolders.length; i++) {
                    File src = new File(path, backFolders[i]);
                    File dst = new File(path, (i + 1) + ".tar");
                    exec("tar cvf " + dst.toString() + " " + src + "/");
                    exec("chmod 777 " + dst.toString());
                }
            } else if (mode == Mode.Bak) {
                for (int i = 1; i <= backFolders.length; i++) {
                    String fn = i + ".tar";
                    InputStream raw = getAssets().open(fn);
                    File f = new File(path, fn);
                    FileOutputStream fos = new FileOutputStream(f, false);
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = raw.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    raw.close();

                    exec("tar xvf " + f.toString() + " -C " + path);
                    f.delete();
                }

                // exec("rm -f " + new File(path, "1.tar").toString());
                // exec("rm -f " + new File(path, "2.tar").toString());
                // exec("rm -f " + new File(path, "3.tar").toString());
                // exec("rm -f " + new File(path, "4.tar").toString());
            } else {
                exec("chmod -R 775 " + path);
            }


            exec("ls -al " + path);

        } catch (Exception e) {
            e.printStackTrace();
        }*/
        backupBtn.setOnClickListener(v -> {
            backup();
        });
        restoreBtn.setOnClickListener(v -> {
            restore(null);
        });
    }

    private void createView() {
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content).getRootView();
        LinearLayout parent = new LinearLayout(this);
        int DP_16 = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                getResources().getDisplayMetrics()
        );
        {
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            parent.setLayoutParams(layout);
            parent.setBackgroundColor(Color.BLACK);
            parent.setPadding(DP_16, DP_16, DP_16, DP_16);
            parent.setOrientation(LinearLayout.VERTICAL);
            root.addView(parent);
        }

        LinearLayout actionLayer = new LinearLayout(this);
        {
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layout.setMarginEnd(DP_16);
            actionLayer.setLayoutParams(layout);
            actionLayer.setOrientation(LinearLayout.HORIZONTAL);
            parent.addView(actionLayer);
        }
        {
            Button btn = backupBtn = new Button(this);
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );
            layout.setMarginEnd(DP_16 >> 1);
            btn.setPadding(0, DP_16 << 1, 0, DP_16 << 1);
            btn.setText("Backup");
            btn.setLayoutParams(layout);
            actionLayer.addView(btn);
        }
        {
            Button btn = restoreBtn = new Button(this);
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );
            layout.setMarginStart(DP_16 >> 1);
            btn.setPadding(0, DP_16 << 1, 0, DP_16 << 1);
            btn.setText("Restore");
            btn.setLayoutParams(layout);
            actionLayer.addView(btn);
        }
        {
            scrollView = new ScrollView(this);
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            scrollView.setLayoutParams(layout);
            parent.addView(scrollView);
        }
        {
            TextView text = consoleView = new TextView(this);
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            text.setLayoutParams(layout);
            text.setTextColor(Color.WHITE);

            text.setLineSpacing(text.getLineSpacingExtra(), 1.25f);
            Typeface typeface = Typeface.create("monospace", Typeface.NORMAL);
            text.setTypeface(typeface);
            scrollView.addView(text);
        }
    }

    private int exec(String cmd) throws InterruptedException, IOException {
        appendToConsole(cmd);
        System.out.println("EXEC: " + cmd);
        Process proc = Runtime.getRuntime().exec(cmd);
        System.out.println("----------");
        System.out.println("Stdout:");
        streamToString(proc.getInputStream(), System.out);
        System.out.println("----------");
        System.out.println("StdErr:");
        streamToString(proc.getErrorStream(), System.err);
        System.out.println("----------");

        int exitCode = proc.waitFor();
        System.out.println("Exit Code: " + exitCode);
        System.out.println("----------");
        System.out.println("=============================================");

        return exitCode;
    }

    private void backup() {
        final String[] backFolders = new String[]{"databases", "files", "no_backup", "shared_prefs"};

        final File targetFile = new File(
                backupTargetFolder,
                "LINE_"
                        + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Calendar.getInstance().getTime())
                        + ".tar.gz"
        );
        final File tmpFile = new File(
                getExternalCacheDir(),
                targetFile.getName()
        );

        targetFile.getParentFile().mkdirs();
        tmpFile.getParentFile().mkdirs();

        StringBuilder sb = new StringBuilder();

        sb
                .append("tar")
                .append(' ').append("-C")
                .append(' ').append(path)
                .append(' ').append("-zcvf")
                .append(' ').append(tmpFile.getAbsolutePath());

        for (String bakFolder : backFolders) {
            sb.append(' ').append(bakFolder);
        }

        try {
            exec("ls -al " + path);

            showMessage("Start backup.");
            exec(sb.toString());
            if (tmpFile.exists()) {
                tmpFile.renameTo(targetFile);

                showMessage("Backup to \"" + targetFile.getAbsolutePath() + "\".");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restore(Intent oldIntent) {
        boolean isFromRestore = oldIntent != null && oldIntent.getBooleanExtra(RESTORE_INTENT_KEY, false);
        if (!isFromRestore) {
            Intent intent = new Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(Intent.createChooser(intent, "Select a file"), CHOOSE_RESTORE_FILE);
            return;
        }

        final File bakFile = new File(oldIntent.getStringExtra(RESTORE_FILE_PATH_KEY));
        final File tmpFile = new File(
                getExternalCacheDir(),
                bakFile.getName()
        );
        tmpFile.getParentFile().mkdirs();

        try (FileInputStream fis = new FileInputStream(bakFile)) {
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {

                copyStream(fis, fos);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        if (tmpFile.exists()) {

            StringBuilder sb = new StringBuilder();

            sb
                    .append("tar")
                    .append(' ').append("-C")
                    .append(' ').append(path)
                    .append(' ').append("-zxvf")
                    .append(' ').append(tmpFile.getAbsolutePath());

            try {
                showMessage("Restore file from \"" + tmpFile.getAbsolutePath() + "\".");
                exec(sb.toString());
                showMessage("Restored.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            showMessage("Cleanup...");
            tmpFile.delete();
            showMessage("Finish.");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean isFromRestore = intent.getBooleanExtra(RESTORE_INTENT_KEY, false);
        if (isFromRestore) {
            restore(intent);
        }
    }

    private String streamToString(InputStream is, PrintStream ps) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String tmp = null;
        while ((tmp = reader.readLine()) != null) {
            if (ps != null) ps.println(tmp);
            stringBuilder.append(tmp).append("\n");
            appendToConsole(tmp);
        }
        return stringBuilder.toString();

    }

    private void checkAndRequestPermission() {
        final String[] permissions = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        updateBackupTargetFolder();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        int grantCnt = 0;
        for (String p : permissions) {
            if (checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED)
                grantCnt++;
        }

        if (grantCnt == permissions.length)
            return;

        backupTargetFolder = null;

        requestPermissions(permissions, REQUEST_CODE_WRITE_EXT_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_WRITE_EXT_STORAGE: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateBackupTargetFolder();
                } else {
                    finish();
                }
                return;
            }
        }
    }

    private void updateBackupTargetFolder() {
        backupTargetFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "jp.naver.line.android");
        backupTargetFolder = new File(backupTargetFolder, "backup");
    }

    private void appendToConsole(String newLine) {
        runOnUiThread(() -> {
            consoleView.append(newLine);
            consoleView.append("\n");
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_RESTORE_FILE && resultCode == RESULT_OK) {
            Uri selectedFile = data.getData(); //The uri with the location of the file
            final String filePath = selectedFile.getPath();

            if (!filePath.endsWith(".tar.gz") && !filePath.endsWith(".tgz")) {
                showMessage("File format error.");
                return;
            }

            Intent intent = new Intent(this, getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.putExtra(RESTORE_INTENT_KEY, true);
            intent.putExtra(RESTORE_FILE_PATH_KEY, filePath);
            startActivity(intent);
        }
    }

    private static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) > 0) {
            os.write(buffer, 0, len);
        }
    }

    private void showMessage(String str) {
        Log.i("BAK", str);
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
}
