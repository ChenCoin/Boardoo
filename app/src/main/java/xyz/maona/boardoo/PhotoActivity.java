package xyz.maona.boardoo;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.bm.library.PhotoView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PhotoActivity extends Activity {

    private String FILE_NAME = "cache.png";
    private PhotoView photoView;
    private TextView tip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_photo);
        photoView = findViewById(R.id.photoView);
        tip = findViewById(R.id.tip);
        photoView.enable();

        View menuContainer = findViewById(R.id.menu);
        findViewById(R.id.cancel).setOnClickListener(view -> {
            menuContainer.setVisibility(View.GONE);
        });
//        photoView.setOnLongClickListener(view -> {
//            menuContainer.setVisibility(View.VISIBLE);
//            return true;
//        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        tip.setText(R.string.loading);
        updatePhoto(this::loadLocalPhoto);
    }

    private void loadPhoto(File file) {
        photoView.setVisibility(View.VISIBLE);
        tip.setVisibility(View.INVISIBLE);
        Glide.with(this).load(file).skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE).into(photoView);
    }

    private void loadLocalPhoto() {
        File file = new File(getCacheDir(), FILE_NAME);
        if (!file.exists()) {
            tip.setText(R.string.tip);
            photoView.setVisibility(View.INVISIBLE);
            tip.setVisibility(View.VISIBLE);
        } else loadPhoto(file);
    }

    private void updatePhoto(Runnable also) {
        boolean goNext = true;
        Intent intent = getIntent();
        if (intent != null) {
            ClipData clipData = intent.getClipData();
            if (clipData != null && clipData.getItemCount() > 0) {
                Uri uri = clipData.getItemAt(0).getUri();
                if (uri != null) {
                    goNext = false;
                    new Thread(() -> {
                        InputStream in = null;
                        FileOutputStream out = null;
                        try {
                            in = getContentResolver().openInputStream(uri);
                            File file = new File(getCacheDir(), FILE_NAME);
                            if (!file.exists() || file.delete()) {
                                out = new FileOutputStream(file);
                                byte[] buffer = new byte[1024];
                                if (in != null) {
                                    while (in.read(buffer) != -1) out.write(buffer);
                                    in.close();
                                }
                                out.close();
                                runOnUiThread(() -> loadPhoto(file));
                            }
                        } catch (Exception ignored) {
                            also.run();
                        } finally {
                            try {
                                if (in != null) in.close();
                                if (out != null) out.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }).start();
                }
            }
        }
        if (goNext) also.run();
    }
}
