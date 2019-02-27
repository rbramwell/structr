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
package org.structr.memory;

import java.util.HashMap;
import java.util.Iterator;
import org.structr.memory.index.MemoryRelationshipIndex;
import org.structr.memory.index.MemoryNodeIndex;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.structr.api.AbstractDatabaseService;
import org.structr.api.DatabaseFeature;
import static org.structr.api.DatabaseFeature.LargeStringIndexing;
import static org.structr.api.DatabaseFeature.QueryLanguage;
import org.structr.api.NativeQuery;
import org.structr.api.NotInTransactionException;
import org.structr.api.Transaction;
import org.structr.api.graph.Direction;
import org.structr.api.graph.GraphProperties;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.api.util.CountResult;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.Iterables;
import org.structr.api.util.NodeWithOwnerResult;
import org.structr.memory.index.filter.Filter;
import org.structr.memory.index.filter.MemoryLabelFilter;
import org.structr.memory.index.filter.SourceNodeFilter;
import org.structr.memory.index.filter.TargetNodeFilter;

/**
 */
public class MemoryDatabaseService extends AbstractDatabaseService implements GraphProperties {

	private static final Map<String, RelationshipType> relTypeCache     = new ConcurrentHashMap<>();
	private static final Map<String, Object> graphProperties            = new HashMap<>();
	private static final ThreadLocal<MemoryTransaction> transactions    = new ThreadLocal<>();
	private final MemoryRelationshipRepository relationships            = new MemoryRelationshipRepository();
	private final MemoryNodeRepository nodes                            = new MemoryNodeRepository();
	private MemoryRelationshipIndex relIndex                            = null;
	private MemoryNodeIndex nodeIndex                                   = null;

	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public void shutdown() {
	}

	@Override
	public void clearCaches() {
	}

	@Override
	public void cleanDatabase() {

		nodes.clear();
		relationships.clear();
	}

	@Override
	public Transaction beginTx() {

		MemoryTransaction tx = transactions.get();
		if (tx == null) {

			tx = new MemoryTransaction(this);
			transactions.set(tx);
		}

		return tx;
	}

	@Override
	public Node createNode(final String type, final Set<String> labels, final Map<String, Object> properties) {

		final MemoryTransaction tx = getCurrentTransaction();
		final MemoryIdentity id   = new MemoryIdentity(type);
		final MemoryNode newNode  = new MemoryNode(this, id);

		// base type is always a label
		newNode.addLabel(type, false);

		// add labels
		if (labels != null) {

			for (final String label : labels) {
				newNode.addLabel(label, false);
			}
		}

		tx.create(newNode);

		newNode.setProperties(properties);

		return newNode;
	}

	@Override
	public NodeWithOwnerResult createNodeWithOwner(final Identity ownerId, final String type, final Set<String> labels, final Map<String, Object> nodeProperties, final Map<String, Object> ownsProperties, final Map<String, Object> securityProperties) {

		final Node newNode         = createNode(type, labels, nodeProperties);
		final Node owner           = getNodeById(ownerId);

		final Relationship ownsRelationship = createRelationship((MemoryNode)owner, (MemoryNode)newNode, forName(RelationshipType.class, "OWNS"));
		ownsRelationship.setProperties(ownsProperties);

		final Relationship securityRelationship = createRelationship((MemoryNode)owner, (MemoryNode)newNode, forName(RelationshipType.class, "SECURITY"));
		securityRelationship.setProperties(securityProperties);

		return new NodeWithOwnerResult(newNode, securityRelationship, ownsRelationship);
	}

	@Override
	public Node getNodeById(final Identity id) {

		final MemoryTransaction tx = getCurrentTransaction();

		return tx.getNodeById((MemoryIdentity)id);
	}

	@Override
	public Relationship getRelationshipById(final Identity id) {

		final MemoryTransaction tx = getCurrentTransaction();

		return tx.getRelationshipById((MemoryIdentity)id);
	}

	@Override
	public Iterable<Node> getAllNodes() {
		return Iterables.map(n -> n, getFilteredNodes(null));
	}

	@Override
	public Iterable<Node> getNodesByLabel(final String label) {

		if (label == null) {
			return getAllNodes();
		}

		return Iterables.map(n -> n, getFilteredNodes(new MemoryLabelFilter<>(label)));
	}

	@Override
	public Iterable<Node> getNodesByTypeProperty(final String type) {
		return getNodesByLabel(type);
	}

	@Override
	public void deleteNodesByLabel(final String label) {

		final MemoryTransaction tx = getCurrentTransaction();
		for (final Node node : getNodesByLabel(label)) {

			tx.delete((MemoryNode)node);
		}
	}

	@Override
	public Iterable<Relationship> getAllRelationships() {
		return Iterables.map(r -> r, getFilteredRelationships(null));
	}

	@Override
	public Iterable<Relationship> getRelationshipsByType(final String type) {

		if (type == null) {
			return getAllRelationships();
		}

		return Iterables.map(r -> r, Iterables.filter(n -> type.equals(n.getType().name()), getFilteredRelationships(new MemoryLabelFilter<>(type))));
	}

	@Override
	public GraphProperties getGlobalProperties() {
		return this;
	}

	@Override
	public Index<Node> nodeIndex() {

		if (nodeIndex == null) {

			nodeIndex = new MemoryNodeIndex(this);
		}

		return nodeIndex;
	}

	@Override
	public Index<Relationship> relationshipIndex() {

		if (relIndex == null) {

			relIndex = new MemoryRelationshipIndex(this);
		}

		return relIndex;
	}

	@Override
	public void updateIndexConfiguration(final Map<String, Map<String, Boolean>> schemaIndexConfig, final Map<String, Map<String, Boolean>> removedClasses) {
	}

	@Override
	public CountResult getNodeAndRelationshipCount() {
		final MemoryTransaction tx = getCurrentTransaction();
		return new CountResult(Iterables.count(tx.getNodes(null)), Iterables.count(tx.getRelationships(null)));
	}

	@Override
	public <T> T execute(final NativeQuery<T> nativeQuery) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public <T> NativeQuery<T> query(final Object query, final Class<T> resultType) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setProperty(String name, Object value) {
		graphProperties.put(name, value);
	}

	@Override
	public Object getProperty(String name) {
		return graphProperties.get(name);
	}

	public Iterable<MemoryNode> getFilteredNodes(final Filter<MemoryNode> filter) {

		return new LazyAccessor<>(() -> {

			final MemoryTransaction tx = getCurrentTransaction();
			return Iterables.map(n -> n, tx.getNodes(filter));
		});
	}

	public Iterable<MemoryRelationship> getFilteredRelationships(final Filter<MemoryRelationship> filter) {

		return new LazyAccessor<>(() -> {

			final MemoryTransaction tx = getCurrentTransaction();
			return Iterables.map(n -> n, tx.getRelationships(filter));

		});
	}

	@Override
	public boolean supportsFeature(final DatabaseFeature feature, final Object... parameters) {

		switch (feature) {

			case LargeStringIndexing:
				return true;

			case QueryLanguage:
				return false;
		}

		return false;
	}

	// ----- graph repository methods -----
	public Relationship createRelationship(final MemoryNode sourceNode, final MemoryNode targetNode, final RelationshipType relType) {

		sourceNode.lock();
		targetNode.lock();

		final MemoryTransaction tx               = getCurrentTransaction();
		final MemoryIdentity id                  = new MemoryIdentity(relType.name());
		final MemoryRelationship newRelationship = new MemoryRelationship(this, id, relType, (MemoryIdentity)sourceNode.getId(), (MemoryIdentity)targetNode.getId());

		// base type is always a label
		newRelationship.addLabel(relType.name());

		tx.create(newRelationship);

		return newRelationship;
	}

	public Iterable<Relationship> getRelationships(final MemoryNode node) {

		final MemoryTransaction tx = getCurrentTransaction();
		final MemoryIdentity id = node.getIdentity();

		return Iterables.map(n -> n, Iterables.filter(r -> (id.equals(r.getSourceNodeIdentity()) || id.equals(r.getTargetNodeIdentity())), tx.getRelationships(null)));
	}

	public Iterable<Relationship> getRelationships(final MemoryNode node, final Direction direction) {

		final MemoryTransaction tx = getCurrentTransaction();
		final MemoryIdentity id = node.getIdentity();

		switch (direction) {

			case BOTH:
				return getRelationships(node);

			case INCOMING:
				return Iterables.map(n -> n, Iterables.filter(r -> (id.equals(r.getTargetNodeIdentity())), tx.getRelationships(null)));

			case OUTGOING:
				return Iterables.map(n -> n, Iterables.filter(r -> (id.equals(r.getSourceNodeIdentity())), tx.getRelationships(null)));
		}

		return null;
	}

	public Iterable<Relationship> getRelationships(final MemoryNode node, final Direction direction, final RelationshipType relationshipType) {

		final MemoryTransaction tx              = getCurrentTransaction();
		final MemoryIdentity id                 = node.getIdentity();
		final String relType                    = relationshipType.name();

		switch (direction) {

			case BOTH:
				return getRelationships(node);

			case INCOMING:
				return Iterables.map(n -> n, Iterables.filter(r -> (r.getType().name().equals(relType) && id.equals(r.getTargetNodeIdentity())), tx.getRelationships(new TargetNodeFilter<>(id))));

			case OUTGOING:
				return Iterables.map(n -> n, Iterables.filter(r -> (r.getType().name().equals(relType) && id.equals(r.getSourceNodeIdentity())), tx.getRelationships(new SourceNodeFilter<>(id))));
		}

		return null;
	}

	public void delete(final MemoryNode node) {

		final MemoryTransaction tx = getCurrentTransaction();
		final MemoryIdentity id    = node.getIdentity();

		tx.delete(node);

		// remove relationships as well
		for (final Iterator<MemoryRelationship> it = tx.getRelationships(null).iterator(); it.hasNext();) {

			final MemoryRelationship rel = it.next();

			if (id.equals(rel.getSourceNodeIdentity()) || id.equals(rel.getTargetNodeIdentity())) {

				tx.delete(rel);
			}
		}
	}

	public void delete(final MemoryRelationship rel) {

		final MemoryTransaction tx = getCurrentTransaction();
		tx.delete(rel);
	}

	MemoryTransaction getCurrentTransaction() throws NotInTransactionException {
		return getCurrentTransaction(true);
	}

	MemoryTransaction getCurrentTransaction(final boolean reportNotInTransaction) throws NotInTransactionException {

		final MemoryTransaction tx = transactions.get();
		if (tx == null && reportNotInTransaction) {

			throw new NotInTransactionException("Not in transaction.");
		}

		return tx;
	}

	synchronized void commitTransaction(final Map<MemoryIdentity, MemoryNode> newNodes, final Map<MemoryIdentity, MemoryRelationship> newRelationships, Set<MemoryIdentity> deletedNodes, Map<MemoryIdentity, MemoryRelationship> deletedRelationships) {

		nodes.remove(deletedNodes);
		nodes.add(newNodes);

		relationships.remove(deletedRelationships);
		relationships.add(newRelationships);

		transactions.remove();
	}

	void rollbackTransaction() {
		transactions.remove();
	}

	Iterable<MemoryNode> getNodes(final Filter<MemoryNode> filter) {
		return nodes.values(filter);
	}

	Iterable<MemoryRelationship> getRelationships(final Filter<MemoryRelationship> filter) {
		return relationships.values(filter);
	}

	MemoryNode getNodeFromRepository(final MemoryIdentity id) {
		return nodes.get(id);
	}

	MemoryRelationship getRelationshipFromRepository(final MemoryIdentity id) {
		return relationships.get(id);
	}

	boolean exists(final MemoryIdentity id) {
		return nodes.contains(id) || relationships.contains(id);
	}

	void updateCache(final MemoryNode node) {
		nodes.updateCache(node);
	}

	void updateCache(final MemoryRelationship relationship) {
		relationships.updateCache(relationship);
	}

	// ----- nested classes -----
	private class LazyAccessor<T> implements Iterable<T> {

		private Accessor<Iterable<T>> accessor = null;

		public LazyAccessor(final Accessor<Iterable<T>> source) {
			this.accessor = source;
		}

		@Override
		public Iterator<T> iterator() {
			return accessor.get().iterator();
		}
	}

	@FunctionalInterface
	private interface Accessor<T> {
		T get();
	}
}