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

import static android.Manifest.permission.CAMERA;
import static android.healthconnect.HealthConnectManager.isHealthPermission;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.HealthPermissions;
import android.healthconnect.InsertRecordsResponse;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.ReadRecordsRequestUsingIds;
import android.healthconnect.ReadRecordsResponse;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.BasalMetabolicRateRecord;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.HeartRateRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.StepsRecord;
import android.healthconnect.datatypes.units.Power;
import android.os.OutcomeReceiver;
import android.platform.test.annotations.AppModeFull;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** CTS test for API provided by HealthConnectManager. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerTest {
    static final class RecordAndIdentifier {
        private final int id;
        private final Record recordClass;

        public RecordAndIdentifier(int id, Record recordClass) {
            this.id = id;
            this.recordClass = recordClass;
        }

        public int getId() {
            return id;
        }

        public Record getRecordClass() {
            return recordClass;
        }
    }

    private static final String TAG = "HealthConnectManagerTest";

    @Test
    public void testHCManagerIsAccessible_viaHCManager() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
    }

    @Test
    public void testHCManagerIsAccessible_viaContextConstant() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
    }

    @Test
    public void testRecordIdentifiers() {
        for (RecordAndIdentifier recordAndIdentifier : getRecordsAndIdentifiers()) {
            assertThat(recordAndIdentifier.getRecordClass().getRecordType())
                    .isEqualTo(recordAndIdentifier.getId());
        }
    }

    @Test
    public void testInsertRecords() throws Exception {
        insertRecords(getTestRecords());
    }

    @Test
    public void testReadRecord_usingIds() throws InterruptedException {
        testRead_StepsRecordIds();
        testRead_HeartRateRecord();
        testRead_BasalMetabolicRateRecord();
    }

    @Test
    public void testReadRecord_usingClientRecordIds() throws InterruptedException {
        testRead_StepsRecordClientIds();
    }

    @Test
    public void testReadRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class).addId("abc").build();
        List<StepsRecord> result = readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<StepsRecord> result = readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testIsHealthPermission_forHealthPermission_returnsTrue() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        assertThat(isHealthPermission(context, HealthPermissions.READ_ACTIVE_CALORIES_BURNED))
                .isTrue();
        assertThat(isHealthPermission(context, HealthPermissions.READ_ACTIVE_CALORIES_BURNED))
                .isTrue();
    }

    @Test
    public void testIsHealthPermission_forNonHealthGroupPermission_returnsFalse() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        assertThat(isHealthPermission(context, HealthPermissions.MANAGE_HEALTH_PERMISSIONS))
                .isFalse();
        assertThat(isHealthPermission(context, CAMERA)).isFalse();
    }

    @Test
    public void testReadStepsRecordUsingFilters_default() throws InterruptedException {
        List<StepsRecord> oldStepsRecords =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        insertRecords(Collections.singletonList(getStepsRecord()));
        List<StepsRecord> newStepsRecords =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        assertThat(newStepsRecords.size()).isEqualTo(oldStepsRecords.size() + 1);
        assertThat(newStepsRecords.get(newStepsRecords.size() - 1).getCount())
                .isEqualTo(getStepsRecord().getCount());
    }

    @Test
    public void testReadStepsRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        insertRecords(Collections.singletonList(getStepsRecord()));
        List<StepsRecord> newStepsRecords =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newStepsRecords.size()).isEqualTo(1);
        assertThat(newStepsRecords.get(0).getCount()).isEqualTo(getStepsRecord().getCount());
    }

    @Test
    public void testReadStepsRecordUsingFilters_dataFilter_incorrect() throws InterruptedException {
        insertRecords(Collections.singletonList(getStepsRecord()));
        List<StepsRecord> newStepsRecords =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newStepsRecords.size()).isEqualTo(0);
    }

    @Test
    public void testReadStepsRecordUsingFilters_dataFilter_correct() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<StepsRecord> oldStepsRecords =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        insertRecords(Collections.singletonList(getStepsRecord()));
        List<StepsRecord> newStepsRecords =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newStepsRecords.size() - oldStepsRecords.size()).isEqualTo(1);
        assertThat(newStepsRecords.get(0).getCount()).isEqualTo(getStepsRecord().getCount());
    }

    // TODO(b/257796081): Move read tests to respective record type classes, verify that the correct
    // details are being fetched, and add tests for all record type

    /**
     * Test to verify the working of {@link
     * android.healthconnect.HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, update them and check by reading them.
     */
    @Test
    public void testUpdateRecords_validInput_dataBaseUpdatedSuccessfully()
            throws InterruptedException, InvocationTargetException, InstantiationException,
                    IllegalAccessException, NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<HealthConnectException> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = insertRecords(getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords(/* isSetClientRecordId */ false);

        // Modify the Uid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords
                    .get(itr)
                    .getMetadata()
                    .setId(insertRecords.get(itr).getMetadata().getId());
        }

        service.updateRecords(
                updateRecords,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<Void, HealthConnectException>() {
                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        responseException.set(exception);
                        latch.countDown();
                        Log.e(
                                TAG,
                                "Exception: "
                                        + exception.getMessage()
                                        + ", error code: "
                                        + exception.getErrorCode());
                    }
                });

        // assert the inserted data has been modified per the updateRecords.
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);

        // assert the inserted data has been modified by reading the data.
        // TODO(b/260181009): Modify and complete Tests for Update API in HealthConnectManagerTest
        //  using read API

        assertThat(responseException.get()).isNull();
    }

    /**
     * Test to verify the working of {@link
     * android.healthconnect.HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, while updating provide input with a few invalid
     * records. These records will have UUIDs that are not present in the table. Since this record
     * won't be updated, the transaction should fail and revert and no other record(even though
     * valid inputs) should not be modified either.
     */
    @Test
    public void testUpdateRecords_invalidInputRecords_noChangeInDataBase()
            throws InterruptedException, InvocationTargetException, InstantiationException,
                    IllegalAccessException, NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<HealthConnectException> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = insertRecords(getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords(/* isSetClientRecordId */ false);

        // Modify the Uid of the updateRecords to the UUID that was present in the insert records,
        // leaving out alternate records so that they have a new UUID which is not present in the
        // dataBase.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords
                    .get(itr)
                    .getMetadata()
                    .setId(
                            itr % 2 == 0
                                    ? insertRecords.get(itr).getMetadata().getId()
                                    : String.valueOf(Math.random()));
        }

        // perform the update operation.
        service.updateRecords(
                updateRecords,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<Void, HealthConnectException>() {
                    @Override
                    public void onResult(Void result) {}

                    @Override
                    public void onError(HealthConnectException exception) {
                        responseException.set(exception);
                        latch.countDown();
                        Log.e(
                                TAG,
                                "Exception: "
                                        + exception.getMessage()
                                        + ", error code: "
                                        + exception.getErrorCode());
                    }
                });

        assertThat(latch.await(/* timeout */ 3, TimeUnit.SECONDS)).isEqualTo(true);

        // assert the inserted data has not been modified by reading the data.
        // TODO(b/260181009): Modify and complete Tests for Update API in HealthConnectManagerTest
        //  using read API

        // verify that the testcase failed due to invalid argument exception.
        assertThat(responseException.get()).isNotNull();
        assertThat(responseException.get().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    /**
     * Test to verify the working of {@link
     * android.healthconnect.HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, while updating add an input record with an
     * invalid packageName. Since this is an invalid record the transaction should fail and revert
     * and no other record(even though valid inputs) should not be modified either.
     */
    @Test
    public void testUpdateRecords_recordWithInvalidPackageName_noChangeInDataBase()
            throws InterruptedException, InvocationTargetException, InstantiationException,
                    IllegalAccessException, NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<Exception> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = insertRecords(getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords(/* isSetClientRecordId */ false);

        // Modify the Uuid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords
                    .get(itr)
                    .getMetadata()
                    .setId(insertRecords.get(itr).getMetadata().getId());

            //             adding an entry with invalid packageName.
            if (updateRecords.get(itr).getRecordType() == RECORD_TYPE_STEPS) {
                updateRecords.set(
                        itr, getStepsRecord(false, /* incorrectPackageName */ "abc.xyz.pqr"));
            }
        }

        try {
            // perform the update operation.
            service.updateRecords(
                    updateRecords,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<Void, HealthConnectException>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(HealthConnectException exception) {
                            responseException.set(exception);
                            latch.countDown();
                            Log.e(
                                    TAG,
                                    "Exception: "
                                            + exception.getMessage()
                                            + ", error code: "
                                            + exception.getErrorCode());
                        }
                    });

        } catch (Exception exception) {
            latch.countDown();
            responseException.set(exception);
        }
        assertThat(latch.await(/* timeout */ 3, TimeUnit.SECONDS)).isEqualTo(true);

        // assert the inserted data has not been modified by reading the data.
        // TODO(b/260181009): Modify and complete Tests for Update API in HealthConnectManagerTest
        //  using read API

        // verify that the testcase failed due to invalid argument exception.
        assertThat(responseException.get()).isNotNull();
        assertThat(responseException.get().getClass()).isEqualTo(IllegalArgumentException.class);
    }

    private List<Record> insertRecords(List<Record> records) throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<List<Record>> response = new AtomicReference<>();
        service.insertRecords(
                records,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<InsertRecordsResponse, HealthConnectException>() {
                    @Override
                    public void onResult(InsertRecordsResponse result) {
                        response.set(result.getRecords());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(response.get()).hasSize(records.size());
        return response.get();
    }

    private void testRead_StepsRecordIds() throws InterruptedException {
        List<Record> recordList = Arrays.asList(getStepsRecord(), getStepsRecord());
        List<Record> insertedRecords = insertRecords(recordList);
        readStepsRecordUsingIds(insertedRecords);
    }

    private void testRead_StepsRecordClientIds() throws InterruptedException {
        List<Record> recordList = Arrays.asList(getStepsRecord(), getStepsRecord());
        List<Record> insertedRecords = insertRecords(recordList);
        readStepsRecordUsingClientId(insertedRecords);
    }

    private void testRead_HeartRateRecord() throws InterruptedException {
        List<Record> recordList = Arrays.asList(getHeartRateRecord(), getHeartRateRecord());
        readHeartRateRecordUsingIds(recordList);
    }

    private void testRead_BasalMetabolicRateRecord() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getBasalMetabolicRateRecord(), getBasalMetabolicRateRecord());
        readBasalMetabolicRateRecordUsingIds(recordList);
    }

    private void readStepsRecordUsingIds(List<Record> insertedRecord) throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class);
        for (Record record : insertedRecord) {
            request.addId(record.getMetadata().getId());
        }
        List<StepsRecord> result = readRecords(request.build());
        verifyStepsRecordReadResults(insertedRecord, result);
    }

    private void readStepsRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<StepsRecord> result = readRecords(request.build());
        verifyStepsRecordReadResults(insertedRecord, result);
    }

    private void verifyStepsRecordReadResults(
            List<Record> insertedRecords, List<StepsRecord> readResult) {
        assertThat(readResult).hasSize(insertedRecords.size());
        for (int i = 0; i < readResult.size(); i++) {
            StepsRecord stepsRecord = readResult.get(i);
            StepsRecord input = (StepsRecord) insertedRecords.get(i);
            assertThat(stepsRecord.getRecordType()).isEqualTo(input.getRecordType());
            assertThat(stepsRecord.getMetadata().getDevice().getManufacturer())
                    .isEqualTo(input.getMetadata().getDevice().getManufacturer());
            assertThat(stepsRecord.getMetadata().getDevice().getModel())
                    .isEqualTo(input.getMetadata().getDevice().getModel());
            assertThat(stepsRecord.getMetadata().getDevice().getType())
                    .isEqualTo(input.getMetadata().getDevice().getType());
            assertThat(stepsRecord.getMetadata().getDataOrigin().getPackageName())
                    .isEqualTo(input.getMetadata().getDataOrigin().getPackageName());
            assertThat(stepsRecord.getCount()).isEqualTo(input.getCount());
        }
    }

    private void readHeartRateRecordUsingIds(List<Record> recordList) throws InterruptedException {
        List<Record> insertedRecords = insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<HeartRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HeartRateRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<HeartRateRecord> result = readRecords(request.build());
        verifyHeartRateRecordReadResults(insertedRecords, result);
    }

    private void verifyHeartRateRecordReadResults(
            List<Record> insertedRecords, List<HeartRateRecord> readResult) {
        assertThat(readResult).hasSize(insertedRecords.size());
        for (int i = 0; i < readResult.size(); i++) {
            HeartRateRecord heartRateRecord = readResult.get(i);
            HeartRateRecord input = (HeartRateRecord) insertedRecords.get(i);
            assertThat(heartRateRecord.getRecordType()).isEqualTo(input.getRecordType());
            assertThat(heartRateRecord.getMetadata().getDevice().getManufacturer())
                    .isEqualTo(input.getMetadata().getDevice().getManufacturer());
            assertThat(heartRateRecord.getMetadata().getDevice().getModel())
                    .isEqualTo(input.getMetadata().getDevice().getModel());
            assertThat(heartRateRecord.getMetadata().getDevice().getType())
                    .isEqualTo(input.getMetadata().getDevice().getType());
            assertThat(heartRateRecord.getMetadata().getDataOrigin().getPackageName())
                    .isEqualTo(input.getMetadata().getDataOrigin().getPackageName());
        }
    }

    private void readBasalMetabolicRateRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<BasalMetabolicRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<BasalMetabolicRateRecord> result = readRecords(request.build());
        verifyBMRRecordReadResults(insertedRecords, result);
    }

    private void verifyBMRRecordReadResults(
            List<Record> insertedRecords, List<BasalMetabolicRateRecord> readResult) {
        assertThat(readResult).hasSize(insertedRecords.size());
        for (int i = 0; i < readResult.size(); i++) {
            BasalMetabolicRateRecord bmrRecord = readResult.get(i);
            BasalMetabolicRateRecord input = (BasalMetabolicRateRecord) insertedRecords.get(i);
            assertThat(bmrRecord.getRecordType()).isEqualTo(input.getRecordType());
            assertThat(bmrRecord.getMetadata().getDevice().getManufacturer())
                    .isEqualTo(input.getMetadata().getDevice().getManufacturer());
            assertThat(bmrRecord.getMetadata().getDevice().getModel())
                    .isEqualTo(input.getMetadata().getDevice().getModel());
            assertThat(bmrRecord.getMetadata().getDevice().getType())
                    .isEqualTo(input.getMetadata().getDevice().getType());
            assertThat(bmrRecord.getMetadata().getDataOrigin().getPackageName())
                    .isEqualTo(input.getMetadata().getDataOrigin().getPackageName());
            assertThat(bmrRecord.getBasalMetabolicRate().getInWatts())
                    .isEqualTo(input.getBasalMetabolicRate().getInWatts());
        }
    }

    private <T extends Record> List<T> readRecords(ReadRecordsRequestUsingIds<T> request)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(service).isNotNull();
        AtomicReference<List<T>> response = new AtomicReference<>();
        service.readRecords(
                request,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException>() {
                    @Override
                    public void onResult(ReadRecordsResponse<T> result) {
                        response.set(result.getRecords());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        return response.get();
    }

    private <T extends Record> List<T> readRecords(ReadRecordsRequestUsingFilters<T> request)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(service).isNotNull();
        AtomicReference<List<T>> response = new AtomicReference<>();
        service.readRecords(
                request,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException>() {
                    @Override
                    public void onResult(ReadRecordsResponse<T> result) {
                        response.set(result.getRecords());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        return response.get();
    }

    private List<Record> getTestRecords() {
        return getTestRecords(/* isSetClientRecordId */ true);
    }

    private List<Record> getTestRecords(boolean isSetClientRecordId) {
        return Arrays.asList(
                getStepsRecord(isSetClientRecordId, ""),
                getHeartRateRecord(isSetClientRecordId),
                getBasalMetabolicRateRecord(isSetClientRecordId));
    }

    private List<RecordAndIdentifier> getRecordsAndIdentifiers() {
        return Arrays.asList(
                new RecordAndIdentifier(RECORD_TYPE_STEPS, getStepsRecord()),
                new RecordAndIdentifier(RECORD_TYPE_HEART_RATE, getHeartRateRecord()),
                new RecordAndIdentifier(
                        RECORD_TYPE_BASAL_METABOLIC_RATE, getBasalMetabolicRateRecord()));
    }

    private StepsRecord getStepsRecord() {
        return getStepsRecord(true, "");
    }

    private StepsRecord getStepsRecord(boolean isSetClientRecordId, String packageName) {
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder()
                        .setPackageName(
                                packageName.isEmpty() ? "android.healthconnect.cts" : packageName)
                        .build();

        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (isSetClientRecordId) {
            testMetadataBuilder.setClientRecordId("SR" + Math.random());
        }
        return new StepsRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Instant.now(), 10)
                .build();
    }

    private HeartRateRecord getHeartRateRecord() {
        return getHeartRateRecord(true);
    }

    private HeartRateRecord getHeartRateRecord(boolean isSetClientRecordId) {
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(72, Instant.now());
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (isSetClientRecordId) {
            testMetadataBuilder.setClientRecordId("HR" + Math.random());
        }
        return new HeartRateRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Instant.now(), heartRateSamples)
                .build();
    }

    private BasalMetabolicRateRecord getBasalMetabolicRateRecord() {
        return getBasalMetabolicRateRecord(true);
    }

    private BasalMetabolicRateRecord getBasalMetabolicRateRecord(boolean isSetClientRecordId) {
        Device device =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel4a")
                        .setType(2)
                        .build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (isSetClientRecordId) {
            testMetadataBuilder.setClientRecordId("BMR" + Math.random());
        }
        return new BasalMetabolicRateRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Power.fromWatts(100.0))
                .build();
    }
}
