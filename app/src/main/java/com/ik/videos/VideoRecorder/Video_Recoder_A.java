package com.ik.videos.VideoRecorder;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.ik.videos.R;
import com.ik.videos.RingdroidSelectActivity;
import com.ik.videos.SegmentProgress.ProgressBarListener;
import com.ik.videos.SegmentProgress.SegmentedProgressBar;
import com.ik.videos.config.Global;
import com.wonderkiln.camerakit.CameraKit;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class Video_Recoder_A extends AppCompatActivity implements View.OnClickListener {


    CameraView cameraView;
    int number=0;
    ArrayList<String> videopaths=new ArrayList<>();
    TextView txt60,txt18;
    ImageView record_image;
    ImageView done_btn;
    boolean is_recording=false;
    boolean is_flash_on=false;
    ImageView flash_btn;
    SegmentedProgressBar video_progress;
    LinearLayout camera_options;
    ImageView rotate_camera;
    public static int Sounds_list_Request_code=7;
    TextView add_sound_txt;
    int sec_passed=0;
    File file;
    int recording_status =0;  // 0>record 1>paused >2 resumed
    int selected_time=18;
    String storage="no";


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hide_navigation();
        setContentView(R.layout.activity_video_recoder);

        sec_passed=0;
        Global.Selected_sound_id="null";

        cameraView = findViewById(R.id.camera);
        camera_options=findViewById(R.id.camera_options);
        txt18= findViewById(R.id.txt_15sec);
        txt60 = findViewById(R.id.txt_60sec);

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {
            }

            @Override
            public void onError(CameraKitError cameraKitError) {
            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });


        record_image=findViewById(R.id.record_image);


        findViewById(R.id.upload_layout).setOnClickListener(this);


        done_btn=findViewById(R.id.done);
        done_btn.setEnabled(false);
        done_btn.setOnClickListener(this);


        rotate_camera=findViewById(R.id.rotate_camera);
        rotate_camera.setOnClickListener(this);
        flash_btn=findViewById(R.id.flash_camera);
        flash_btn.setOnClickListener(this);

        findViewById(R.id.Goback).setOnClickListener(this);

        add_sound_txt=findViewById(R.id.add_sound_txt);
        add_sound_txt.setOnClickListener(this);




        // this is code hold to record the video
        final Timer[] timer = {new Timer()};
        record_image.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    timer[0] =new Timer();

                    timer[0].schedule(new TimerTask() {
                        @Override
                        public void run() {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(!is_recording)
                                        Toast.makeText(Video_Recoder_A.this, "start", Toast.LENGTH_SHORT).show();
                                    Start_or_Stop_Recording();
                                }
                            });

                        }
                    }, 200);


                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    timer[0].cancel();
                    if(is_recording){
                        Toast.makeText(Video_Recoder_A.this, "stop", Toast.LENGTH_SHORT).show();
                        Start_or_Stop_Recording();
                    }
                }
                return false;
            }

        });




        video_progress=findViewById(R.id.video_progress);
        video_progress.enableAutoProgressView(selected_time*1000);
        video_progress.setDividerColor(Color.WHITE);
        video_progress.setDividerEnabled(true);
        video_progress.setDividerWidth(4);
        video_progress.setShader(new int[]{Color.CYAN, Color.CYAN, Color.CYAN});

        video_progress.SetListener(new ProgressBarListener() {
            @Override
            public void TimeinMill(long mills) {
                sec_passed = (int) (mills/1000);

                if(sec_passed >= selected_time){
                    Start_or_Stop_Recording();
                }

            }
        });

    }




    // if the Recording is stop then it we start the recording
    // and if the mobile is recording the video then it will stop the recording
    public void Start_or_Stop_Recording(){
        number=number+1;
        file = new File(Global.root + "/" + "myvideo"+(number)+".mp4");

        if (!is_recording && sec_passed<selected_time && recording_status ==0) {
            recording_status =1;

            is_recording=true;

            videopaths.add(Global.root + "/" + "myvideo"+(number)+".mp4");
            cameraView.captureVideo(file);


            if(audio!=null)
                audio.start();

            video_progress.resume();


            done_btn.setBackgroundResource(R.drawable.ic_not_done);
            done_btn.setEnabled(false);
            record_image.setImageDrawable(getResources().getDrawable(R.drawable.ic_recoding_yes));

            camera_options.setVisibility(View.GONE);
            add_sound_txt.setClickable(false);
            rotate_camera.setVisibility(View.GONE);

        }

        else if (is_recording && recording_status ==1 && sec_passed<selected_time) {
            Toast.makeText(this, "pause", Toast.LENGTH_SHORT).show();
            recording_status=2;
            video_progress.pause();
            //  video_progress.addDivider();
            if(audio!=null)
                audio.pause();

            cameraView.stopVideo();
            if(sec_passed>5) {
                done_btn.setBackgroundResource(R.drawable.ic_done);
                done_btn.setEnabled(true);
            }
            record_image.setImageDrawable(getResources().getDrawable(R.drawable.ic_recoding_no));
            camera_options.setVisibility(View.VISIBLE);
        }
        else if (recording_status==2) {
            Toast.makeText(this, "resume", Toast.LENGTH_SHORT).show();
            recording_status =1;
            video_progress.resume();
            if(audio!=null)
                audio.start();
            // File file = new File(Variables.root + "/" + "myvideo"+(number)+".mp4");
            videopaths.add(Global.root + "/" + "myvideo"+(number)+".mp4");
            cameraView.captureVideo(file);

            done_btn.setBackgroundResource(R.drawable.ic_not_done);
            done_btn.setEnabled(false);
            record_image.setImageDrawable(getResources().getDrawable(R.drawable.ic_recoding_yes));

            camera_options.setVisibility(View.GONE);
            add_sound_txt.setClickable(false);
            rotate_camera.setVisibility(View.GONE);

        }
        else if(sec_passed>selected_time-1){
            Toast.makeText(this, "Video only can be a "+selected_time+"s.", Toast.LENGTH_LONG).show();
            video_progress.pause();
            if(audio!=null)
                audio.pause();
            cameraView.stopVideo();
            if(sec_passed>5) {
                done_btn.setBackgroundResource(R.drawable.ic_done);
                done_btn.setEnabled(true);
            }
            record_image.setImageDrawable(getResources().getDrawable(R.drawable.ic_recoding_no));
            camera_options.setVisibility(View.VISIBLE);
        }

    }



    // this will apped all the videos parts in one  fullvideo
    private boolean append() {
        Toast.makeText(this, "Preview", Toast.LENGTH_SHORT).show();
        final ProgressDialog progressDialog=new ProgressDialog(Video_Recoder_A.this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {

                        progressDialog.setMessage("Please wait..");
                        progressDialog.show();
                    }
                });
                ArrayList<String> video_list=new ArrayList<>();
                for (int i=0;i<videopaths.size();i++){

                    File file=new File(videopaths.get(i));
                    if(file.exists()) {

                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(Video_Recoder_A.this, Uri.fromFile(file));
                        String hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
                        boolean isVideo = "yes".equals(hasVideo);

                        if (isVideo && file.length() > 3000) {
                            Log.d("resp", videopaths.get(i));
                            video_list.add(videopaths.get(i));
                        }
                    }
                }



                try {

                    Movie[] inMovies = new Movie[video_list.size()];

                    for (int i=0;i<video_list.size();i++){

                        inMovies[i]= MovieCreator.build(video_list.get(i));
                    }


                    List<Track> videoTracks = new LinkedList<Track>();
                    List<Track> audioTracks = new LinkedList<Track>();
                    for (Movie m : inMovies) {
                        for (Track t : m.getTracks()) {
                            if (t.getHandler().equals("soun")) {
                                audioTracks.add(t);
                            }
                            if (t.getHandler().equals("vide")) {
                                videoTracks.add(t);
                            }
                        }
                    }
                    Movie result = new Movie();
                    if (audioTracks.size() > 0) {
                        result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
                    }
                    if (videoTracks.size() > 0) {
                        result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
                    }

                    Container out = new DefaultMp4Builder().build(result);

                    String outputFilePath=null;
                    if(audio!=null){
                        outputFilePath=Global.outputfile;
                    }else {
                        outputFilePath=Global.outputfile2;
                    }

                    FileOutputStream fos = new FileOutputStream(new File(outputFilePath));
                    out.writeContainer(fos.getChannel());
                    fos.close();

                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressDialog.dismiss();

                            if(audio!=null)
                                Merge_withAudio();
                            else {
                                Go_To_preview_Activity();
                            }

                        }
                    });



                } catch (Exception e) {

                }
            }
        }).start();



        return true;
    }



    // this will add the select audio with the video
    public void Merge_withAudio(){

        String root = Environment.getExternalStorageDirectory().toString();
        String audio_file;
        if (storage.equalsIgnoreCase("no")) {
            audio_file = root + "/" + Global.SelectedAudio;
        }
        else {
            audio_file =Global.SelectedAudio;
        }

        String video = root + "/"+"output.mp4";
        String finaloutput = root + "/"+"output2.mp4";

        Merge_Video_Audio merge_video_audio=new Merge_Video_Audio(Video_Recoder_A.this);
        merge_video_audio.doInBackground(audio_file,video,finaloutput, storage);

    }




    public void RotateCamera(){
        cameraView.toggleFacing();
    }



    @SuppressLint("WrongConstant")
    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.rotate_camera:
                RotateCamera();
                break;

            case R.id.upload_layout:

                break;

            case R.id.done:
                append();
                break;

            case R.id.record_image:
                Start_or_Stop_Recording();
                break;

            case R.id.flash_camera:

                if(is_flash_on){
                    is_flash_on=false;
                    cameraView.setFlash(0);
                    // flash_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_flash_on));

                }else {
                    is_flash_on=true;
                    cameraView.setFlash(CameraKit.Constants.FLASH_TORCH);
                    //  flash_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_flash_off));
                }

                break;

            case R.id.Goback:
                onBackPressed();
                break;

            case R.id.add_sound_txt:
//                Intent intent =new Intent(this, SoundList_Main_A.class);
//                startActivityForResult(intent,Sounds_list_Request_code);
//                overridePendingTransition(R.anim.in_from_bottom, R.anim.out_to_top);
                Intent intent=new Intent(Video_Recoder_A.this, RingdroidSelectActivity.class);
                startActivityForResult(intent, Sounds_list_Request_code);
                break;

        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==Sounds_list_Request_code)
        {
            if(data!=null){
                storage =data.getStringExtra("music_path");
              //  String sound_name = data.getStringExtra("sound_name");
                char char1 = storage.charAt(storage.length() -1);
                char char2 = storage.charAt(storage.length() -2);
                char char3 = storage.charAt(storage.length() -3);
                String f = String.valueOf(char3) + char2 +char1;
              //  Toast.makeText(this, f, Toast.LENGTH_SHORT).show();
                if (f.equalsIgnoreCase("aac")){
                    Global.SelectedAudio = storage;
                    add_sound_txt.setText("Music Added");
                    PreparedStoredAudio();
//                Toast.makeText(this, storage, Toast.LENGTH_SHORT).show();
                } else {
                       convertAudio(storage,data.getStringExtra("sound_name"));
                }

            }

        }
    }

    public void convertAudio(String dir, String sound_name){

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Converting audio file...");
        progressDialog.show();

        File wavFile = new File(dir);
        IConvertCallback callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {

                progressDialog.dismiss();
                storage =convertedFile.getPath();
                // output.putExtra("sound_name", convertedFile.getName());
                if(!storage.equals("")){
                    if(!storage.equalsIgnoreCase("yes")){
                        add_sound_txt.setText(convertedFile.getName());
                        Global.SelectedAudio = storage;
                        PreparedStoredAudio();
                    }
                    else {
                        add_sound_txt.setText(convertedFile.getName());
                        Global.Selected_sound_id = storage;
                        //PreparedAudio();
                        PreparedStoredAudio();
                    }
                }

            }
            @Override
            public void onFailure(Exception error) {
                progressDialog.dismiss();
                new AlertDialog.Builder(getApplicationContext())
                        .setTitle("Too Large File!!!")
                        .setMessage("You can not select a large music file. \nPlease select music files below 60sec.")

//                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int which) {
//                                // Continue with delete operation
//                            }
//                        })

                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton("ok", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                 //  Toast.makeText(getApplicationContext(), "ERROR: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
       //  Toast.makeText(getApplicationContext(), "Converting audio file...", Toast.LENGTH_SHORT).show();
        AndroidAudioConverter.with(this)
                .setFile(wavFile)
                .setFormat(AudioFormat.AAC)
                .setCallback(callback)
                .convert();
    }


    // this will play the sound with the video when we select the audio
    MediaPlayer audio;
    public  void PreparedAudio(){
        File file=new File(Global.root+"/"+ Global.SelectedAudio);
        if(file.exists()) {
            audio = new MediaPlayer();
            try {
                audio.setDataSource(Global.root + "/" + Global.SelectedAudio);
                audio.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public  void PreparedStoredAudio(){
        File file=new File(Global.SelectedAudio);
        if(file.exists()) {
            audio = new MediaPlayer();
            try {
                audio.setDataSource(Global.SelectedAudio);
                audio.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {

            if (audio != null) {
                audio.stop();
                audio.reset();
                audio.release();
            }
            cameraView.stop();

        }catch (Exception e){

        }
    }




    @Override
    public void onBackPressed() {

        new AlertDialog.Builder(this)
                .setTitle("Alert")
                .setMessage("Are you Sure? if you Go back you can't undo this action")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();

                        DeleteFile();
                        finish();
                        //    overridePendingTransition(R.anim.in_from_top, R.anim.out_from_bottom);

                    }
                }).show();

    }



    public void Go_To_preview_Activity(){
        Intent intent =new Intent(this,Preview_Video_A.class);
        startActivity(intent);
        // overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
    }


    // this will delete all the video parts that is create during priviously created video
    int delete_count=0;
    public void DeleteFile(){
        delete_count++;
        File output = new File(Global.outputfile);
        File output2 = new File(Global.outputfile2);
        File output_filter_file = new File(Global.output_filter_file);

        if(output.exists()){
            output.delete();
        }
        if(output2.exists()){

            output2.delete();
        }
        if(output_filter_file.exists()){
            output_filter_file.delete();
        }

        File file = new File(Global.root + "/" + "myvideo"+(delete_count)+".mp4");
        if(file.exists()){
            file.delete();
            DeleteFile();
        }

    }


    // this will hide the bottom mobile navigation controll
    public void Hide_navigation(){

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        // This work only for android 4.4+

        getWindow().getDecorView().setSystemUiVisibility(flags);

        // Code below is to handle presses of Volume up or Volume down.
        // Without this, after pressing volume buttons, the navigation bar will
        // show up and won't hide
        final View decorView = getWindow().getDecorView();
        decorView
                .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
                {

                    @Override
                    public void onSystemUiVisibilityChange(int visibility)
                    {
                        if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                        {
                            decorView.setSystemUiVisibility(flags);
                        }
                    }
                });

    }


    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus)
        {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }


    public void etnClick(View view) {
        selected_time = 18;
        txt60.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        txt18.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.yellow));
    }

    public void sxtysClick(View view) {
        selected_time = 60;
        video_progress.enableAutoProgressView(selected_time*1000);
        txt60.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.yellow));
        txt18.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
    }

    public void close(View view) {
        onBackPressed();
    }
}

