package com.ik.videos.config;

import android.os.Environment;

/**
 * Created by Tamim on 28/09/2017.
 */



public class Global {
    public static final String API_URL = "http://admin.ikvideos.xyz/api/";
    public static final String SECURE_KEY = "4F5A9C3D9A86FA54EACEDDD635185";



    public static final String ITEM_PURCHASE_CODE = "16edd7cf-2525-485e-b11a-3dd35f382457";

    public static final String SUBSCRIPTION_ID = "SUBSCRIPTION_ID";
    public static final String MERCHANT_KEY = "MERCHANT_KEY" ; // PUT YOUR MERCHANT KEY HERE;
    public static final long   SUBSCRIPTION_DURATION = 30; // PUT SUBSCRIPTION DURATION DAYS HERE;

    public static String root= Environment.getExternalStorageDirectory().toString();
    public static String outputfile2=root + "/output2.mp4";
    public static String Selected_sound_id = "null";
    public static String outputfile=root + "/output.mp4";
    public static String SelectedAudio = "SelectedAudio.aac";
    public static String output_filter_file=root + "/output-filtered.mp4";
}