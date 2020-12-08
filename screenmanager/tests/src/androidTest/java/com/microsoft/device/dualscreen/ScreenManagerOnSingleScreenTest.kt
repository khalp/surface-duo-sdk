/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.device.dualscreen

import android.content.res.Configuration
import android.view.Surface
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.microsoft.device.dualscreen.utils.SINGLE_SCREEN_HINGE_RECT
import com.microsoft.device.dualscreen.utils.SINGLE_SCREEN_WINDOW_RECT
import com.microsoft.device.dualscreen.utils.START_SCREEN_RECT
import com.microsoft.device.dualscreen.utils.ScreenInfoListenerImpl
import com.microsoft.device.dualscreen.utils.any
import com.microsoft.device.dualscreen.utils.setOrientationLeft
import com.microsoft.device.dualscreen.utils.setOrientationRight
import com.microsoft.device.dualscreen.utils.unfreezeRotation
import org.junit.After
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.mockito.Mockito
import org.mockito.Mockito.times

@MediumTest
@RunWith(AndroidJUnit4ClassRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ScreenManagerOnSingleScreenTest {
    @Rule
    @JvmField
    var rule: ActivityTestRule<SampleActivity> = ActivityTestRule(SampleActivity::class.java, false, false)
    private var screenInfoListener = ScreenInfoListenerImpl()

    @After
    fun tearDown() {
        val screenManager = ScreenManagerProvider.getScreenManager()
        screenManager.removeScreenInfoListener(screenInfoListener)
        screenInfoListener.resetScreenInfo()
    }

    @Test
    fun testScreenInfo() {
        assertThat(screenInfoListener.screenInfo).isNull()

        rule.launchActivity(null)
        getInstrumentation().waitForIdleSync()
        ScreenManagerProvider.getScreenManager().addScreenInfoListener(screenInfoListener)

        assertThat(screenInfoListener.screenInfo).isNotNull()
        assertThat(screenInfoListener.screenInfo?.isSurfaceDuoDevice()).isTrue()
        assertThat(screenInfoListener.screenInfo?.isDualMode()).isFalse()
        assertThat(screenInfoListener.screenInfo?.getHinge()).isEqualTo(SINGLE_SCREEN_HINGE_RECT)

        val screenRectangles = screenInfoListener.screenInfo?.getScreenRectangles()
        assertThat(screenRectangles?.size).isEqualTo(1)
        assertThat(screenRectangles?.get(0)).isEqualTo(START_SCREEN_RECT)

        assertThat(screenInfoListener.screenInfo?.getWindowRect()).isEqualTo(SINGLE_SCREEN_WINDOW_RECT)
    }

    @Test
    fun testScreenRotation() {
        assertThat(screenInfoListener.screenInfo).isNull()

        rule.launchActivity(null)
        getInstrumentation().waitForIdleSync()
        ScreenManagerProvider.getScreenManager().addScreenInfoListener(screenInfoListener)

        screenInfoListener.waitForScreenInfoChanges()
        assertThat(screenInfoListener.screenInfo).isNotNull()
        assertThat(screenInfoListener.screenInfo?.getScreenRotation()).isEqualTo(Surface.ROTATION_0)
        screenInfoListener.resetScreenInfoCounter()

        setOrientationLeft()
        screenInfoListener.waitForScreenInfoChanges()
        assertThat(screenInfoListener.screenInfo?.getScreenRotation()).isEqualTo(Surface.ROTATION_90)
        screenInfoListener.resetScreenInfoCounter()

        setOrientationRight()
        screenInfoListener.waitForScreenInfoChanges()
        assertThat(screenInfoListener.screenInfo?.getScreenRotation()).isEqualTo(Surface.ROTATION_270)
        screenInfoListener.resetScreenInfoCounter()

        unfreezeRotation()
    }

    @Test
    fun testOnConfigurationChanged() {
        assertThat(screenInfoListener.screenInfo).isNull()
        ScreenManagerProvider.getScreenManager().addScreenInfoListener(screenInfoListener)

        rule.launchActivity(null)
        getInstrumentation().waitForIdleSync()
        rule.runOnUiThread {
            rule.activity.onConfigurationChanged(Configuration())
        }

        assertThat(screenInfoListener.screenInfo).isNotNull()
    }

    @Test
    fun testMultipleListeners() {
        val screenManager = ScreenManagerProvider.getScreenManager()
        val screenInfo1 = Mockito.mock(ScreenInfoListener::class.java)
        Mockito.doNothing().`when`(screenInfo1).onScreenInfoChanged(any())
        screenManager.addScreenInfoListener(screenInfo1)

        val screenInfo2 = Mockito.mock(ScreenInfoListener::class.java)
        Mockito.doNothing().`when`(screenInfo2).onScreenInfoChanged(any())
        screenManager.addScreenInfoListener(screenInfo2)

        val screenInfo3 = Mockito.mock(ScreenInfoListener::class.java)
        Mockito.doNothing().`when`(screenInfo3).onScreenInfoChanged(any())
        screenManager.addScreenInfoListener(screenInfo3)

        rule.launchActivity(null)
        getInstrumentation().waitForIdleSync()
        // Activity is started
        Mockito.verify(screenInfo1, times(1)).onScreenInfoChanged(any())
        Mockito.verify(screenInfo2, times(1)).onScreenInfoChanged(any())
        Mockito.verify(screenInfo3, times(1)).onScreenInfoChanged(any())

        screenManager.removeScreenInfoListener(screenInfo1)
        screenManager.removeScreenInfoListener(screenInfo2)
        screenManager.removeScreenInfoListener(screenInfo3)
    }
}