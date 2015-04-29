package com.tchip.carlauncher.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.tchip.carlauncher.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class RouteListActivity extends Activity {
	private ListView routeList;
	private final String ROUTE_PATH = "/sdcard/Route/";
	private ArrayAdapter<String> adapter;
	private CalendarView filterDate;
	private TextView tvNoFile;

	private int focusItemPos = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_route_list);

		tvNoFile = (TextView) findViewById(R.id.tvNoFile);
		tvNoFile.setVisibility(View.GONE);

		filterDate = (CalendarView) findViewById(R.id.filterDate);
		filterDate.setOnDateChangeListener(new MyDateChangeListener());

		routeList = (ListView) findViewById(R.id.routeList);
		showRouteList("20");
	}

	private void showRouteList(String datePrefix) {
		try {
			File[] files = new File(ROUTE_PATH).listFiles();
			final List<String> fileNameList = new ArrayList<String>();
			for (File file : files) {
				String fileName = file.getName();
				String format = fileName.substring(
						fileName.lastIndexOf('.') + 1, fileName.length());
				if (format.equals("txt") || format.equals("json"))
					if (file.getName().startsWith(datePrefix))
						fileNameList.add(file.getName());
			}

			if (fileNameList.isEmpty()) {
				tvNoFile.setVisibility(View.VISIBLE);
			}

			// (context, resource, textViewResourceId, objects)
			adapter = new ArrayAdapter<String>(this, R.layout.route_list_item,
					R.id.text, fileNameList);
			routeList.setAdapter(adapter);

			// 单击监听
			routeList
					.setOnItemClickListener(new AdapterView.OnItemClickListener() {
						public void onItemClick(
								android.widget.AdapterView<?> parent,
								android.view.View view, int position, long id) {
							focusItemPos = position;
							Intent intent = new Intent(RouteListActivity.this,
									RouteShowActivity.class);
							intent.putExtra("filePath",
									fileNameList.get(position));
							startActivity(intent);
						}
					});

			// 长按弹出ContextMenu
			registerForContextMenu(routeList);

		} catch (Exception e) {
			e.printStackTrace();
			tvNoFile.setVisibility(View.VISIBLE);
			tvNoFile.setText("无轨迹文件");
		}
	}

	class MyDateChangeListener implements CalendarView.OnDateChangeListener {

		@Override
		public void onSelectedDayChange(CalendarView view, int year, int month,
				int dayOfMonth) {
			String strDate = "";
			String strDay = "" + dayOfMonth;
			month = month + 1;
			String strMonth = "" + month;
			if (month < 10)
				strMonth = "0" + month;
			if (dayOfMonth < 10)
				strDay = "0" + dayOfMonth;
			strDate = year + strMonth + strDay;
			Toast.makeText(getApplicationContext(), strDate, Toast.LENGTH_SHORT)
					.show();

			showRouteList(strDate);

		}

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.setHeaderTitle("选项");
		// add(int groupId, int itemId, int order,
		// CharSequence title)
		menu.add(0, 0, 0, "删除");
		menu.add(0, 1, 1, "编辑");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// ContextMenuInfo menuInfo = (ContextMenuInfo) item.getMenuInfo();
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		// 获得AdapterContextMenuInfo,以此来获得选择的listview项目

		switch (item.getItemId()) {
		case 0:
			// 删除
			File file = new File(ROUTE_PATH
					+ adapter.getItem(menuInfo.position));
			file.delete();
			DeleteUpdateList(menuInfo.position);
			return true;
		case 1:
			// 分享
			// Intent intent = new Intent("android.intent.action.VIEW");
			Intent intent = new Intent("android.intent.action.EDIT");
			// intent.addCategory("android.intent.category.DEFAULT");
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			Uri uri = Uri.fromFile(new File(ROUTE_PATH
					+ adapter.getItem(menuInfo.position)));
			intent.setDataAndType(uri, "text/plain");
			startActivity(intent);
			return true;
		}
		return false;
	}

	/**
	 * 用ContextMenu删除Item时刷新ListView
	 * 
	 * @param position
	 */
	private void DeleteUpdateList(int position) {
		adapter.remove(adapter.getItem(position));
	}

	@Override
	protected void onResume() {
		super.onResume();
		View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
	}
}