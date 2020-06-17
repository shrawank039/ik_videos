package com.ik.videos.ui.Activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.ik.videos.Adapters.CategorySelectAdapter;
import com.ik.videos.Adapters.LanguageSelectAdapter;
import com.ik.videos.Adapters.SelectableCategoryViewHolder;
import com.ik.videos.Adapters.SelectableLanguageViewHolder;
import com.ik.videos.Provider.PrefManager;
import com.ik.videos.R;
import com.ik.videos.VideoCompressor;
import com.ik.videos.api.ProgressRequestBody;
import com.ik.videos.api.apiClient;
import com.ik.videos.api.apiRest;
import com.ik.videos.model.ApiResponse;
import com.ik.videos.model.Category;
import com.ik.videos.model.Language;

import net.vrgsoft.videcrop.VideoCropActivity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
//import rx.Subscriber;
//import rx.android.schedulers.AndroidSchedulers;
//import com.github.tcking.giraffecompressor.GiraffeCompressor;
public class UploadVideoActivity extends AppCompatActivity implements ProgressRequestBody.UploadCallbacks,SelectableCategoryViewHolder.OnItemSelectedListener ,SelectableLanguageViewHolder.OnItemSelectedListener {
    private Spinner spinner_categories_upload;
    private ArrayList<CharSequence> categoriesList = new ArrayList<>();
    private RelativeLayout relative_layout_upload;
    private ArrayAdapter<CharSequence> adapter;



    private RecyclerView recycle_view_selected_language;
    private RecyclerView recycle_view_selected_category;


    private LinearLayoutManager gridLayoutManagerCategorySelect;
    private LinearLayoutManager gridLayoutManagerLanguageSelect;

    private ArrayList<Category> categoriesListObj = new ArrayList<Category>();
    private CategorySelectAdapter categorySelectAdapter;
    private LanguageSelectAdapter languageSelectAdapter;

    private List<Language> languageList = new ArrayList<Language>();

    protected Button selectColoursButton;

    protected String[] colours ;

    protected ArrayList<CharSequence> selectedColours = new ArrayList<CharSequence>();
    private LinearLayoutManager linearLayoutManager_color;
    private RecyclerView recycle_view_colors_fragment;
    private int PICK_IMAGE = 1002;
    private Bitmap bitmap_wallpaper;
    private ProgressDialog register_progress;

    private EditText edit_text_upload_title;
    private static final int CAMERA_REQUEST_IMAGE_1 = 3001;
    private String   videoUrl;
    private ProgressDialog pd;
    private LinearLayout linear_layout_select;
    private FloatingActionButton fab_upload;
    private EditText edit_text_upload_description;
    private LinearLayout linear_layout_categories;
    private LinearLayout linear_layout_langauges;
    private static final int CROP_REQUEST = 200;
    private String outputPath;
    private Button videoRecord;
    private String videofile;
    private InterstitialAd mInterstitialAdDownload;

    public void checkPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(UploadVideoActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED) {


                if (ActivityCompat.shouldShowRequestPermissionRationale(UploadVideoActivity.this,   Manifest.permission.READ_CONTACTS)) {
                    Intent intent_status  =  new Intent(getApplicationContext(), PermissionActivity.class);
                    startActivity(intent_status);
                    overridePendingTransition(R.anim.enter, R.anim.exit);
                    finish();
                } else {
                    Intent intent_status  =  new Intent(getApplicationContext(), PermissionActivity.class);
                    startActivity(intent_status);
                    overridePendingTransition(R.anim.enter, R.anim.exit);
                    finish();
                }
            }

        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
        setContentView(R.layout.activity_upload_video);
        Intent intent = getIntent();

        loadLang();
        initView();
        initAction();
        if (null != intent) { //Null Checking
            if(intent.hasExtra("videofile")) {
                videofile = intent.getStringExtra("videofile");
                videoUrl = videofile;
                File file = new File(videoUrl);
                Log.v("SIZE", file.getName() + "");
                edit_text_upload_title.setText(file.getName().replace(".mp4", "").replace(".MP4", ""));
            }
        }
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.upload_video));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initInterstitialAdPrepare();
    }
    private void requestNewInterstitial() {


        AdRequest adRequest = new AdRequest.Builder()
                .build();

        mInterstitialAdDownload.loadAd(adRequest);
    }

    private void initInterstitialAdPrepare() {
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest2 = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest2);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
               // Toast.makeText( getApplicationContext(), "Ad failed: " + errorCode, Toast.LENGTH_SHORT).show();
            }

        });
        AdLoader adLoader = new AdLoader.Builder(getApplicationContext(), "ca-app-pub-3940256099942544/2247696110")
                .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        // Show the ad.
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // Handle the failure by logging, altering the UI, and so on.
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .build())
                .build();
        mInterstitialAdDownload = new InterstitialAd(this);
        mInterstitialAdDownload.setAdUnitId(getResources().getString(R.string.ad_unit_id_interstitial));

        mInterstitialAdDownload.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {

                requestNewInterstitial();


            }
        });

        requestNewInterstitial();
    }
    private void initAction() {
        linear_layout_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectImage();
            }
        });

        fab_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit_text_upload_title.getText().toString().trim().length()<3){
                    Toasty.error(UploadVideoActivity.this, getResources().getString(R.string.edit_text_upload_title_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (videoUrl==null){
                    Toasty.error(UploadVideoActivity.this, getResources().getString(R.string.video_upload_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                ComverVideo();
            }
        });

    }

    private void SelectImage() {
        if (ContextCompat.checkSelfPermission(UploadVideoActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UploadVideoActivity.this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }else{
            Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            i.setType("video/mp4");
            startActivityForResult(i, PICK_IMAGE);
        }
    }

    private void initView() {
        this.linear_layout_langauges=(LinearLayout) findViewById(R.id.linear_layout_langauges);
        this.linear_layout_categories=(LinearLayout) findViewById(R.id.linear_layout_categories);
        pd = new ProgressDialog(UploadVideoActivity.this);
        pd.setMessage("Uploading video");
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setCancelable(false);
        this.edit_text_upload_description =  (EditText) findViewById(R.id.edit_text_upload_description);
        this.fab_upload =  (FloatingActionButton) findViewById(R.id.fab_upload);
        this.linear_layout_select =  (LinearLayout) findViewById(R.id.linear_layout_select);
        this.edit_text_upload_title=(EditText) findViewById(R.id.edit_text_upload_title);
        this.relative_layout_upload=(RelativeLayout) findViewById(R.id.relative_layout_upload);

        this.videoRecord=(Button)findViewById(R.id.videoRecord);
        this.videoRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(videoUrl!=null) {
                    String inputPath = videoUrl;
                    outputPath = videoUrl.replaceAll(".mp4", "-1.mp4");
                    startActivityForResult(VideoCropActivity.createIntent(getApplicationContext(), inputPath, outputPath), CROP_REQUEST);
            }
                else
                {
                    Toast.makeText(getApplicationContext(), "Please select video", Toast.LENGTH_SHORT).show();
                }
            }
        });

        PrefManager prf= new PrefManager(getApplicationContext());

        this.linearLayoutManager_color = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);



        gridLayoutManagerCategorySelect = new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false);
        gridLayoutManagerLanguageSelect = new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false);

        recycle_view_selected_category= (RecyclerView) findViewById(R.id.recycle_view_selected_category);
        recycle_view_selected_language= (RecyclerView) findViewById(R.id.recycle_view_selected_language);
        getCategory();
    }

    protected void showSelectColoursDialog() {

        boolean[] checkedColours = new boolean[colours.length];
        int count = colours.length;
        for(int i = 0; i < count; i++)
            checkedColours[i] = selectedColours.contains(colours[i]);
        DialogInterface.OnMultiChoiceClickListener coloursDialogListener = new DialogInterface.OnMultiChoiceClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                if(isChecked)
                    selectedColours.add(colours[which]);
                else
                    selectedColours.remove(colours[which]);

                onChangeSelectedColours();

            }

        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Select Colours");
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });




        builder.setMultiChoiceItems(colours, checkedColours, coloursDialogListener);
        AlertDialog dialog = builder.create();
        dialog.show();

    }
    protected void onChangeSelectedColours() {




    }
    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK
                && null != data) {


            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Video.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            videoUrl = picturePath  ;
            if(videoUrl.endsWith(".mp4")) {
                File file = new File(videoUrl);
                Log.v("SIZE", file.getName() + "");
                edit_text_upload_title.setText(file.getName().replace(".mp4", "").replace(".MP4", ""));
                String inputPath = videoUrl;
                outputPath = videoUrl.replaceAll(".mp4", "-1.mp4");
                startActivityForResult(VideoCropActivity.createIntent(getApplicationContext(), inputPath, outputPath), CROP_REQUEST);
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Please Select Only MP4 Video Files",Toast.LENGTH_LONG).show();
            }

            }
        else if(requestCode == CROP_REQUEST && resultCode == RESULT_OK){
            //crop successful
            Toast.makeText(getApplicationContext(),videoUrl+" | Video Edit Done"+outputPath,Toast.LENGTH_LONG).show();
            videoUrl=outputPath;
        }
        else {

            Log.i("SonaSys", "resultCode: " + resultCode);
            switch (resultCode) {
                case 0:
                    Log.i("SonaSys", "User cancelled");
                    break;
                case -1:
                    break;
            }
        }
    }

    public static File getAndroidMoviesFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
    }
    VideoCompressor mVideoCompressor;
    public void ComverVideo()
    {
      mVideoCompressor = new VideoCompressor(getApplicationContext());
        outputPath = videoUrl.replaceAll(".mp4", "-c.mp4");
        pd = new ProgressDialog(this);
        pd.setMessage("Compressing video");
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setCancelable(false);
        pd.show();
        mVideoCompressor.startCompressing(videoUrl, new VideoCompressor.CompressionListener() {
            @Override
            public void compressionFinished(int status, boolean isVideo, String fileOutputPath) {

                if (mVideoCompressor.isDone()) {
                    videoUrl=fileOutputPath;
                    File outputFile = new File(fileOutputPath);
                    long outputCompressVideosize = outputFile.length();
                    long fileSizeInKB = outputCompressVideosize / 1024;
                    long fileSizeInMB = fileSizeInKB / 1024;
                    pd.cancel();
                    pd.dismiss();
                    upload(CAMERA_REQUEST_IMAGE_1);
                }
                else
                {
                    upload(CAMERA_REQUEST_IMAGE_1);
                }

            }

            @Override
            public void onFailure(String message) {
            pd.cancel();
            pd.dismiss();
            upload(CAMERA_REQUEST_IMAGE_1);
            }

            @Override
            public void onProgress(final int progress) {
                pd.setProgress(progress);
               }
        });

        /*
        VideoCompress.compressVideoMedium(videoUrl, outputPath, new VideoCompress.CompressListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onSuccess() {
                pd.cancel();
                pd.dismiss();
                videoUrl=outputPath;
            upload(CAMERA_REQUEST_IMAGE_1);
            }

            @Override
            public void onFail() {
                pd.cancel();
                pd.dismiss();
                Toast.makeText(getApplicationContext(),"Failed",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(float percent) {
                pd.setProgress((int)percent );
            }
        });

        GiraffeCompressor.init(this);
        GiraffeCompressor.create("ffmpeg")
                .input(videoUrl)
                .output(outputPath)
                .bitRate(345600)
                .resizeFactor(1)
               // .watermark("/sdcard/videoCompressor/watermarker.png")//add watermark(take a long time) 水印图片(需要长时间处理)
                .ready()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<GiraffeCompressor.Result>() {
                    @Override
                    public void onCompleted() {
                        pd.cancel();
                        pd.dismiss();
                        videoUrl=outputPath;;
                        upload(CAMERA_REQUEST_IMAGE_1);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        pd.cancel();
                        pd.dismiss();

                    }

                    @Override
                    public void onNext(GiraffeCompressor.Result s) {
                        String msg = String.format("compress completed \ntake time:%s \nout put file:%s", s.getCostTime(), s.getOutput());
                        msg = msg + "\ninput file size:"+ Formatter.formatFileSize(getApplication(),videoUrl.length());
                        msg = msg + "\nout file size:"+ Formatter.formatFileSize(getApplication(),new File(s.getOutput()).length());
                        System.out.println(msg);
                    }
                });

         */

    }
    public void upload(final int CODE){
pd.setMessage("Uploading Video");
        File file1 = new File(videoUrl);
        int file_size = Integer.parseInt(String.valueOf(file1.length()/1024/1024));
        if (file_size>20){
            Toasty.error(getApplicationContext(),"Max file size allowed 20M",Toast.LENGTH_LONG).show();
            return;
        }

        Log.v("SIZE",file1.getName()+"");


        PrefManager prf = new PrefManager(getApplicationContext());

        Retrofit retrofit = apiClient.getClient();
        apiRest service = retrofit.create(apiRest.class);

        //File creating from selected URL
        final File file = new File(videoUrl);
        String  type = "video";

        Bitmap bMap = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
        if (bMap.getHeight()>bMap.getWidth()){
            type = "fullscreen";
        }

        File file_thum = new File(getApplicationContext().getCacheDir(), "thumb.png");
        OutputStream os = null;
        try {

            os = new BufferedOutputStream(new FileOutputStream(file_thum));
            bMap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.close();

        } catch (FileNotFoundException e) {
            Toasty.error(getApplicationContext(),"The selected file not a supported video format",Toast.LENGTH_LONG).show();
            return;
        } catch (NullPointerException e) {
            Toasty.error(getApplicationContext(),"The selected file not a supported video format",Toast.LENGTH_LONG).show();
            return;
        } catch (IOException e) {
            Toasty.error(getApplicationContext(),"The selected file not a supported video format",Toast.LENGTH_LONG).show();
            return;
        }

        pd.show();

        ProgressRequestBody requestFile = new ProgressRequestBody(file, UploadVideoActivity.this);
        ProgressRequestBody requestFile_thum = new ProgressRequestBody(file_thum, UploadVideoActivity.this);

        // create RequestBody instance from file
        // RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body =MultipartBody.Part.createFormData("uploaded_file", file.getName(), requestFile);
        MultipartBody.Part bodythum =MultipartBody.Part.createFormData("uploaded_file_thum", file.getName(), requestFile_thum);
        String id_ser=  prf.getString("ID_USER");
        String key_ser=  prf.getString("TOKEN_USER");

        Call<ApiResponse> request = service.uploadVideo(body,bodythum,type, id_ser, key_ser, edit_text_upload_title.getText().toString().trim(),edit_text_upload_description.getText().toString().trim(),getSelectedLanguages(),getSelectedCategories());

        request.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {

                if (response.isSuccessful()){
                    Toasty.success(getApplication(),getResources().getString(R.string.video_upload_success),Toast.LENGTH_LONG).show();
                    finish();
                }else{
                    Toasty.error(getApplication(),getResources().getString(R.string.no_connexion),Toast.LENGTH_LONG).show();

                }
                // file.delete();
                // getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                pd.dismiss();
                pd.cancel();
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toasty.error(getApplication(),getResources().getString(R.string.no_connexion),Toast.LENGTH_LONG).show();
                pd.dismiss();
                pd.cancel();
            }
        });
    }
    public String getSelectedCategories(){
        String categories = "";
        for (int i = 0; i < categorySelectAdapter.getSelectedItems().size(); i++) {
            categories+="_"+categorySelectAdapter.getSelectedItems().get(i).getId();
        }
        Log.v("categories",categories);

        return categories;
    }
    public String getSelectedLanguages(){
        String colors = "";
        for (int i = 0; i < languageSelectAdapter.getSelectedItems().size(); i++) {
            colors+="_"+languageSelectAdapter.getSelectedItems().get(i).getId();
        }
        Log.v("colors",colors);
        return colors;
    }
    @Override
    public void onProgressUpdate(int percentage) {
        pd.setProgress(percentage);
    }

    @Override
    public void onError() {
        pd.dismiss();
        pd.cancel();
    }

    @Override
    public void onFinish() {
        pd.dismiss();
        pd.cancel();

    }
    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        // overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
        return;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                super.onBackPressed();
                //  overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void loadLang(){
        Retrofit retrofit = apiClient.getClient();
        apiRest service = retrofit.create(apiRest.class);
        Call<List<Language>> call = service.languageAll();
        call.enqueue(new Callback<List<Language>>() {
            @Override
            public void onResponse(Call<List<Language>> call, final Response<List<Language>> response) {

            }
            @Override
            public void onFailure(Call<List<Language>> call, Throwable t) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();
    }


    private void getCategory() {
        register_progress= ProgressDialog.show(this, null,getResources().getString(R.string.operation_progress), true);

        Retrofit retrofit = apiClient.getClient();
        apiRest service = retrofit.create(apiRest.class);
        Call<List<Category>> call = service.categoriesImageAll();
        call.enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if(response.isSuccessful()){
                    categoriesListObj.clear();
                    categoriesListObj.clear();
                    for (int i = 0; i < response.body().size(); i++) {

                        categoriesListObj.add(response.body().get(i));
                    }
                    categorySelectAdapter = new CategorySelectAdapter(UploadVideoActivity.this, categoriesListObj, true, UploadVideoActivity.this);
                    recycle_view_selected_category.setHasFixedSize(true);
                    recycle_view_selected_category.setAdapter(categorySelectAdapter);
                    recycle_view_selected_category.setLayoutManager(gridLayoutManagerCategorySelect);
                    if (response.body().size()>0) {
                        linear_layout_categories.setVisibility(View.VISIBLE);
                    }
                }else {
                    Snackbar snackbar = Snackbar
                            .make(relative_layout_upload, getResources().getString(R.string.no_connexion), Snackbar.LENGTH_INDEFINITE)
                            .setAction(getResources().getString(R.string.retry), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                }
                            });
                    snackbar.setActionTextColor(android.graphics.Color.RED);
                    View sbView = snackbar.getView();
                    TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);
                    textView.setTextColor(android.graphics.Color.YELLOW);
                    snackbar.show();
                }
                getLanguages();
                register_progress.dismiss();

            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                getLanguages();
                register_progress.dismiss();
                Snackbar snackbar = Snackbar
                        .make(relative_layout_upload, getResources().getString(R.string.no_connexion), Snackbar.LENGTH_INDEFINITE)
                        .setAction(getResources().getString(R.string.retry), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getCategory();
                            }
                        });
                snackbar.setActionTextColor(android.graphics.Color.RED);
                View sbView = snackbar.getView();
                TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);
                textView.setTextColor(android.graphics.Color.YELLOW);
                snackbar.show();
            }

        });
    }
    private void getLanguages(){
        Retrofit retrofit = apiClient.getClient();
        apiRest service = retrofit.create(apiRest.class);
        Call<List<Language>> call = service.languageAll();
        call.enqueue(new Callback<List<Language>>() {
            @Override
            public void onResponse(Call<List<Language>> call, Response<List<Language>> response) {
                if(response.isSuccessful()) {
                    List<String> colortitles = new ArrayList<String>();
                    languageList.clear();
                    for (int i = 0; i < response.body().size(); i++) {
                        if (i != 0) {
                            languageList.add(response.body().get(i));
                            colortitles.add(response.body().get(i).getLanguage());
                        }
                    }
                    languageSelectAdapter = new LanguageSelectAdapter(UploadVideoActivity.this, languageList, true, UploadVideoActivity.this);
                    recycle_view_selected_language.setHasFixedSize(true);
                    recycle_view_selected_language.setAdapter(languageSelectAdapter);
                    recycle_view_selected_language.setLayoutManager(gridLayoutManagerLanguageSelect);
                    if (response.body().size()>1) {
                        linear_layout_langauges.setVisibility(View.VISIBLE);
                    }

                    //fab_save_upload.show();
                }
                register_progress.dismiss();
            }
            @Override
            public void onFailure(Call<List<Language>> call, Throwable t) {
                register_progress.dismiss();

            }
        });
    }

    @Override
    public void onItemSelected(Language item) {

    }

    @Override
    public void onItemSelected(Category item) {

    }
}