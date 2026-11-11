package io.krypton.plugin;

import android.app.Activity;
import android.os.Bundle;

// Activity kosong untuk launcher; langsung ditutup.
public final class PluginActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
}
