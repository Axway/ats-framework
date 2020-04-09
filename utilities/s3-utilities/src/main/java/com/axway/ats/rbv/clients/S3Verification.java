/*
 * Copyright 2017 Axway Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.axway.ats.rbv.clients;

import java.util.Date;
import java.util.List;

import com.axway.ats.action.s3.S3ObjectInfo;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.clients.VerificationSkeleton;
import com.axway.ats.rbv.executors.MetaExecutor;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.s3.S3MetaData;
import com.axway.ats.rbv.s3.S3SearchTerm;
import com.axway.ats.rbv.s3.S3Storage;
import com.axway.ats.rbv.s3.rules.FileFolderS3Rule;
import com.axway.ats.rbv.s3.rules.FileMd5S3Rule;
import com.axway.ats.rbv.s3.rules.FileModtimeS3Rule;
import com.axway.ats.rbv.s3.rules.FileSizeS3Rule;

/**
 *
 * S3 storage verification client.
 * Could be used for Amazon S3 and compatible storages.
 * For examples and details refer to ATS framework guide
 * <a href="https://axway.github.io/ats-framework/Common-test-verifications.html">here</a>
 *
 */
@PublicAtsApi
public class S3Verification extends VerificationSkeleton {

    private String monitorName = "s3_monitor_";

    /**
     * Construct S3 object verification client to Amazon S3 storage.
     * For custom endpoint use the other constructor at
     * {@link #S3Verification(String, String, String, String, String, String, boolean)}
     * @param accessKey credentials
     * @param secretKey credentials
     * @param bucketName name of the bucket to be used
     * @param directory base directory. Should not start with slash and root bucket directory is just empty string
     * @param objectName object name/key to be found
     * @param recursive should search for match be done recursively
     */
    @PublicAtsApi
    public S3Verification( String accessKey, String secretKey,  String bucketName, String directory, String objectName,
                           boolean recursive ) {

        this(null, accessKey, secretKey, bucketName, directory, objectName, recursive);
    }

    /**
     * Construct S3 object verification client to custom endpoint, Amazon-S3 compliant endpoint
     * @param endpoint Custom endpoint with protocol (http or https ), hostname and port.
     * @param accessKey credentials
     * @param secretKey credentials
     * @param bucketName name of the bucket to be used
     * @param directory base directory. Should not start with slash and root bucket directory is just empty string
     * @param fileName object name/key to be found
     * @param recursive should search for match be done recursively
     */
    @PublicAtsApi
    public S3Verification( String endpoint, String accessKey, String secretKey, String bucketName,
                           String directory, String fileName, boolean recursive) {

        this(endpoint, accessKey, secretKey, null, bucketName, directory, fileName, recursive);
    }

    /**
     * Construct S3 object verification client to custom endpoint, Amazon-S3 compliant endpoint
     * @param endpoint Custom endpoint with protocol (http or https ), hostname and port.
     * @param accessKey credentials
     * @param secretKey credentials
     * @param region name of the S3 storage region like
     *           <a href="http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">these</a> Amazon ones.
     *           Null means default region.
     * @param bucketName name of the bucket to be used
     * @param directory base directory. Should not start with slash and root bucket directory is just empty string
     * @param fileName object name/key to be found
     * @param recursive should search for match be done recursively
     */
    @PublicAtsApi
    public S3Verification( String endpoint, String accessKey, String secretKey, String region,
                           String bucketName, String directory, String fileName, boolean recursive) {

        super();
        this.monitorName += accessKey;

        S3Storage storage = new S3Storage();
        folder = storage.getFolder(new S3SearchTerm(endpoint, accessKey, secretKey, region, bucketName,
                                                    directory, fileName, recursive));
        this.executor = new MetaExecutor();
    }

    @PublicAtsApi
    public void checkSize( long size ) {

        FileSizeS3Rule rule = new FileSizeS3Rule(size, "checkSize", true);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public void checkSizeDifferent( long size ) {

        FileSizeS3Rule rule = new FileSizeS3Rule(size, "checkSizeDifferent", false);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public void checkModificationTime( long modTime ) {

        FileModtimeS3Rule rule = new FileModtimeS3Rule(modTime, "checkModificationTime", true);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public void checkModificationTimeDifferent( long modTime ) {

        FileModtimeS3Rule rule = new FileModtimeS3Rule(modTime, "checkModificationTimeDifferent", false);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public void checkMd5( String md5 ) {

        FileMd5S3Rule rule = new FileMd5S3Rule(md5, "checkMd5", true);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public void checkMd5Different( String md5 ) {

        FileMd5S3Rule rule = new FileMd5S3Rule(md5, "checkMd5Different", false);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public S3ObjectInfo[] verifyObjectExists() throws RbvException {

        addFileCheckRule();

        List<MetaData> matchedMetaData = verifyExists();

        S3ObjectInfo[] matchedS3Objects = new S3ObjectInfo[matchedMetaData.size()];
        for (int i = 0; i < matchedMetaData.size(); i++) {
            S3ObjectInfo newMatchedObject = new S3ObjectInfo();
            matchedS3Objects[i] = newMatchedObject;
            MetaData currentMetaData = matchedMetaData.get(i);
            newMatchedObject.setBucketName((String) currentMetaData.getProperty(S3MetaData.BUCKET_NAME));
            newMatchedObject.setName((String) currentMetaData.getProperty(S3MetaData.FILE_NAME));
            newMatchedObject.setSize((Long) currentMetaData.getProperty(S3MetaData.SIZE));
            newMatchedObject.setMd5((String) currentMetaData.getProperty(S3MetaData.MD5));
            newMatchedObject.setLastModified((Date) currentMetaData.getProperty(S3MetaData.LAST_MODIFIED));
        }

        return matchedS3Objects;
    }

    @PublicAtsApi
    public S3ObjectInfo[] verifyObjectAlwaysExists() throws RbvException {

        addFileCheckRule();

        List<MetaData> matchedMetaData = verifyAlwaysExists();

        S3ObjectInfo[] matchedS3Objects = new S3ObjectInfo[matchedMetaData.size()];
        for (int i = 0; i < matchedMetaData.size(); i++) {
            S3ObjectInfo newMatchedObject = new S3ObjectInfo();
            matchedS3Objects[i] = newMatchedObject;
            MetaData currentMetaData = matchedMetaData.get(i);
            newMatchedObject.setBucketName((String) currentMetaData.getProperty(S3MetaData.BUCKET_NAME));
            newMatchedObject.setName((String) currentMetaData.getProperty(S3MetaData.FILE_NAME));
            newMatchedObject.setSize((Long) currentMetaData.getProperty(S3MetaData.SIZE));
            newMatchedObject.setMd5((String) currentMetaData.getProperty(S3MetaData.MD5));
            newMatchedObject.setLastModified((Date) currentMetaData.getProperty(S3MetaData.LAST_MODIFIED));
        }

        return matchedS3Objects;
    }

    @PublicAtsApi
    public void verifyObjectNeverExist() throws RbvException {

        addFileCheckRule();

        verifyNeverExists();
    }


    @PublicAtsApi
    public void verifyObjectDoesNotExist() throws RbvException {

        addFileCheckRule();

        verifyDoesNotExist();
    }


    private void addFileCheckRule() {

        // set the second highest priority for this rule - if the file path is correct the second most
        // important thing is to check if the entity is a file
        FileFolderS3Rule rule = new FileFolderS3Rule(true, "checkIsFile", true, Integer.MIN_VALUE);
        rootRule.addRule(rule);
    }

    @Override
    protected String getMonitorName() {

        return monitorName;
    }

}
