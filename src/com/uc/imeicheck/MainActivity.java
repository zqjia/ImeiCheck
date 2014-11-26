package com.uc.imeicheck;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends Activity {
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        

        //第二到第六个参数依次为platform，product，version，subversion和IMEI号
        //大小写不影响
        new ImeiCheck(this, "Android", "ucmobile", "9.9.8", "100", "864691022852926", new ImeiCheckCallBack() {
			@Override
			public void handleFail() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void handleSuccess() {
				// TODO Auto-generated method stub
				
			}
			
		}).startCheck();
              
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
   
}
