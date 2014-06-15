package com.example.youtubeuploadv3;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTubeScopes;

//import android.provider.MediaStore.Video;

public class SubmitActivity extends Activity {

	/**
	 * Logging level for HTTP requests/responses.
	 * 
	 * <p>
	 * To turn on, set to {@link Level#CONFIG} or {@link Level#ALL} and run this
	 * from command line:
	 * </p>
	 * 
	 * <pre>
	 * 	adb shell setprop log.tag.HttpTransport DEBUG
	 * </pre>
	 */

	private static final Level LOGGING_LEVEL = Level.OFF;

	private static final String PREF_ACCOUNT_NAME = "accountName";

	static final String TAG = "YoutubeSampleActivity";

	static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;

	static final int REQUEST_AUTHORIZATION = 1;

	static final int REQUEST_ACCOUNT_PICKER = 2;

	final HttpTransport transport = AndroidHttp.newCompatibleTransport();

	final JsonFactory jsonFactory = new GsonFactory();

	GoogleAccountCredential credential;

	/** Global instance of Youtube object to make all API requests. */
	com.google.api.services.youtube.YouTube youtube;

	int numAsyncTasks;

	/*
	 * Global instance of the format used for the video being uploaded (MIME
	 * type).
	 */
	private static String VIDEO_FILE_FORMAT = "video/*";

	// CalendarModel model = new CalendarModel();
	// ArrayAdapter<CalendarInfo> adapter;
	// com.google.api.services.calendar.Calendar client;

	Uri videoUri = null;
	TextView textViewStatus;
	ProgressDialog dialog = null;

	private Date dateTaken = null;
	private Long uploadFileSize = null;

	WebView web;
	ImageView thumbnail;
	SharedPreferences pref;

	// youtube 上傳用
	private static String CLIENT_ID = "Use your own client id";
	// Use your own client id
	private static String CLIENT_SECRET = "Use your own client secret";
	// Use your own client secret
	private static String REDIRECT_URI = "Use your own REDIRECT_URI";
	
	private static String GRANT_TYPE = "authorization_code";
	private static String TOKEN_URL = "https://accounts.google.com/o/oauth2/token";
	private static String OAUTH_URL = "https://accounts.google.com/o/oauth2/auth";
	private static String OAUTH_SCOPE = "https://www.googleapis.com/auth/youtube.upload";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// enable logging
		Logger.getLogger("com.google.api.client").setLevel(LOGGING_LEVEL);
		// view and menu
		setContentView(R.layout.activity_submit);

		// Google Accounts
		credential = GoogleAccountCredential.usingOAuth2(this,
				YouTubeScopes.YOUTUBE_UPLOAD);
		// save simple data
		SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
		credential.setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME,
				null));
		// youtube client
		youtube = new com.google.api.services.youtube.YouTube.Builder(
				transport, jsonFactory, credential).setApplicationName(
				"Google-YoutubeUploadAndroidSample/1.0").build();

		Intent intent = this.getIntent();
		this.videoUri = intent.getData();
		Log.d(TAG, intent.getData().toString());

		Cursor cursor = this
				.managedQuery(this.videoUri, null, null, null, null);

		if (cursor.getCount() == 0) {
			Log.d("cursor==", "not a valid video uri");
			Toast.makeText(SubmitActivity.this, "not a valid video uri",
					Toast.LENGTH_LONG).show();
		} else {

			if (cursor.moveToFirst()) {

				long id = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Video.VideoColumns._ID));
				this.dateTaken = new Date(
						cursor.getLong(cursor
								.getColumnIndex(MediaStore.Video.VideoColumns.DATE_TAKEN)));
				SimpleDateFormat dateFormat = new SimpleDateFormat(
						"EEE, MMM d, yyyy hh:mm aaa");
				Configuration userConfig = new Configuration();
				Settings.System.getConfiguration(getContentResolver(),
						userConfig);
				Calendar cal = Calendar.getInstance(userConfig.locale);
				TimeZone tz = cal.getTimeZone();

				dateFormat.setTimeZone(tz);

				this.uploadFileSize = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Video.VideoColumns.SIZE));

				TextView dateTakenView = (TextView) findViewById(R.id.dateCaptured);
				dateTakenView.setText(" - Date captured: "
						+ dateFormat.format(dateTaken) + "\n - FileSize: "
						+ uploadFileSize / 1024 + " KB ");

				ImageView thumbnail = (ImageView) findViewById(R.id.thumbnail);
				ContentResolver crThumb = getContentResolver();
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 2;
				Bitmap curThumb = MediaStore.Video.Thumbnails.getThumbnail(
						crThumb, id, MediaStore.Video.Thumbnails.MINI_KIND,
						options);
				thumbnail.setImageBitmap(curThumb);

				textViewStatus = (TextView) findViewById(R.id.textViewStatus);
			}
		}

		// startUpload(SubmitActivity.this.videoUri);
		findViewById(R.id.uploadButton).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (checkGooglePlayServicesAvailable()) {
							haveGooglePlayServices();
						}
					}
				});

	}

	void showGooglePlayServicesAvailabilityErrorDialog(
			final int connectionStatusCode) {
		runOnUiThread(new Runnable() {
			public void run() {
				Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
						connectionStatusCode, SubmitActivity.this,
						REQUEST_GOOGLE_PLAY_SERVICES);
				dialog.show();
			}
		});
	}

	void refreshView() {
		// adapter = new ArrayAdapter<String>(this,
		// android.R.layout.simple_list_item_1, tasksList);
		// listView.setAdapter(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_GOOGLE_PLAY_SERVICES:
			if (resultCode == Activity.RESULT_OK) {
				haveGooglePlayServices();
			} else {
				checkGooglePlayServicesAvailable();
			}
			break;
		case REQUEST_AUTHORIZATION:
			if (resultCode == Activity.RESULT_OK) {
//				AsyncLoadYoutube.run(this);
				oauth_web();
			} else {
				chooseAccount();
			}
			break;
		case REQUEST_ACCOUNT_PICKER:
			if (resultCode == Activity.RESULT_OK && data != null
					&& data.getExtras() != null) {
				String accountName = data.getExtras().getString(
						AccountManager.KEY_ACCOUNT_NAME);
				oauth_web();
				if (accountName != null) {
					credential.setSelectedAccountName(accountName);
					SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString(PREF_ACCOUNT_NAME, accountName);
					editor.commit();
//					AsyncLoadYoutube.run(this);
				}
			}
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			// AsyncLoadYoutube.run(this);

			break;
		case R.id.menu_accounts:
			chooseAccount();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/** Check that Google Play services APK is installed and up to date. */
	private boolean checkGooglePlayServicesAvailable() {
		final int connectionStatusCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
			showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
			return false;
		}
		return true;
	}

	private void haveGooglePlayServices() {
		Log.d(TAG, "accountName=" + credential.getSelectedAccountName());
		// check if there is already an account selected
		if (credential.getSelectedAccountName() == null) {
			// ask user to choose account
			chooseAccount();
		} else {
			// upload youtube.
//			AsyncLoadYoutube.run(this);
			oauth_web();
		}
	}

	private void chooseAccount() {
		startActivityForResult(credential.newChooseAccountIntent(),
				REQUEST_ACCOUNT_PICKER);
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		if (id == 1) {
			dialog = new ProgressDialog(this);
			dialog.setTitle("upload video on youtube");
			dialog.setMessage("upload video on youtube");
			dialog.setMax(100);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

		}
		return dialog;
	}

	/**
	 * 真正的上傳
	 */
	public void upload() {
		AsyncLoadYoutube.run(this);
	}

	public void oauth_web() {
		pref = getSharedPreferences("AppPref", MODE_PRIVATE);

		final Dialog auth_dialog;
		auth_dialog = new Dialog(SubmitActivity.this);
		auth_dialog.setContentView(R.layout.auth_dialog);
		web = (WebView) auth_dialog.findViewById(R.id.webv);

		web.getSettings().setJavaScriptEnabled(true);
		web.loadUrl(OAUTH_URL + "?redirect_uri=" + REDIRECT_URI
				+ "&response_type=code&client_id=" + CLIENT_ID + "&scope="
				+ OAUTH_SCOPE);
		web.setWebViewClient(new WebViewClient() {

			boolean authComplete = false;
			Intent resultIntent = new Intent();

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);

			}

			String authCode;

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);

				if (url.contains("?code=") && authComplete != true) {
					Uri uri = Uri.parse(url);
					authCode = uri.getQueryParameter("code");
					Log.i("", "CODE : " + authCode);
					authComplete = true;
					resultIntent.putExtra("code", authCode);
					SubmitActivity.this.setResult(Activity.RESULT_OK,
							resultIntent);
					setResult(Activity.RESULT_CANCELED, resultIntent);

					SharedPreferences.Editor edit = pref.edit();
					edit.putString("Code", authCode);
					edit.commit();
					auth_dialog.dismiss();
					new TokenGet().execute();
					// Toast.makeText(getApplicationContext(),
					// "Authorization Code is: " + authCode,
					// Toast.LENGTH_SHORT).show();

				} else if (url.contains("error=access_denied")) {
					Log.i("", "ACCESS_DENIED_HERE");
					resultIntent.putExtra("code", authCode);
					authComplete = true;
					setResult(Activity.RESULT_CANCELED, resultIntent);
					// Toast.makeText(getApplicationContext(), "Error Occured",
					// Toast.LENGTH_SHORT).show();

					auth_dialog.dismiss();
				}
			}
		});
		auth_dialog.show();
		auth_dialog.setTitle("Authorize Learn2Crack");
		auth_dialog.setCancelable(true);
	}

	private class TokenGet extends AsyncTask<String, String, JSONObject> {
		private ProgressDialog pDialog;
		String Code;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(SubmitActivity.this);
			pDialog.setMessage("Contacting Google ...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			Code = pref.getString("Code", "");
			pDialog.show();
		}

		@Override
		protected JSONObject doInBackground(String... args) {
			GetAccessToken jParser = new GetAccessToken();
			JSONObject json = jParser.gettoken(TOKEN_URL, Code, CLIENT_ID,
					CLIENT_SECRET, REDIRECT_URI, GRANT_TYPE);
			return json;
		}

		@Override
		protected void onPostExecute(JSONObject json) {
			pDialog.dismiss();
			if (json != null) {

				// try {
				//
				// String tok = json.getString("access_token");
				// String expire = json.getString("expires_in");
				// String refresh = json.getString("refresh_token");
				//
				// Log.d("Token Access", tok);
				// Log.d("Expire", expire);
				// Log.d("Refresh", refresh);
				//
				// } catch (JSONException e) {
				// e.printStackTrace();
				// }

			} else {
				Toast.makeText(getApplicationContext(), "Network Error",
						Toast.LENGTH_SHORT).show();
				pDialog.dismiss();
			}

			upload();
		}
	}

}