/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.healthconnect.datatypes.BasalMetabolicRateRecord;
import android.healthconnect.datatypes.InstantRecord;
import android.healthconnect.datatypes.IntervalRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.StepsRecord;
import android.healthconnect.datatypes.units.Power;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class GetActivityDatesTest {
    private static final String TAG = "GetActivityDatesTest";

    // TODO(b/257796081): Test the response size after database clean up is implemented
    //    @Test
    //    public void testEmptyActivityDates() throws InterruptedException {
    //        List<Record> records = getTestRecords();

    //         List<LocalDate> activityDates = getActivityDates(
    //                 records.stream().map(Record::getClass).collect(Collectors.toList()));
    //
    //        assertThat(activityDates).hasSize(0);
    //    }

    @Test
    public void testActivityDates() throws InterruptedException {
        List<Record> records = getTestRecords();
        TestUtils.insertRecords(records);
        // Wait for some time, as activity dates are updated in the background so might take some
        // additional time.
        Thread.sleep(500);
        List<LocalDate> activityDates =
                TestUtils.getActivityDates(
                        records.stream().map(Record::getClass).collect(Collectors.toList()));
        assertThat(activityDates.size()).isGreaterThan(1);
        assertThat(activityDates)
                .containsAtLeastElementsIn(
                        records.stream().map(this::getRecordDate).collect(Collectors.toSet()));
    }

    private List<Record> getTestRecords() {
        return new ArrayList<>(
                Arrays.asList(
                        new StepsRecord.Builder(
                                        new Metadata.Builder().build(),
                                        Instant.now(),
                                        Instant.now(),
                                        10)
                                .build(),
                        new BasalMetabolicRateRecord.Builder(
                                        new Metadata.Builder().build(),
                                        Instant.now().minus(3, ChronoUnit.DAYS),
                                        Power.fromWatts(100.0))
                                .build()));
    }

    private LocalDate getRecordDate(Record record) {
        LocalDate activityDate;
        if (record instanceof IntervalRecord) {
            activityDate =
                    LocalDate.ofInstant(
                            ((IntervalRecord) record).getStartTime(),
                            ((IntervalRecord) record).getStartZoneOffset());
        } else if (record instanceof InstantRecord) {
            activityDate =
                    LocalDate.ofInstant(
                            ((InstantRecord) record).getTime(),
                            ((InstantRecord) record).getZoneOffset());
        } else {
            activityDate =
                    LocalDate.ofInstant(
                            Instant.now(),
                            ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        }
        return activityDate;
    }
}
