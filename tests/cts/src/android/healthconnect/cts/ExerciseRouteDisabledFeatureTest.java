/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.healthconnect.cts;

import static android.Manifest.permission.WRITE_DEVICE_CONFIG;

import static com.google.common.truth.Truth.assertThat;

import android.app.UiAutomation;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.Record;
import android.provider.DeviceConfig;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;

import java.util.List;

public class ExerciseRouteDisabledFeatureTest {

    private final UiAutomation mUiAutomation =
            InstrumentationRegistry.getInstrumentation().getUiAutomation();

    @After
    public void tearDown() {
        setExerciseRouteFeatureEnabledFlag(true);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testWriteRoute_insertWithDisableFeature_throwsException()
            throws InterruptedException {
        setExerciseRouteFeatureEnabledFlag(false);
        List<Record> records = List.of(TestUtils.buildExerciseSession());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadRoute_insertAndRead_routeIsNotAvailable() throws InterruptedException {
        List<Record> records = List.of(TestUtils.buildExerciseSession());
        TestUtils.insertRecords(records);
        setExerciseRouteFeatureEnabledFlag(false);
        ExerciseSessionRecord insertedRecord = (ExerciseSessionRecord) records.get(0);
        assertThat(insertedRecord.hasRoute()).isTrue();
        assertThat(insertedRecord.getRoute()).isNotNull();

        ReadRecordsRequestUsingIds.Builder<ExerciseSessionRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class);
        request.addId(records.get(0).getMetadata().getId());
        ExerciseSessionRecord readRecord = TestUtils.readRecords(request.build()).get(0);
        assertThat(readRecord.hasRoute()).isFalse();
        assertThat(readRecord.getRoute()).isNull();
    }

    private void setExerciseRouteFeatureEnabledFlag(boolean flag) {
        mUiAutomation.adoptShellPermissionIdentity(WRITE_DEVICE_CONFIG);
        DeviceConfig.setProperty(
                "health_connect", "exercise_routes_enable", flag ? "true" : "false", false);
        mUiAutomation.dropShellPermissionIdentity();
    }
}