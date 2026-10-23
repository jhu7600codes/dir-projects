package net.sourceforge.opencamera;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

/** Lenz's settings screen: a short list of the settings people actually change.
 */
public class LenzSettingsActivity extends AppCompatActivity {
    private static final String SOURCE_URL = "https://github.com/jhu7600codes/dir-projects";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float density = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.lenz_settings_background));

        TextView title = new TextView(this);
        title.setText(R.string.lenz_settings);
        title.setTextColor(ContextCompat.getColor(this, R.color.lenz_on_surface));
        title.setTextSize(32);
        title.setGravity(Gravity.BOTTOM);
        title.setPadding((int)(24 * density), (int)(72 * density), (int)(24 * density), (int)(24 * density));
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.lenz_settings_container);
        root.addView(container, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        setContentView(root);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        if( savedInstanceState == null ) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.lenz_settings_container, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.lenz_settings, rootKey);

            Preference version = findPreference("lenz_version");
            if( version != null ) {
                try {
                    PackageInfo info = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
                    version.setSummary(info.versionName);
                }
                catch(PackageManager.NameNotFoundException e) {
                    version.setSummary(null);
                }
            }
            Preference performance = findPreference(LenzPerformance.PerformancePreferenceKey);
            if( performance != null ) {
                performance.setSummaryProvider(p -> {
                    String value = ((androidx.preference.ListPreference)p).getValue();
                    if( LenzPerformance.PERFORMANCE_ULTRA.equals(value) )
                        return getString(R.string.lenz_performance_ultra);
                    if( LenzPerformance.PERFORMANCE_OFF.equals(value) )
                        return getString(R.string.lenz_performance_off);
                    return getString(LenzPerformance.isLowEndDevice(requireContext()) ? R.string.lenz_performance_auto_on : R.string.lenz_performance_auto_off);
                });
            }
            Preference source = findPreference("lenz_source");
            if( source != null ) {
                source.setOnPreferenceClickListener(p -> {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)));
                    return true;
                });
            }
        }
    }
}
