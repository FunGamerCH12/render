package io.krypton.plugin;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

// Layar info sederhana saat plugin dibuka langsung dari launcher HP.
public final class InfoActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView t = new TextView(this);
        t.setText("Krypton Wrapper (NG-GL4ES) + PowerVR optimizer\n\n"
                + "Ini plugin renderer. Buka Zalith Launcher 2, lalu pilih\n"
                + "renderer \"Krypton Wrapper (Optimizer)\".");
        t.setGravity(Gravity.CENTER);
        t.setTextSize(16f);
        t.setPadding(48, 48, 48, 48);
        setContentView(t);
    }
}
