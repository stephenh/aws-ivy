/*
 * Copyright 2004-2007 the original author or authors.
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
package org.springframework.aws.ivy;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ivy.plugins.repository.Resource;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;

/**
 * A Resource implementation that extracts its data from an S3 resource.
 * 
 * @author Ben Hale
 */
public class S3Resource implements Resource {

	private S3Service service;

	private S3Bucket bucket;

	private String key;

	private boolean exists;

	private long contentLength;

	private long lastModified;

	private String name;

	public S3Resource(S3Service service, String uri) {
		this.service = service;
		initializeS3(uri);
		initalizeResource();
	}

	public Resource clone(String newUri) {
		return new S3Resource(service, newUri);
	}

	public boolean exists() {
		return exists;
	}

	public long getContentLength() {
		return contentLength;
	}

	public long getLastModified() {
		return lastModified;
	}

	public String getName() {
		return name;
	}

	public boolean isLocal() {
		return false;
	}

	public InputStream openStream() throws IOException {
		try {
			return service.getObject(bucket, key).getDataInputStream();
		}
		catch (S3ServiceException e) {
			throw new S3RepositoryException(e);
		}
	}

	private void initializeS3(String uri) {
		this.bucket = S3Utils.getBucket(uri);
		this.key = S3Utils.getKey(uri);
	}

	private void initalizeResource() {
		try {
			S3Object details = service.getObjectDetails(bucket, key);

			this.exists = true;
			this.contentLength = details.getContentLength();
			this.lastModified = details.getLastModifiedDate().getTime();
			this.name = "s3://" + details.getBucketName() + "/" + details.getKey();
		}
		catch (S3ServiceException e) {
			this.exists = false;
			this.contentLength = 0;
			this.lastModified = 0;
			this.name = "";
		}
	}

	@Override
	public String toString() {
		return name;
	}
}
