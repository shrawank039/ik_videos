package com.ik.videos.ui.Activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLException;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daasuu.camerarecorder.CameraRecordListener;
import com.daasuu.camerarecorder.CameraRecorder;
import com.daasuu.camerarecorder.CameraRecorderBuilder;
import com.daasuu.camerarecorder.LensFacing;
import com.ik.videos.R;
import com.ik.videos.RingdroidSelectActivity;
import com.ik.videos.services.Mp4ParseUtil;
import com.ik.videos.widget.Filters;
import com.ik.videos.widget.SampleGLView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.microedition.khronos.opengles.GL10;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;
/*
import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;
*/

public class BaseCameraActivity extends AppCompatActivity {

    private SampleGLView sampleGLView;
    protected CameraRecorder cameraRecorder;
    private String filepath;
    private TextView recordBtn;
    protected LensFacing lensFacing = LensFacing.BACK;
    protected int cameraWidth = 640;
    protected int cameraHeight = 360;
    protected int videoWidth = 360;
    protected int videoHeight = 360;
    private AlertDialog filterDialog;
    private boolean toggleClick = false;
    private Button recordBtnAudio;
     Uri musicURI=null;
    private MediaPlayer mediaPlayer;
 String outVideoPath;
    private boolean mute=false;
    private String musicPath="";
    private boolean audioSelect=false;
  //  private IConvertCallback callback;
    private ProgressDialog pd;
    private IConvertCallback callback;

    protected void onCreateActivity(Context cc) {
        getSupportActionBar().hide();
        pd = new ProgressDialog(cc);
        pd.setMessage("Preparing video");
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setCancelable(false);

         callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {
                System.out.println("Audio:"+convertedFile.getPath().toString());
                outVideoPath=getVideoFilePath("out");
                musicPath=convertedFile.getPath().toString();
                boolean ouputs = Mp4ParseUtil.muxAacMp4(musicPath, filepath.toString(), outVideoPath.toString());
                if(ouputs)
                {
                    Intent intent = new Intent(getApplicationContext(), UploadVideoActivity.class);
                    intent.putExtra("videofile", outVideoPath);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(intent);
                }
                pd.cancel();
                pd.dismiss();
            }
            @Override
            public void onFailure(Exception error) {
            Toast.makeText(getApplicationContext(),"Audios"+error.getMessage(),Toast.LENGTH_SHORT).show();
            System.out.println(error.getCause());
                pd.cancel();
                pd.dismiss();
            }

        };

        recordBtn = findViewById(R.id.btn_record);
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (recordBtn.getTag().equals(getString(R.string.app_record))) {
                    filepath = getVideoFilePath("og");
                    cameraRecorder.start(filepath);
                    recordBtn.setTag(R.string.app_record);
                    recordBtn.setBackgroundResource(R.drawable.recordstop);

                    if (musicURI != null) {
                        mute = true;
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.reset();
                        try {
                            mediaPlayer.setDataSource(getApplicationContext(), musicURI);
                            mediaPlayer.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            public void onCompletion(MediaPlayer mp) {
                                Toast.makeText(getApplicationContext(), "Media Completed", Toast.LENGTH_SHORT).show();
                                cameraRecorder.stop();
                                recordBtn.setTag(getString(R.string.app_record));
                                recordBtn.setBackgroundResource(R.drawable.recordstart);
                            }
                        });
                    }
                } else {
                    cameraRecorder.stop();
                    recordBtn.setTag(getString(R.string.app_record));
                    recordBtn.setBackgroundResource(R.drawable.recordstart);
                }
            }

        });

        recordBtnAudio = findViewById(R.id.btnAudio);
        recordBtnAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent videoIntent = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(videoIntent, "Select Audio"), 11);
            }
        });


        findViewById(R.id.btn_flash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraRecorder != null && cameraRecorder.isFlashSupport()) {
                    cameraRecorder.switchFlashMode();
                    cameraRecorder.changeAutoFocus();
                }
            }
        });

        findViewById(R.id.btn_switch_camera).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view){
                    releaseCamera();
                    if (lensFacing == LensFacing.BACK) {
                        lensFacing = LensFacing.FRONT;
                    } else {
                        lensFacing = LensFacing.BACK;
                    }
                    toggleClick = true;
                }
        });

        findViewById(R.id.btn_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (filterDialog == null) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setTitle("Choose a filter");
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            filterDialog = null;

                        }
                    });

                    final Filters[] filters = Filters.values();
                    CharSequence[] charList = new CharSequence[filters.length];
                    for (int i = 0, n = filters.length; i < n; i++) {
                        charList[i] = filters[i].name();
                    }
                    builder.setItems(charList, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            changeFilter(filters[item]);
                        }
                    });
                    filterDialog = builder.show();
                } else {
                    filterDialog.dismiss();
                }
            }
        });
/*
        findViewById(R.id.btn_image_capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            captureBitmap(bitmap -> {
                new Handler().post(() -> {
                    String imagePath = getImageFilePath();
                    saveAsPngImage(bitmap, imagePath);

                    exportPngToGallery(getApplicationContext(), imagePath);
                });
            });
        });

*/
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    }
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        if (requestCode == 11 && null != data) {
            if (requestCode == 11) {

                musicURI = data.getData();
                try {
                    musicPath = getAudioPath(musicURI);
                    Toast.makeText(getApplicationContext(),musicPath,Toast.LENGTH_LONG).show();
                    File f = new File(musicPath);
                    long fileSizeInBytes = f.length();
                    long fileSizeInKB = fileSizeInBytes / 1024;
                    long fileSizeInMB = fileSizeInKB / 1024;
                    mute=true;
                    audioSelect = true;if (fileSizeInMB > 8) {
                        Toast.makeText(getApplicationContext(),"File too Large",Toast.LENGTH_LONG).show();
                    }
                    setUpCamera();
                } catch (Exception e) {
                    //handle exception
                    Toast.makeText(getApplicationContext(), "Unable to process,try again", Toast.LENGTH_SHORT).show();
                }
                //   String path1 = uri.getPath();

            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    private String getAudioPath(Uri uri) {
        String[] data = {MediaStore.Audio.Media.DATA};
        CursorLoader loader = new CursorLoader(getApplicationContext(), uri, data, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
    @Override
    protected void onResume() {
        super.onResume();
        setUpCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
    }

    private void releaseCamera() {
        if (sampleGLView != null) {
            sampleGLView.onPause();
        }

        if (cameraRecorder != null) {
            cameraRecorder.stop();
            cameraRecorder.release();
            cameraRecorder = null;
        }

        if (sampleGLView != null) {
            ((FrameLayout) findViewById(R.id.wrap_view)).removeView(sampleGLView);
            sampleGLView = null;
        }
    }


    private void setUpCameraView() {
        runOnUiThread(new Runnable(){
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void run() {
                FrameLayout frameLayout = findViewById(R.id.wrap_view);
                frameLayout.removeAllViews();
                sampleGLView = null;
                sampleGLView = new SampleGLView(getApplicationContext());
                sampleGLView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        int width = v.getLayoutParams().width;
                        int height = v.getLayoutParams().height;
                        if (cameraRecorder != null)
                        cameraRecorder.changeManualFocusPoint(event.getX(), event.getY(), width, height);


                        return false;
                    }
                });
                frameLayout.addView(sampleGLView);
            }
        });
    }


    private void setUpCamera() {
        setUpCameraView();

        cameraRecorder = new CameraRecorderBuilder(this, sampleGLView)
                //.recordNoFilter(true)
                .cameraRecordListener(new CameraRecordListener() {
                    @Override
                    public void onGetFlashSupport(final boolean flashSupport) {
                        runOnUiThread(new Runnable(){
                            @SuppressLint("ClickableViewAccessibility")
                            @Override
                            public void run() {
                                findViewById(R.id.btn_flash).setEnabled(flashSupport);
                            }
                        });
                    }

                    @Override
                    public void onRecordComplete() {
                        pd.show();
                        exportMp4ToGallery(getApplicationContext(), filepath);

                    }

                    @Override
                    public void onRecordStart() {

                    }

                    @Override
                    public void onError(Exception exception) {
                        Log.e("CameraRecorder", exception.toString());
                    }

                    @Override
                    public void onCameraThreadFinish() {
                        if (toggleClick) {
                            runOnUiThread(new Runnable(){
                                                          @Override
                                                          public void run() {
                                                              setUpCamera();
                                                          }
                            });
                        }
                        toggleClick = false;
                    }
                })
                .videoSize(videoWidth, videoHeight)
                .cameraSize(cameraWidth, cameraHeight)
                .mute(mute)
                .lensFacing(lensFacing)
                .build();


    }
public void mergeAudio()
{

}
    private void changeFilter(Filters filters) {
        cameraRecorder.setFilter(Filters.getFilterInstance(filters, getApplicationContext()));
    }


    private interface BitmapReadyCallbacks {
        void onBitmapReady(Bitmap bitmap);
    }

    private void captureBitmap(final BitmapReadyCallbacks bitmapReadyCallbacks) {
        /*
        sampleGLView.queueEvent(() -> {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            GL10 gl = (GL10) egl.eglGetCurrentContext().getGL();
            Bitmap snapshotBitmap = createBitmapFromGLSurface(sampleGLView.getMeasuredWidth(), sampleGLView.getMeasuredHeight(), gl);

            runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    bitmapReadyCallbacks.onBitmapReady(snapshotBitmap);
                }
            });
        });

         */
    }

    private Bitmap createBitmapFromGLSurface(int w, int h, GL10 gl) {

        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            gl.glReadPixels(0, 0, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2, texturePixel, blue, red, pixel;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    texturePixel = bitmapBuffer[offset1 + j];
                    blue = (texturePixel >> 16) & 0xff;
                    red = (texturePixel << 16) & 0x00ff0000;
                    pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            Log.e("CreateBitmap", "createBitmapFromGLSurface: " + e.getMessage(), e);
            return null;
        }

        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }

    public void saveAsPngImage(Bitmap bitmap, String filePath) {
        try {
            File file = new File(filePath);
            FileOutputStream outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public  void exportMp4ToGallery(Context context, String filePath) {
        final ContentValues values = new ContentValues(2);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, filePath);
        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + filePath)));
        File f = new File(filePath);
        long fileSizeInBytes = f.length();
        long fileSizeInKB = fileSizeInBytes / 1024;
        long fileSizeInMB = fileSizeInKB / 1024;
       if (fileSizeInMB < 15) {
           if (musicURI != null) {
               try {
                   File fileAudio = new File(musicPath);

                   AndroidAudioConverter.with(context)
                           .setFile(fileAudio)
                           .setFormat(AudioFormat.AAC)
                           .setCallback(callback)
                           .convert();

               } catch (Exception e) {
                   e.printStackTrace();
                   Toast.makeText(context, "Audio"+e.getMessage(), Toast.LENGTH_LONG).show();
               }
           } else {
               pd.cancel();
               pd.dismiss();
               Intent intent = new Intent(context, UploadVideoActivity.class);
               intent.putExtra("videofile", filePath);
               intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               context.startActivity(intent);
           }
       }
       else
       {
           Toast.makeText(context,"Sorry We can not record more than 15mb video, Try again", Toast.LENGTH_LONG).show();
       }


    }

    public static String getVideoFilePath(String outs) {
        return getAndroidMoviesFolder().getAbsolutePath() + "/IKRecord-"+outs+ new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()) + ".mp4";
    }

    public static File getAndroidMoviesFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
    }

    private static void exportPngToGallery(Context context, String filePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(filePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    public static String getImageFilePath() {
        return getAndroidImageFolder().getAbsolutePath() + "/" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()) + "cameraRecorder.png";
    }

    public static File getAndroidImageFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    }

}
