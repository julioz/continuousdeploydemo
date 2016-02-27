package br.com.zynger.continuousdeploydemo;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = (TextView) findViewById(R.id.activity_main_text);
        textView.setText(getString(R.string.app_message, BuildConfig.VERSION_NAME));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
