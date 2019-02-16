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
package org.structr.core.graph;

import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.Transaction;
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.api.service.Command;
import org.structr.api.service.SingletonService;
import org.structr.api.service.StructrServices;
import org.structr.api.util.CountResult;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;

/**
 * The graph/node service.
 */
public class NodeService implements SingletonService {

	private static final Logger logger   = LoggerFactory.getLogger(NodeService.class.getName());
	private DatabaseService graphDb      = null;
	private Index<Node> nodeIndex        = null;
	private Index<Relationship> relIndex = null;
	private String filesPath             = null;
	private boolean isInitialized        = false;
	private CountResult initialCount     = null;

	@Override
	public void injectArguments(Command command) {

		if (command != null) {

			command.setArgument("graphDb",           graphDb);
			command.setArgument("nodeIndex",         nodeIndex);
			command.setArgument("relationshipIndex", relIndex);
			command.setArgument("filesPath",         filesPath);
		}
	}

	@Override
	public boolean initialize(final StructrServices services) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final String databaseDriver = Settings.DatabaseDriver.getValue();
		graphDb = (DatabaseService)Class.forName(databaseDriver).newInstance();
		if (graphDb != null) {

			if (graphDb.initialize()) {

				filesPath = Settings.FilesPath.getValue();

				// check existence of files path
				File files = new File(filesPath);
				if (!files.exists()) {

					files.mkdir();
				}

				logger.info("Database driver loaded, initializing indexes..");

				// index creation transaction
				try ( final Transaction tx = graphDb.beginTx() ) {

					nodeIndex = graphDb.nodeIndex();
					relIndex  = graphDb.relationshipIndex();

					tx.success();

					// if the above operations fail, the database is probably not available
					isInitialized = true;

					logger.info("Indexes successfully initialized.");

				} catch (Throwable t) {

					logger.warn("Error while initializing indexes: {}", t.getMessage());
				}
			}
		}

		return isInitialized;
	}

	@Override
	public void initialized() {

		// check for empty database and seed file
		String basePath = Settings.getBasePath();

		if (StringUtils.isEmpty(basePath)) {
			basePath = ".";
		}

		// don't check cache sizes when testing..
		if (!Services.isTesting()) {
			checkCacheSizes();
		}
	}

	@Override
	public void shutdown() {

		if (isRunning()) {

			logger.info("Shutting down graph database service");
			graphDb.shutdown();

			graphDb       = null;
			isInitialized = false;
		}
	}

	@Override
	public String getName() {
		return NodeService.class.getSimpleName();
	}

	public DatabaseService getGraphDb() {
		return graphDb;
	}

	@Override
	public boolean isRunning() {

		return ((graphDb != null) && isInitialized);
	}

	@Override
	public boolean isVital() {
		return true;
	}

	@Override
	public int getRetryDelay() {
		return Settings.NodeServiceStartTimeout.getValue();
	}

	@Override
	public int getRetryCount() {
		return Settings.NodeServiceStartRetries.getValue();
	}

	@Override
	public boolean waitAndRetry() {
		return true;
	}

	public Index<Node> getNodeIndex() {
		return nodeIndex;
	}

	public Index<Relationship> getRelationshipIndex() {
		return relIndex;
	}

	public CountResult getInitialCounts() {

		if (initialCount == null) {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				initialCount = graphDb.getNodeAndRelationshipCount();
				tx.success();

			} catch (Throwable t) {
				logger.warn("Unable to count number of nodes and relationships: {}", t.getMessage());
			}
		}

		return initialCount;
	}

	public CountResult getCurrentCounts() {
		return graphDb.getNodeAndRelationshipCount();
	}

	public void createAdminUser() {

		if (!Services.isTesting()) {

			// do two very quick count queries to determine the number of Structr nodes in the database
			final CountResult count           = getInitialCounts();
			final long nodeCount              = count.getNodeCount();
			final boolean hasApplicationNodes = nodeCount > 0;

			if (!hasApplicationNodes) {

				logger.info("Creating initial user..");

				final Class userType = StructrApp.getConfiguration().getNodeEntityClass("User");
				if (userType != null) {

					final PropertyKey<Boolean> isAdminKey = StructrApp.key(userType, "isAdmin");
					final PropertyKey<String> passwordKey = StructrApp.key(userType, "password");
					final PropertyKey<String> nameKey     = StructrApp.key(userType, "name");
					final App app                         = StructrApp.getInstance();

					try (final Tx tx = app.tx()) {

						app.create(userType,
							new NodeAttribute<>(nameKey,     "admin"),
							new NodeAttribute<>(passwordKey, "admin"),
							new NodeAttribute<>(isAdminKey,  true)
						);

						tx.success();

					} catch (Throwable t) {
						logger.warn("Unable to count number of nodes and relationships: {}", t.getMessage());
					}
				}
			}
		}
	}

	// ----- private methods -----
	private void checkCacheSizes() {

		final CountResult counts = getInitialCounts();
		final long nodeCacheSize = Settings.NodeCacheSize.getValue();
		final long relCacheSize  = Settings.RelationshipCacheSize.getValue();
		final long nodeCount     = counts.getNodeCount();
		final long relCount      = counts.getRelationshipCount();

		logger.info("Database contains {} nodes, {} relationships.", nodeCount, relCount);

		if (nodeCacheSize < nodeCount) {
			logger.warn("Insufficient node cache size detected, please set application.cache.node.size to at least {} for best performance.", nodeCount);
		}

		if (relCacheSize < relCount) {
			logger.warn("Insufficient relationship cache size detected, please set application.cache.relationship.size to at least {} for best performance.", relCount);
		}

	}
	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "core";
	}
}
