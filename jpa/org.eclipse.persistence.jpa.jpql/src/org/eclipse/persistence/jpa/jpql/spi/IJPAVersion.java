/*******************************************************************************
 * Copyright (c) 2006, 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation
 *
 ******************************************************************************/
package org.eclipse.persistence.jpa.jpql.spi;

import java.util.Arrays;
import java.util.List;

/**
 * An enumeration listing the various released versions of the Java Persistence specification.
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
public enum IJPAVersion {

	/**
	 * The constant for the default version used by the parsing system, which is always the latest
	 * version of the Java Persistence functional specification.
	 */
	DEFAULT_VERSION(2.0),

	/**
	 * The constant for the Java Persistence version 1.0.
	 */
	VERSION_1_0(1.0),

	/**
	 * The constant for the Java Persistence version 2.0.
	 */
	VERSION_2_0(2.0);

	/**
	 * The real version number.
	 */
	private double version;

	/**
	 * Creates a new <code>IJPAVersion</code>.
	 *
	 * @param version The actual version number
	 */
	private IJPAVersion(double version) {
		this.version = version;
	}

	/**
	 * Retrieves the enumeration constant for the given value. If the value is not known, then
	 * {@link #DEFAULT_VERSION} will be returned.
	 *
	 * @param value The value to retrieve its constant version
	 * @return The constant version of the given value
	 */
	public static IJPAVersion value(String value) {
		for (IJPAVersion version : versions()) {
			if (version.getVersion().equals(value)) {
				return version;
			}
		}
		return DEFAULT_VERSION;
	}

	/**
	 * Returns the list of {@link IJPAVersion IJPAVersions} excluding {@link #DEFAULT_VERSION}.
	 *
	 * @return The list of unique constants
	 */
	public static IJPAVersion[] versions() {
		List<IJPAVersion> values = Arrays.asList(values());
		values.remove(DEFAULT_VERSION);
		return values.toArray(new IJPAVersion[values.size()]);
	}

	/**
	 * Returns the real version this constant represents.
	 *
	 * @return The string value of the version
	 */
	public String getVersion() {
		return String.valueOf(version);
	}

	/**
	 * Determines whether this constant represents a version that is newer than the given version.
	 *
	 * @param version The constant to verify if it's representing a version that is older than this one
	 * @return <code>true</code> if this constant represents a newer version and the given constant
	 * represents a version that is older; <code>false</code> if the given constant represents a
	 * newer and this constant represents an older version
	 */
	public boolean isNewerThan(IJPAVersion version) {
		return this.version > version.version;
	}

	/**
	 * Determines whether this constant represents a version that is newer than the given version or
	 * if it's the same version.
	 *
	 * @param version The constant to verify if it's representing a version that is older than this
	 * one or if it's the same than this one
	 * @return <code>true</code> if this constant represents a newer version and the given constant
	 * represents a version that is older or if it's the same constant; <code>false</code> if the
	 * given constant represents a newer and this constant represents an older version
	 */
	public boolean isNewerThanOrEqual(IJPAVersion version) {
		return this.version >= version.version;
	}

	/**
	 * Determines whether this constant represents a version that is older than the given version.
	 *
	 * @param jpaVersion The constant to verify if it's representing a version that is more recent
	 * than this one
	 * @return <code>true</code> if this constant represents an earlier version and the given
	 * constant represents a version that is more recent; <code>false</code> if the given constant
	 * represents an earlier version and this constant represents a more recent version
	 */
	public boolean isOlderThan(IJPAVersion version) {
		return this.version < version.version;
	}

	/**
	 * Determines whether this constant represents a version that is older than the given version or
	 * if it's the same version.
	 *
	 * @param version The constant to verify if it's representing a version that is more recent than
	 * this one or if it's the same than this one
	 * @return <code>true</code> if this constant represents an earlier version and the given
	 * constant represents a version that is more recent or if it's the same constant; <code>false</code>
	 * if the given constant represents an earlier version and this constant represents a more recent
	 * version
	 */
	public boolean isOlderThanOrEqual(IJPAVersion version) {
		return this.version <= version.version;
	}

	/**
	 * Converts the current constant to one of the known versions, this means if the constant is
	 * {@link #DEFAULT_VERSION}, then it will be converted into the actual constant representing that
	 * version.
	 *
	 * @return Either this same constant or the actual version constant
	 */
	public IJPAVersion toCurrentVersion() {

		if (this == DEFAULT_VERSION) {

			String currentVersion = getVersion();

			for (IJPAVersion version : versions()) {
				if (currentVersion.equals(version.version)) {
					return version;
				}
			}
		}

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getVersion();
	}
}