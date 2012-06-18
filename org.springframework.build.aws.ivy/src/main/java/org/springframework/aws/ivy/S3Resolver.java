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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ivy.plugins.resolver.RepositoryResolver;
import org.apache.ivy.util.Message;

/**
 * A dependency resolver that looks to an S3 repository to resolve dependencies.
 * 
 * @author Ben Hale
 */
public class S3Resolver extends RepositoryResolver {
	
	static {
		Logger.getLogger("org.jets3t").setLevel(Level.OFF);
	}
	
	public void setAccessKey(String accessKey) {
		Message.debug("S3Resolver using accessKey " + accessKey);
		((S3Repository)getRepository()).setAccessKey(accessKey);
	}
	
	public void setSecretKey(String secretKey) {
		((S3Repository)getRepository()).setSecretKey(secretKey);
	}

	public S3Resolver() {
		setRepository(new S3Repository());
	}

	public String getTypeName() {
		return "S3";
	}
}
