package com.kasoft.pushnot;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.kasoft.pushnot.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity{
	
	
	public static final String EXTRA_MESSAGE = "message";
	    public static final String PROPERTY_REG_ID = "registration_id";
	    private static final String PROPERTY_APP_VERSION = "appVersion";
	    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	    /**
	     * Substitute you own sender ID here. This is the project number you got
	     * from the API Console, as described in "Getting Started."
	     */
	    String SENDER_ID = "404189604621";

	    /**
	     * Tag used on log messages.
	     */
	    static final String TAG = "GCM Demo";

	    GoogleCloudMessaging gcm;
	    AtomicInteger msgId = new AtomicInteger();
	    Context context;
	    boolean checked;
	    String regid;	    
	    
	    @Override
		protected void onNewIntent(Intent intent) {
			// TODO Auto-generated method stub
			super.onNewIntent(intent);
			String title = intent.getStringExtra("title");
	        String form = intent.getStringExtra("form");
	        TextView displayWidget = (TextView) findViewById(R.id.display);
	    	if(title != null && form != null){
	        	TextView titleWidget = (TextView) findViewById(R.id.title);
	        	TextView formWidget = (TextView) findViewById(R.id.form);
	        	
	        	titleWidget.setText(title);
	        	formWidget.setText(form);
	        	displayWidget.setText("Zadnja poruka");
	        }
		}

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        setContentView(R.layout.main);
	        TextView display = (TextView) findViewById(R.id.display);
	        context = getApplicationContext();

	        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
	        if (checkPlayServices()) {
	            gcm = GoogleCloudMessaging.getInstance(this);
	            regid = getRegistrationId(context);            
		        
		        display.setText("Aplikacija konaktira server...");
	            if (regid.isEmpty()) {
	                registerInBackground();
	                display.setText("Registriranje aplikacije u toku...");
	            }
	            else{
	            	boolean isSent = isSent();
		            if(!isSent){
		            	new AjaxRequest(regid,"4.0").execute(new String[]{});
		            	display.setText("Aplikacija salje informacije serveru");
		            }
		            else{
		            	if(!setResponseMessage()){
		            		display.setText("Aplikacija ceka na nove poruke..");
		            	}
		            }
	            }
	        } else {
	        	display.setText("Server nije moguce konaktirati");
	            Log.i(TAG, "No valid Google Play Services APK found.");
	        }
	    }
	    
	    public boolean setResponseMessage(){
	    	Intent thisIntent = getIntent();
	        String title = thisIntent.getStringExtra("title");
	        String form = thisIntent.getStringExtra("form");
	        TextView displayWidget = (TextView) findViewById(R.id.display);
	    	if(title != null && form != null){
	        	TextView titleWidget = (TextView) findViewById(R.id.title);
	        	TextView formWidget = (TextView) findViewById(R.id.form);
	        	
	        	titleWidget.setText(title);
	        	formWidget.setText(form);
	        	displayWidget.setText("Zadnja poruka");
	        	return true;
	        }
	    	return false;
	    }

	    @Override
	    protected void onResume() {
	        super.onResume();
	        // Check device for Play Services APK.
	        setResponseMessage();
	        //setResponseMessage();
	    }
	    
	    

	    /**
	     * Check the device to make sure it has the Google Play Services APK. If
	     * it doesn't, display a dialog that allows users to download the APK from
	     * the Google Play Store or enable it in the device's system settings.
	     */
	    private boolean checkPlayServices() {
	        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	        if (resultCode != ConnectionResult.SUCCESS) {
	            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
	                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
	            } else {
	                Log.i(TAG, "This device is not supported.");
	                finish();
	            }
	            return false;
	        }
	        return true;
	    }

	    /**
	     * Stores the registration ID and the app versionCode in the application's
	     * {@code SharedPreferences}.
	     *
	     * @param context application's context.
	     * @param regId registration ID
	     */
	    private void storeRegistrationId(Context context, String regId) {
	        final SharedPreferences prefs = getGcmPreferences(context);
	        int appVersion = getAppVersion(context);
	        Log.i(TAG, "Saving regId on app version " + appVersion);
	        SharedPreferences.Editor editor = prefs.edit();
	        editor.putString(PROPERTY_REG_ID, regId);
	        editor.putInt(PROPERTY_APP_VERSION, appVersion);
	        editor.commit();
	    }
	    private void storeKey(String key,String value) {
	        final SharedPreferences prefs = getGcmPreferences(context);
	        SharedPreferences.Editor editor = prefs.edit();
	        editor.putString(key, value);
	        editor.commit();
	    }
	    private boolean isSent() {
	    	boolean sent = false;
	        final SharedPreferences prefs = getGcmPreferences(context);
	        sent = prefs.getBoolean("keysent", false);
	        return sent;	        
	    }
	    
	    private void storeSent(boolean value) {
	    	final SharedPreferences prefs = getGcmPreferences(context);
	        SharedPreferences.Editor editor = prefs.edit();
	        editor.putBoolean("keysent", value);
	        editor.commit();	        
	    }


	    /**
	     * Gets the current registration ID for application on GCM service, if there is one.
	     * <p>
	     * If result is empty, the app needs to register.
	     *
	     * @return registration ID, or empty string if there is no existing
	     *         registration ID.
	     */
	    private String getRegistrationId(Context context) {
	        final SharedPreferences prefs = getGcmPreferences(context);
	        String registrationId = prefs.getString(PROPERTY_REG_ID, "");

	        if (registrationId.isEmpty()) {
	            Log.i(TAG, "Registration not found.");
	            return "";
	            
	        }
	        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	        int currentVersion = getAppVersion(context);
	        if (registeredVersion != currentVersion) {
	            Log.i(TAG, "App version changed.");
	            return "";
	        }
	        return registrationId;
	    }

	    /**
	     * Registers the application with GCM servers asynchronously.
	     * <p>
	     * Stores the registration ID and the app versionCode in the application's
	     * shared preferences.
	     */

	    private void registerInBackground() {
	        new AsyncTask<Void, Void, String>() {
	            @Override
	            protected String doInBackground(Void... params) {
	                String msg = "";
	                try {
	                    if (gcm == null) {
	                        gcm = GoogleCloudMessaging.getInstance(context);
	                    }
	                    regid = gcm.register(SENDER_ID);
	                    msg = "Device registered, registration ID=" + regid;

	                    // You should send the registration ID to your server over HTTP, so it
	                    // can use GCM/HTTP or CCS to send messages to your app.
	                    sendRegistrationIdToBackend();

	                    // For this demo: we don't need to send it because the device will send
	                    // upstream messages to a server that echo back the message using the
	                    // 'from' address in the message.

	                    // Persist the regID - no need to register again.
	                    storeRegistrationId(context, regid);
	                } catch (IOException ex) {
	                    msg = "Error :" + ex.getMessage();
	                    // If there is an error, don't just keep trying to register.
	                    // Require the user to click a button again, or perform
	                    // exponential back-off.
	                }
	                return msg;
	            }

	            @Override
	            protected void onPostExecute(String msg) {
	                //mDisplay.append(msg + "\n");
	            	TextView display = (TextView) findViewById(R.id.display);
	            	display.setText("Registracija uspjesna");
	            }
	        }.execute(null, null, null);
	    }

	    // Send an upstream message.
	    public void onClick(final View view) {

	        /*if (view == findViewById(R.id.send)) {
	            new AsyncTask<Void, Void, String>() {
	                @Override
	                protected String doInBackground(Void... params) {
	                    String msg = "";
	                    try {
	                        Bundle data = new Bundle();
	                        data.putString("my_message", "Hello World");
	                        data.putString("my_action", "com.google.android.gcm.demo.app.ECHO_NOW");
	                        String id = Integer.toString(msgId.incrementAndGet());
	                        gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
	                        msg = "Sent message";
	                    } catch (IOException ex) {
	                        msg = "Error :" + ex.getMessage();
	                    }
	                    return msg;
	                }

	                @Override
	                protected void onPostExecute(String msg) {
	                    mDisplay.append(msg + "\n");
	                }
	            }.execute(null, null, null);
	        } else if (view == findViewById(R.id.clear)) {
	            mDisplay.setText("");
	        }*/
	    }

	    @Override
	    protected void onDestroy() {
	        super.onDestroy();
	    }

	    /**
	     * @return Application's version code from the {@code PackageManager}.
	     */
	    private static int getAppVersion(Context context) {
	        try {
	            PackageInfo packageInfo = context.getPackageManager()
	                    .getPackageInfo(context.getPackageName(), 0);
	            return packageInfo.versionCode;
	        } catch (NameNotFoundException e) {
	            // should never happen
	            throw new RuntimeException("Could not get package name: " + e);
	        }
	    }

	    /**
	     * @return Application's {@code SharedPreferences}.
	     */
	    private SharedPreferences getGcmPreferences(Context context) {
	        // This sample app persists the registration ID in shared preferences, but
	        // how you store the regID in your app is up to you.
	        return getSharedPreferences(MainActivity.class.getSimpleName(),
	                Context.MODE_PRIVATE);
	    }
	    /**
	     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
	     * messages to your app. Not needed for this demo since the device sends upstream messages
	     * to a server that echoes back the message using the 'from' address in the message.
	     */
	    private void sendRegistrationIdToBackend() {
	      new AjaxRequest(regid,"4.0").execute(new String[]{});
	    }
	    class AjaxRequest extends AsyncTask<String,Void,String>{
	    	@Override
			protected void onPostExecute(String result) {
				// TODO Auto-generated method stub
				super.onPostExecute(result);
				TextView display = (TextView) findViewById(R.id.display);
            	display.setText("Aplikacija ceka na nove poruke");
            	storeSent(true);
			}

			private final String id;
	    	private final String androidV;
	    	public AjaxRequest(String id,String androidV){
	    		this.id = id;
	    		this.androidV = androidV;
	    	}
	    	
			@Override
			protected String doInBackground(String... arg0) {
                    String msg = "";
                    HttpResponse httpResponse = null;
                    DefaultHttpClient httpClient = new DefaultHttpClient();
                    String json = "{\"id\":\""+id+"\",\"android_version\":\""+androidV+"\"}";
                    HttpPost post = new HttpPost("http://kasoftpushnotifications.appspot.com/newphone");
                    try {
                        post.setHeader("Content-type", "application/json");
                        post.setHeader("Accept", "application/json");
                        StringEntity input = new StringEntity(json);
                        input.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json"));
                        post.setEntity(input);
                        try {
							httpResponse = httpClient.execute(post);
						} catch (ClientProtocolException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                        String response = "";
						try {
							response = EntityUtils.toString(httpResponse.getEntity());
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Autochecked-generated catch block
							e.printStackTrace();
						}
                        Log.i("json response",response);
                        msg = response;
                    } catch (UnsupportedEncodingException e1) {
                        e1.printStackTrace();
                        System.out.println("wtf cant encoding error >>>>>>>>>>>>>>>>>>>");
                    }
                    return null;
			}
	    }
}
