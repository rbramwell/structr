/**
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.javaparser.entity.relation;

import org.structr.core.entity.OneToOne;

/**
 *
 *
 */
public class PackageSibling extends OneToOne<org.structr.javaparser.entity.Package, org.structr.javaparser.entity.Package> {

	@Override
	public Class<org.structr.javaparser.entity.Package> getSourceType() {
		return org.structr.javaparser.entity.Package.class;
	}

	@Override
	public Class<org.structr.javaparser.entity.Package> getTargetType() {
		return org.structr.javaparser.entity.Package.class;
	}

	@Override
	public String name() {
		return "CONTAINS_NEXT_SIBLING";
	}
}
