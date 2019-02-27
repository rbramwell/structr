/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.memory.index.factory;

import java.util.HashMap;
import java.util.Map;
import org.structr.api.index.AbstractIndex;
import org.structr.api.index.AbstractQueryFactory;

import org.structr.api.search.QueryPredicate;
import org.structr.memory.index.MemoryQuery;
import org.structr.memory.index.predicate.NullPredicate;
import org.structr.memory.index.predicate.ValuePredicate;
import org.structr.memory.index.predicate.StringContainsPredicate;

public class KeywordQueryFactory extends AbstractQueryFactory<MemoryQuery> {

	protected static final Map<Character, String> SPECIAL_CHARS = new HashMap<>();

	static {

		SPECIAL_CHARS.put('+', "\\");
		SPECIAL_CHARS.put('-', "\\");
		SPECIAL_CHARS.put('*', ".");
		SPECIAL_CHARS.put('?', ".");
		SPECIAL_CHARS.put('~', "\\");
		SPECIAL_CHARS.put('.', "\\");
		SPECIAL_CHARS.put('(', "\\");
		SPECIAL_CHARS.put(')', "\\");
		SPECIAL_CHARS.put('{', "\\");
		SPECIAL_CHARS.put('}', "\\");
		SPECIAL_CHARS.put('[', "\\");
		SPECIAL_CHARS.put(']', "\\");
		SPECIAL_CHARS.put(':', "\\");
		SPECIAL_CHARS.put('^', "\\");
		SPECIAL_CHARS.put('&', "\\");
		SPECIAL_CHARS.put('|', "\\");
	}

	public KeywordQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final MemoryQuery query, final boolean isFirst) {

		final boolean isString = predicate.getType().equals(String.class);
		final Object value     = getReadValue(predicate.getValue());
		final String name      = predicate.getName();

		checkOccur(query, predicate.getOccurrence(), isFirst);

		// only String properties can be used for inexact search
		if (predicate.isExactMatch() || !isString) {

			if (isString && value == null) {

				// special handling for string attributes
				// (empty string is equal to null)
				query.beginGroup();
				query.addPredicate(new NullPredicate(name));
				query.or();
				query.addPredicate(new ValuePredicate(name, ""));
				query.endGroup();

			} else {

				query.addPredicate(new ValuePredicate(name, value));
			}

		} else {

			if (value != null && isString) {

				query.addPredicate(new StringContainsPredicate(name, (String)value));

			} else {

				query.beginGroup();
				query.addPredicate(new NullPredicate(name));
				query.or();
				query.addPredicate(new ValuePredicate(name, ""));
				query.endGroup();
			}
		}

		return true;
	}
}