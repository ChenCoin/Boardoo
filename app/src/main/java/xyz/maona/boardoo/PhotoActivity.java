package xyz.maona.boardoo;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PhotoActivity extends Activity {

    private String FILE_NAME = "cache.png";
    private PhotoView photoView;
    private View index;
    View background;
    private View menu;
    private View loading;
    private boolean menuShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(R.layout.activity_photo);
        photoView = findViewById(R.id.photoView);
        index = findViewById(R.id.index);
        background = findViewById(R.id.background);
        menu = findViewById(R.id.menu);
        loading = findViewById(R.id.loading);

        findViewById(R.id.cancel).setOnClickListener(view -> hideMenu());
        findViewById(R.id.delete).setOnClickListener(view -> {
            File file = new File(getCacheDir(), FILE_NAME);
            if (!file.exists() || !file.delete()) {
                Toast.makeText(this, getResources().getString(R.string.error),
                        Toast.LENGTH_SHORT).show();
            }
            hideMenu();
            visibility(index);
        });
        photoView.setOnLongClickListener(this::showMenu);
        photoView.setZoomable(true);
        background.setOnClickListener(view -> {
            if (menuShowing) hideMenu();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        visibility(loading);
        updatePhoto(this::loadLocalPhoto);
    }

    @Override
    public void onBackPressed() {
        if (menuShowing) hideMenu();
        else super.onBackPressed();
    }

    private void visibility(View show) {
        photoView.setVisibility(View.INVISIBLE);
        index.setVisibility(View.INVISIBLE);
        loading.setVisibility(View.INVISIBLE);
        show.setVisibility(View.VISIBLE);
    }

    private void loadPhoto(File file) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
            photoView.setImageBitmap(bitmap);
            visibility(photoView);
        } catch (FileNotFoundException ignored) {
            visibility(index);
            Toast.makeText(this, getResources().getString(R.string.error),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLocalPhoto() {
        File file = new File(getCacheDir(), FILE_NAME);
        if (!file.exists()) {
            visibility(index);
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

    private boolean showMenu(View view) {
        menuShowing = true;
        menu.setVisibility(View.VISIBLE);
        background.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(background, "alpha", 0, 0.6F)
                .setDuration(300).start();
        ObjectAnimator animator = ObjectAnimator.ofFloat(menu,
                "translationY", menu.getHeight(), 0);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(300);
        animator.start();
        return true;
    }

    private void hideMenu() {
        menuShowing = false;
        ObjectAnimator.ofFloat(background, "alpha", 0.6F, 0)
                .setDuration(300).start();
        ObjectAnimator animator = ObjectAnimator.ofFloat(menu,
                "translationY", 0, menu.getHeight());
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
        new Handler().postDelayed(() -> {
            menu.setVisibility(View.INVISIBLE);
            background.setVisibility(View.INVISIBLE);
        }, 300);
    }

}
