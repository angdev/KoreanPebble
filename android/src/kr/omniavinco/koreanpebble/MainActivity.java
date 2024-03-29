package kr.omniavinco.koreanpebble;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.os.Bundle;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;

public class MainActivity extends Activity {
	SharedPreferences preferences;
	TableLayout table;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (item.getItemId() == R.id.action_send_msg) {
			
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle("Title");

					// Set up the input
					final EditText input = new EditText(getBaseContext());
					// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
					input.setInputType(InputType.TYPE_CLASS_TEXT);
					input.setTextColor(Color.BLACK);
					input.setTextSize(18);
					builder.setView(input);

					// Set up the buttons
					builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
							final Map<String, Object> data = new HashMap<String, Object>();
					        data.put("title", "사용자 메시지");
					        data.put("body", input.getText().toString());
					        final JSONObject jsonData = new JSONObject(data);
					        final String notificationData = jsonData.toString();

					        // Create the intent to house the Pebble notification
					        final Intent intent = new Intent(Constants.INTENT_SEND_PEBBLE_NOTIFICATION);
					        intent.putExtra("messageType", Constants.PEBBLE_MESSAGE_TYPE_ALERT);
					        intent.putExtra("sender", "custom message");
					        intent.putExtra("notificationData", notificationData);

					        sendBroadcast(intent);
					    }
					});
					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
					        dialog.cancel();
					    }
					});
					builder.show();
				}
			});
			
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		preferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
		setContentView(R.layout.activity_main);
		table = (TableLayout) findViewById(R.id.appList);
		updateAppTable();
		AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Service.ACCESSIBILITY_SERVICE);
		// feedbackSpoken|feedbackHaptic|feedbackVisual|feedbackGeneric
		List<AccessibilityServiceInfo> installedServices = accessibilityManager
				.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        
        boolean found = false;
        for (AccessibilityServiceInfo serviceInfo : installedServices) {
        	String key = serviceInfo.getId();
        	if (key.equals("kr.omniavinco.koreanpebble/.NotiService")) {
        		found = true;
        		break;
        	}
        }
        if (!found) {
        	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        	builder.setTitle("Warning");
        	builder.setMessage(getString(R.string.need_to_accessibility_setup));
        	builder.setPositiveButton("Open Setting", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
					startActivityForResult(intent, 0);
				}
			});
        	builder.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        dialog.cancel();
			    }
			});
        	builder.show();
        }
	}

	protected void updateAppTable() {
		table.removeAllViewsInLayout();
		int dip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float) 1, getResources().getDisplayMetrics());
		final PackageManager pm = getPackageManager();
		//get a list of installed apps.
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

		for (final ApplicationInfo packageInfo : packages) {
			/*if ((packageInfo.flags & ApplicationInfo.FLAG_) != 0) {
				continue;
			}*/
			String appName = packageInfo.loadLabel(getPackageManager()).toString();
			TableRow row = new TableRow(this);
			TextView title = new TextView(this);
			title.setText(appName);
			title.setHeight(50 * dip);
			ImageView icon = new ImageView(this);
			icon.setImageDrawable(packageInfo.loadIcon(getPackageManager()));
			Switch notiSwitch = new Switch(this);
			notiSwitch.setWidth(100 * dip);
			notiSwitch.setHeight(50 * dip);
			notiSwitch.setGravity(Gravity.RIGHT);
			if (preferences.contains(packageInfo.packageName)) {
				notiSwitch.setChecked(preferences.getBoolean(packageInfo.packageName, false));
			}
			notiSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				final String packageName = String.copyValueOf(packageInfo.packageName.toCharArray());
				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
					Editor editor = preferences.edit();
					if (arg1) {
						editor.putBoolean(packageName, arg1);
					} else if (preferences.contains(packageName)) {
						editor.remove(packageName);	
					}
					editor.commit();
				}
			});
			row.addView(icon, 50 * dip, 50 * dip);
			row.addView(title);
			row.addView(notiSwitch);
			table.addView(row, new TableLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
