package com.tchip.carlauncher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class NearActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
		setContentView(R.layout.activity_near);

		LinearLayout layoutBackFromNear = (LinearLayout) findViewById(R.id.layoutBackFromNear);
		layoutBackFromNear.setOnClickListener(new MyOnClickListener());

		ImageView imgNearOilStation = (ImageView) findViewById(R.id.imgNearOilStation);
		imgNearOilStation.setOnClickListener(new MyOnClickListener());

		ImageView imgNearHotel = (ImageView) findViewById(R.id.imgNearHotel);
		imgNearHotel.setOnClickListener(new MyOnClickListener());

		ImageView imgNear4S = (ImageView) findViewById(R.id.imgNear4S);
		imgNear4S.setOnClickListener(new MyOnClickListener());

	}

	class MyOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.layoutBackFromNear:
				backToMain();
				break;
			case R.id.imgNearOilStation:
				Intent intent1 = new Intent(NearActivity.this,
						NearResultActivity.class);
				intent1.putExtra("findType", "oil");
				startActivity(intent1);
				break;
			case R.id.imgNearHotel:
				Intent intent2 = new Intent(NearActivity.this,
						NearResultActivity.class);
				intent2.putExtra("findType", "hotel");
				startActivity(intent2);
				break;
			case R.id.imgNear4S:
				Intent intent3 = new Intent(NearActivity.this,
						NearResultActivity.class);
				intent3.putExtra("findType", "4s");
				startActivity(intent3);
				break;
			}

		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			backToMain();
			return true;
		} else
			return super.onKeyDown(keyCode, event);
	}

	private void backToMain() {
		finish();
		// add for animation start
		int version = Integer.valueOf(android.os.Build.VERSION.SDK);
		if (version > 5) {
			overridePendingTransition(R.anim.zms_translate_up_out,
					R.anim.zms_translate_up_in);
		}
		// add for animation end
	}
}