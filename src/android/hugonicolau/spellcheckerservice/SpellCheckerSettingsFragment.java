/**
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package android.hugonicolau.spellcheckerservice;

import android.hugonicolau.braillespellcheckerservice.R;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Preference screen.
 */
public class SpellCheckerSettingsFragment extends PreferenceFragment {

    /**
     * Empty constructor for fragment generation.
     */
    public SpellCheckerSettingsFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.spell_checker_settings);
    }
}
