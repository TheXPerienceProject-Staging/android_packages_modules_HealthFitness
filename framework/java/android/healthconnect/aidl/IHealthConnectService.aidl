package android.healthconnect.aidl;

import android.healthconnect.HealthConnectDataState;
import android.healthconnect.aidl.ActivityDatesRequestParcel;
import android.healthconnect.aidl.AggregateDataRequestParcel;
import android.healthconnect.aidl.IAggregateRecordsResponseCallback;
import android.healthconnect.aidl.ChangeLogTokenRequestParcel;
import android.healthconnect.aidl.ChangeLogsRequestParcel;
import android.healthconnect.aidl.DeleteUsingFiltersRequestParcel;
import android.healthconnect.aidl.IAccessLogsResponseCallback;
import android.healthconnect.aidl.IChangeLogsResponseCallback;
import android.healthconnect.aidl.IDataStagingFinishedCallback;
import android.healthconnect.aidl.IEmptyResponseCallback;
import android.healthconnect.aidl.IGetChangeLogTokenCallback;
import android.healthconnect.aidl.IGetHealthConnectDataStateCallback;
import android.healthconnect.aidl.IGetPriorityResponseCallback;
import android.healthconnect.aidl.RecordsParcel;
import android.healthconnect.aidl.IMigrationCallback;
import android.healthconnect.aidl.IApplicationInfoResponseCallback;
import android.healthconnect.aidl.IEmptyResponseCallback;
import android.healthconnect.aidl.IInsertRecordsResponseCallback;
import android.healthconnect.aidl.RecordsParcel;
import android.healthconnect.aidl.UpdatePriorityRequestParcel;
import android.healthconnect.aidl.IReadRecordsResponseCallback;
import android.healthconnect.aidl.IActivityDatesResponseCallback;
import android.healthconnect.aidl.IRecordTypeInfoResponseCallback;
import android.healthconnect.aidl.ReadRecordsRequestParcel;
import android.healthconnect.migration.MigrationEntity;
import android.healthconnect.restore.StageRemoteDataRequest;

import android.os.UserHandle;

import java.util.List;

/**
 * Interface for {@link com.android.healthconnect.HealthConnectManager}
 * {@hide}
 */
interface IHealthConnectService {
    void grantHealthPermission(String packageName, String permissionName, in UserHandle user);
    void revokeHealthPermission(String packageName, String permissionName, String reason, in UserHandle user);
    void revokeAllHealthPermissions(String packageName, String reason, in UserHandle user);
    List<String> getGrantedHealthPermissions(String packageName, in UserHandle user);

    /* @hide */
    long getHistoricalAccessStartDateInMilliseconds(String packageName, in UserHandle user);

    /**
     * Inserts {@code records} into the HealthConnect database.
     *
     * @param packageName name of the package inserting the record.
     * @param recordsParcel represents records to be inserted.
     * @param callback Callback to receive result of performing this operation.
     */
    void insertRecords(
        String packageName,
        in RecordsParcel recordsParcel,
        in IInsertRecordsResponseCallback callback);

    /**
     * Returns aggregation results based on the {@code request} into the HealthConnect database.
     *
     * @param packageName name of the package querying aggregate.
     * @param request represents the request using which the aggregation is to be performed.
     * @param callback Callback to receive result of performing this operation.
     */
    void aggregateRecords(
        String packageName,
        in AggregateDataRequestParcel request,
        in IAggregateRecordsResponseCallback callback);

    /**
     * Reads from the HealthConnect database.
     *
     * @param packageName name of the package reading the record.
     * @param request represents the request to be read.
     * @param callback Callback to receive result of performing this operation.
     */
    void readRecords(
        in String packageName,
        in ReadRecordsRequestParcel request,
        in IReadRecordsResponseCallback callback);

    /**
     * Updates {@param records} in the HealthConnect database.
     *
     * @param packageName name of the package updating the record.
     * @param recordsParcel represents records to be updated.
     * @param callback Callback to receive result of performing this operation.
     */
    void updateRecords(
            String packageName,
            in RecordsParcel recordsParcel,
            in IEmptyResponseCallback callback);

    /**
     * @param packageName calling package name
     * @param request token request
     * @return a token that can be used with {@code getChanges(token)} to fetch the upsert and
     *     delete changes corresponding to {@code request}
     */
    void getChangeLogToken(
        String packageName,
        in ChangeLogTokenRequestParcel request,
        in IGetChangeLogTokenCallback callback);

    /**
     * @param packageName calling package name
     * @param token request token from {@code getChangeLogToken}
     */
    void getChangeLogs(
        String packageName,
        in ChangeLogsRequestParcel token,
        in IChangeLogsResponseCallback callback);

    /**
     * @param packageName Calling package's name
     * @param request Delete request using the mentioned filters
     * @param callback Callback to receive result of performing this operation
     */
    void deleteUsingFilters(
        String packageName,
        in DeleteUsingFiltersRequestParcel request,
        in IEmptyResponseCallback callback);

    /**
     * @param packageName Calling package's name
     * @param permissionCategory PermissionCategory corresponding to which priority is requested
     * @param callback Callback to receive result of performing this operation
     */
    void getCurrentPriority(
        String packageName,
        int permissionCategory,
        in IGetPriorityResponseCallback callback);

    /**
     * @param packageName Calling package's name
     * @param request Delete request using the mentioned filters
     * @param callback Callback to receive result of performing this operation
     */
    void updatePriority(
        String packageName,
        in UpdatePriorityRequestParcel request,
        in IEmptyResponseCallback callback);

    /** Sets record rention period for HC DB */
    void setRecordRetentionPeriodInDays(
        int days,
        in UserHandle userHandle,
        in IEmptyResponseCallback callback);

    /** Gets record rention period for HC DB */
    int getRecordRetentionPeriodInDays(in UserHandle userHandle);

    /**
     * Returns information, represented by {@code ApplicationInfoResponse}, for all the
     * packages that have contributed to the health connect DB.
     *
     * @param callback Callback to receive result of performing this operation.
     */
    void getContributorApplicationsInfo(in IApplicationInfoResponseCallback callback);

    /** Returns information for each RecordType like health permission category, record category and
     * contributing packages.
     * @param callback Callback to receive result of performing this operation.
     */
    void queryAllRecordTypesInfo(in IRecordTypeInfoResponseCallback callback);

    /**
     * @param packageName name of the package reading access logs
     * @param callback Callback to receive result of performing this operation
     */
    void queryAccessLogs(
        String packageName,
        in IAccessLogsResponseCallback callback);

    /**
     * Returns a list of unique dates for which at least one record type has at least one entry.
     *
     * @param recordTypes List of record types classes for which to get the activity dates.
     * @param callback Callback to receive the result of performing this operation.
     * {@hide}
     */
    void getActivityDates(
        in ActivityDatesRequestParcel recordTypes,
        in IActivityDatesResponseCallback callback);

    /**
     * Marks the start of the migration.
     *
     * @param callback Callback to receive a result or an error encountered while performing this
     * operation.
     */
    void startMigration(in IMigrationCallback callback);

    /**
     * Marks the end of the migration.
     *
     * @param callback Callback to receive a result or an error encountered while performing this
     * operation.
     */
    void finishMigration(in IMigrationCallback callback);

    /**
     * Writes given entities to the module database.
     *
     * @param entities List of {@link MigrationDataEntity} to migrate.
     * @param callback Callback to receive a result or an error encountered while performing this
     * operation.
     */
    void writeMigrationData(in List<MigrationEntity> entities, in IMigrationCallback callback);

    /**
     * Stages all HealthConnect remote data and returns any errors in a callback. Errors encountered
     * for all the files are shared in the provided callback.
     *
     * @param pfdsByFileName The map of file names and their {@link ParcelFileDescriptor}s.
     * @param executor       The {@link Executor} on which to invoke the callback.
     * @param callback       The callback which will receive the outcome of this call.
     * @throws NullPointerException if null is passed for any of the required {@link NonNull}
     *                              parameters.
     * @hide
     */
    void stageAllHealthConnectRemoteData(in StageRemoteDataRequest stageRemoteDataRequest,
            in UserHandle userHandle, in IDataStagingFinishedCallback callback);

    /**
     * Deletes all previously staged HealthConnect data from the disk.
     * For testing purposes only.
     *
     * @hide
     */
    void deleteAllStagedRemoteData(in UserHandle userHandle);

    /**
     * Updates the download state of the Health Connect data.
     *
     * @param downloadState The download state which needs to be purely one of:
     *                      {@link HealthConnectManager#CLOUD_DOWNLOAD_STARTED}, {@link
     *                      HealthConnectManager#CLOUD_DOWNLOAD_RETRY}, {@link
     *                      HealthConnectManager#CLOUD_DOWNLOAD_FAILED}, {@link
     *                      HealthConnectManager#CLOUD_DOWNLOAD_COMPLETE}
     * @hide
     */
     void updateDataDownloadState(int downloadState, in UserHandle userHandle);

    /**
     * Asynchronously returns the current state of the Health Connect data as it goes through the Data-Restore and/or the Data-Migration process.
     *
     * <p>See also {@link HealthConnectDataState} object describing the HealthConnect state.
     *
     * @param callback The callback which will receive the current {@link HealthConnectDataState}.
     *
     * @hide
     */
    void getHealthConnectDataState(in UserHandle userHandle, in IGetHealthConnectDataStateCallback callback);
}
