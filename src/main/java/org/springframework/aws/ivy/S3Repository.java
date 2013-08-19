/*
 * Copyright 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aws.ivy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.plugins.repository.AbstractRepository;
import org.apache.ivy.plugins.repository.RepositoryCopyProgressListener;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.util.FileUtil;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * A repository the allows you to upload and download from an S3 repository.
 * 
 * @author Ben Hale
 */
public class S3Repository extends AbstractRepository {

	private String accessKey;
	private String secretKey;
	private AmazonS3 service;
	private CannedAccessControlList acl = CannedAccessControlList.PublicRead;
	// when doing an s3ls in list(), we remember the S3Resource so we later don't have to s3cat it in getResource()
	private Map<String, S3Resource> resourceCache = new HashMap<String, S3Resource>();

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public void setAcl(String acl) {
		if ("PRIVATE".equals(acl)) {
			this.acl = CannedAccessControlList.Private;
		} else if ("PUBLIC_READ".equals(acl)) {
			this.acl = CannedAccessControlList.PublicRead;
		} else if ("PUBLIC_READ_WRITE".equals(acl)) {
			this.acl = CannedAccessControlList.PublicReadWrite;
		} else if ("AUTHENTICATED_READ".equals(acl)) {
			this.acl = CannedAccessControlList.AuthenticatedRead;
		} else {
			throw new IllegalArgumentException("Unknown acl " + acl);
		}
	}

	public void get(String source, File destination) throws IOException {
		Resource resource = getResource(source);
		try {
			fireTransferInitiated(resource, TransferEvent.REQUEST_GET);
			RepositoryCopyProgressListener progressListener = new RepositoryCopyProgressListener(this);
			progressListener.setTotalLength(resource.getContentLength());
			FileUtil.copy(resource.openStream(), new FileOutputStream(destination), progressListener);
		}
		catch (IOException e) {
			fireTransferError(e);
			throw e;
		}
		catch (RuntimeException e) {
			fireTransferError(e);
			throw e;
		}
		finally {
			fireTransferCompleted(resource.getContentLength());
		}
	}

	public Resource getResource(String source) throws IOException {
		if (!resourceCache.containsKey(source)) {
			resourceCache.put(source, new S3Resource(getService(), source));
		}
		return resourceCache.get(source);
	}

	public List<String> list(String parent) throws IOException {
		String bucket = S3Utils.getBucket(parent);
		String key = S3Utils.getKey(parent);
		String marker = null;
		List<String> keys = new ArrayList<String>();
		do {
			ObjectListing objects = getService().listObjects(new ListObjectsRequest()
				.withBucketName(bucket)
				.withPrefix(key)
				.withMarker(marker));
			for (S3ObjectSummary summary : objects.getObjectSummaries()) {
				String uri = "s3://" + bucket + "/" + summary.getKey();
				keys.add(uri);
				resourceCache.put(uri, new S3Resource(service, summary));
			}
			marker = objects.getNextMarker();
		} while (marker != null);
		return keys;
	}

	@Override
	protected void put(File source, String destination, boolean overwrite) throws IOException {
		String bucket = S3Utils.getBucket(destination);
		String key = S3Utils.getKey(destination);
		getService().putObject(new PutObjectRequest(bucket, key, source).withCannedAcl(acl));
	}

	private AmazonS3 getService() {
		if (service == null) {
			service = new AmazonS3Client(getCredentials());
		}
		return service;
	}

	private AWSCredentials getCredentials() {
		if (accessKey.length() > 0 && secretKey.length() > 0) {
			return new BasicAWSCredentials(accessKey, secretKey);
		}
		return null;
	}

}
