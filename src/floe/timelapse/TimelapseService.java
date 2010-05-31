/****************************************************************\
*                Android Background Timelapse                    *
* Copyright (c) 2009-10 by Florian Echtler <floe@butterbrot.org> *
*  Licensed under GNU General Public License (GPL) 3 or later    *
\****************************************************************/

package floe.timelapse;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.view.SurfaceView;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Looper;
import android.widget.Toast;
import java.util.TimerTask;
import java.util.Timer;
import java.lang.Thread;
import java.lang.String;
import android.hardware.Camera;
import android.graphics.PixelFormat;
import java.io.File;
import java.io.FileOutputStream;
import android.util.Log;
import android.text.format.Time;


public class TimelapseService extends Service {

	private NotificationManager mNM;
	private Notification notification;
	private PendingIntent contentIntent;

	private String TAG;
	private Timer timer = null;
	private int counter;
	private Camera cam;
	private String outdir;

	private Camera.PreviewCallback imageCallback = new Camera.PreviewCallback() {
	//private Camera.PictureCallback imageCallback = new Camera.PictureCallback() {

		@Override
		public void onPreviewFrame( byte[] _data, Camera _camera ) {
		//public void onPictureTaken( byte[] _data, Camera _camera ) {

			Log.v( TAG, "picture retrieved, storing.." );
			//String myname = outdir.concat("img").concat(String.valueOf(counter++)).concat(".yuv");
			String myname = outdir.concat("img").concat(String.format("%06d",counter++)).concat(".yuv");

			// store JPEG data
			try {

				FileOutputStream outfile = new FileOutputStream( myname );
				outfile.write( _data );
				outfile.close();

				CharSequence text = "Images: ".concat(String.format("%d",counter));
				showNotification( text );

				Log.v( TAG, "picture stored successfully as " + myname );

			} catch (Exception e) {
				Log.e( TAG, "TimelapseService::imageCallback: " + e.toString() );
			}
		}
	};

	/*private Camera.AutoFocusCallback afCallback = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus( boolean success, Camera camera ) {
			if (success) {
				Log.v( TAG, "autofocus done, taking picture" );
				cam.takePicture( null, null, imageCallback );
			} else {
				Log.v( TAG, "autofocus failed" );
			}
		}
	};*/

	private TimerTask myLoop = new TimerTask() {
		int num;
		@Override
		public void run() {
			//Log.v( TAG, "starting autofocus" );
			//cam.autoFocus( afCallback );
			Log.v( TAG, "taking picture" );
			//cam.takePicture( null, null, imageCallback );
			cam.setOneShotPreviewCallback( imageCallback );
		}
	};

	public class TimelapseBinder extends Binder {
		TimelapseService getService() {
			return TimelapseService.this;
		}
	}
	
	private final IBinder mBinder = new TimelapseBinder();

	@Override
	public IBinder onBind( Intent intent ) {
		return mBinder;
	}

	// called when service gets started
	@Override public void onCreate() {

		TAG = "Timelapse";

		Toast.makeText(this, "Timelapse service started", Toast.LENGTH_SHORT).show();

		cam = Camera.open();

		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting.  We put an icon in the status bar.
		notification = new Notification( R.drawable.camera_tiny, "Timelapse Image Service started", System.currentTimeMillis() );
		notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;

		// The PendingIntent to launch our activity if the user selects this notification
		contentIntent = PendingIntent.getActivity(this, 0, new Intent(TimelapseService.this, Timelapse.class), 0);

		showNotification( "Images: 0" );
	}

	// called when service quits
	@Override public void onDestroy() {
		// Tell the user we stopped.
		Toast.makeText(this, "Timelapse service stopped", Toast.LENGTH_SHORT).show();

		// Cancel the persistent notification.
		mNM.cancel(R.id.bind);

		// stop the timer
		timer.cancel();

		// cleanup the camera
		cam.stopPreview();
		cam.release();
	}

	// after service has been started, this is called from the Activity to set the preview surface
	public void setView( SurfaceView sv ) {
		try {
			Log.v( TAG, "Surfaceview: " + sv.toString() );
			cam.setPreviewDisplay( sv.getHolder() );
			cam.startPreview();
		} catch (Exception e) {
			Log.e( TAG, "TimelapseService::setView: " + e.toString() );
		}
	}

	// launch the timer
	public void startTimer( int delay ) {
		if (timer != null) return;
		timer = new Timer("TimelapseService");
		timer.scheduleAtFixedRate( myLoop, delay, delay );
	}

	public void setOutdir( String dir ) {
		outdir = dir;
	}

	// put up a persistent notification icon for this service
	private void showNotification( CharSequence text ) {

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo( TimelapseService.this, "Timelapse Image Service", text, contentIntent );

		// Send the notification.
		// We use a layout id because it is a unique number.  We use it later to cancel.
		mNM.notify(R.id.bind, notification);
	}
}
