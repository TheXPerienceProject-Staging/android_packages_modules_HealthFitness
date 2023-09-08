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

package com.android.server.healthconnect.storage.datatypehelpers;

import static com.android.server.healthconnect.TestUtils.TEST_USER;
import static com.android.server.healthconnect.storage.datatypehelpers.StepsRecordHelper.STEPS_TABLE_NAME;

import static com.google.common.truth.Truth.assertThat;

import android.database.Cursor;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;

import androidx.test.runner.AndroidJUnit4;

import com.android.server.healthconnect.HealthConnectUserContext;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class RecordHelperTest {
    private static final String TEST_PACKAGE_NAME = "package.name";

    @Rule public final HealthConnectDatabaseTestRule testRule = new HealthConnectDatabaseTestRule();
    private TransactionTestUtils mTransactionTestUtils;

    private TransactionManager mTransactionManager;

    @Before
    public void setup() throws Exception {
        HealthConnectUserContext context = testRule.getUserContext();
        mTransactionManager = TransactionManager.getInstance(context);
        mTransactionTestUtils = new TransactionTestUtils(context, TEST_USER);
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
    }

    @Test
    public void getInternalRecords_insertThenRead_recordReturned() {
        RecordHelper<?> helper = new StepsRecordHelper();
        String uid = mTransactionTestUtils.insertStepsRecord(4000, 5000, 100);
        ReadTableRequest request = new ReadTableRequest(STEPS_TABLE_NAME);
        try (Cursor cursor = mTransactionManager.read(request)) {
            List<RecordInternal<?>> records = helper.getInternalRecords(cursor, 1);
            assertThat(records).hasSize(1);

            StepsRecordInternal record = (StepsRecordInternal) records.get(0);
            assertThat(record.getUuid()).isEqualTo(UUID.fromString(uid));
            assertThat(record.getStartTimeInMillis()).isEqualTo(4000);
            assertThat(record.getEndTimeInMillis()).isEqualTo(5000);
            assertThat(record.getCount()).isEqualTo(100);
        }
    }
}