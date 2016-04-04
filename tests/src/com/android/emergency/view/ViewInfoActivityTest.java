/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.emergency.view;

import android.app.Fragment;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.android.emergency.PreferenceKeys;
import com.android.emergency.R;
import com.android.emergency.edit.EditInfoActivity;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Tests for {@link ViewInfoActivity}.
 */
public class ViewInfoActivityTest extends ActivityInstrumentationTestCase2<ViewInfoActivity> {
    private ArrayList<Pair<String, Fragment>> mFragments;
    private LinearLayout mPersonalCard;
    private TextView mPersonalCardLargeItem;
    private TextView mPersonalCardSmallItem;
    private ViewFlipper mViewFlipper;
    private int mNoInfoIndex;
    private int mTabsIndex;

    public ViewInfoActivityTest() {
        super(ViewInfoActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        enableActivity();
        mPersonalCard = (LinearLayout)  getActivity().findViewById(R.id.name_and_dob_linear_layout);
        mPersonalCardLargeItem = (TextView)  getActivity().findViewById(R.id.personal_card_large);
        mPersonalCardSmallItem = (TextView)  getActivity().findViewById(R.id.personal_card_small);
        mViewFlipper = (ViewFlipper)  getActivity().findViewById(R.id.view_flipper);
        mNoInfoIndex = mViewFlipper
                .indexOfChild(getActivity().findViewById(R.id.no_info_text_view));
        mTabsIndex = mViewFlipper.indexOfChild(getActivity().findViewById(R.id.tabs));

        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().clear().apply();
    }

    public void testInitialState() throws Throwable {
        onPause();
        onResume();

        mFragments = getActivity().getFragments();
        assertEquals(0, mFragments.size());
        assertEquals(View.GONE, mPersonalCard.getVisibility());
        assertEquals(View.GONE, getActivity().getTabLayout().getVisibility());
        assertEquals(mNoInfoIndex, mViewFlipper.getDisplayedChild());
    }

    public void testNameSet() throws Throwable {
        onPause();

        final String name = "John";
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit().putString(PreferenceKeys.KEY_NAME, name).apply();

        onResume();

        mFragments = getActivity().getFragments();
        assertEquals(0, mFragments.size());
        assertEquals(View.GONE, getActivity().getTabLayout().getVisibility());
        assertEquals(View.VISIBLE,
                getActivity().findViewById(R.id.no_info_text_view).getVisibility());
        assertEquals(mNoInfoIndex, mViewFlipper.getDisplayedChild());
        assertEquals(View.VISIBLE, mPersonalCardLargeItem.getVisibility());
        assertEquals(name, mPersonalCardLargeItem.getText());
        assertEquals(View.GONE, mPersonalCardSmallItem.getVisibility());
    }

    public void testDateOfBirthSet() throws Throwable {
        onPause();

        final long dateOfBirth = 537148800000L;
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit().putLong(PreferenceKeys.KEY_DATE_OF_BIRTH, dateOfBirth).apply();

        onResume();

        mFragments = getActivity().getFragments();
        assertEquals(0, mFragments.size());
        assertEquals(View.GONE, getActivity().getTabLayout().getVisibility());
        assertEquals(mNoInfoIndex, mViewFlipper.getDisplayedChild());
        assertEquals(View.VISIBLE, mPersonalCardLargeItem.getVisibility());
        assertEquals(View.VISIBLE, mPersonalCardSmallItem.getVisibility());
    }

    public void testSetEmergencyInfo() throws Throwable {
        onPause();

        final String allergies = "Peanuts";
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit().putString(PreferenceKeys.KEY_ALLERGIES, allergies).apply();

        onResume();

        mFragments = getActivity().getFragments();
        assertEquals(View.GONE, getActivity().getTabLayout().getVisibility());
        assertEquals(mTabsIndex, mViewFlipper.getDisplayedChild());
        assertEquals(1, mFragments.size());
        ViewEmergencyInfoFragment viewEmergencyInfoFragment =
                (ViewEmergencyInfoFragment) mFragments.get(0).second;
        assertNotNull(viewEmergencyInfoFragment);
    }

    //TODO: test set one emergency contact (tab layout still not shown) and set both emergency info
    // and emergency contacts (tab layout shown)

    public void testComputeAge() {
        Calendar today = Calendar.getInstance();
        Calendar dateOfBirth = Calendar.getInstance();

        today.set(2016, 1, 9);
        dateOfBirth.set(1987, 1, 9);
        assertEquals(29, ViewInfoActivity.computeAge(today, dateOfBirth));

        // One day before birthday
        today.set(2016, 1, 8);
        dateOfBirth.set(1987, 1, 9);
        assertEquals(28, ViewInfoActivity.computeAge(today, dateOfBirth));

        // One month before birthday
        today.set(2016, 0, 9);
        dateOfBirth.set(1987, 1, 9);
        assertEquals(28, ViewInfoActivity.computeAge(today, dateOfBirth));

        // Today is same day as birthday
        today.set(2016, 1, 9);
        dateOfBirth.set(2016, 1, 9);
        assertEquals(0, ViewInfoActivity.computeAge(today, dateOfBirth));

        // Today is before birthday
        today.set(2013, 4, 17);
        dateOfBirth.set(2016, 1, 9);
        assertEquals(0, ViewInfoActivity.computeAge(today, dateOfBirth));
    }

    public void testCanGoToEditInfoActivity() {
        final ViewInfoActivity activity = getActivity();

        Instrumentation.ActivityMonitor activityMonitor =
                getInstrumentation().addMonitor(EditInfoActivity.class.getName(),
                        null /* result */, false /* block */);

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().invokeMenuActionSync(activity, R.id.action_edit, 0 /* flags */);

        EditInfoActivity editInfoActivity = (EditInfoActivity)
                getInstrumentation().waitForMonitorWithTimeout(activityMonitor, 1000 /* timeOut */);
        assertNotNull(editInfoActivity);
        assertEquals(true, getInstrumentation().checkMonitorHit(activityMonitor, 1 /* minHits */));
        editInfoActivity.finish();
    }


    private void onPause() {
        getInstrumentation().callActivityOnPause(getActivity());
    }

    private void onResume() throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                getInstrumentation().callActivityOnResume(getActivity());
            }
        });
        getInstrumentation().waitForIdleSync();
    }

    private void enableActivity() {
        // This activity is disabled when no info is set by the EditInfoActivity. We enable it here
        // for the tests.
        PackageManager pm = getInstrumentation().getContext().getPackageManager();
        final ComponentName mComponentName =
                new ComponentName("com.android.emergency",
                        "com.android.emergency.view.ViewInfoActivity");
        pm.setComponentEnabledSetting(mComponentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }
}