/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.messaging.engine.entities;

import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.messaging.engine.relation.MessageClientHASMessageSubscriber;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface MessageSubscriber extends NodeInterface {

    class Impl {
		static {

			final JsonSchema schema     = SchemaService.getDynamicSchema();
			final JsonObjectType type   = schema.addType("MessageSubscriber");

			type.setImplements(URI.create("https://structr.org/v1.1/definitions/MessageSubscriber"));

			type.addStringProperty("topic", PropertyView.Public, PropertyView.Ui).setIndexed(true);
			type.addStringProperty("callback", PropertyView.Public, PropertyView.Ui);

			type.addPropertyGetter("topic", String.class);
			type.addPropertyGetter("callback", String.class);
			type.addPropertyGetter("clients", List.class);
			
			type.overrideMethod("onCreation",     true, MessageSubscriber.class.getName() + ".onCreation(this, arg0, arg1);");
			type.overrideMethod("onModification", true, MessageSubscriber.class.getName() + ".onModification(this, arg0, arg1, arg2);");

			type.addMethod("onMessage")
					.setReturnType(RestMethodResult.class.getName())
					.addParameter("topic", String.class.getName())
					.addParameter("message", String.class.getName())
					.setSource("return " + MessageSubscriber.class.getName() + ".onMessage(this, topic, message, securityContext);")
					.addException(FrameworkException.class.getName())
					.setDoExport(true);


			type.addViewProperty(PropertyView.Public, "clients");
			type.addViewProperty(PropertyView.Ui,     "clients");
		}
	}


	String getTopic();
    String getCallback();
    List<MessageClient> getClients();

    static void subscribeOnAllClients(MessageSubscriber thisSubscriber) {

		if(!StringUtils.isEmpty(thisSubscriber.getTopic()) && (thisSubscriber.getTopic() != null)) {
			Map<String,Object> params = new HashMap<>();
			params.put("topic", thisSubscriber.getTopic());

			List<MessageClient> clientsList = thisSubscriber.getClients();
			clientsList.forEach(client -> {
				try {
					client.invokeMethod("subscribeTopic", params, false);
				} catch (FrameworkException e) {
					logger.error("Could not invoke subscribeTopic on MessageClient: " + e.getMessage());
				}
			});
		}
	}


    static void onCreation(MessageSubscriber thisSubscriber, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

    	if(thisSubscriber.getProperty(StructrApp.key(MessageSubscriber.class,"topic")) != null) {
			subscribeOnAllClients(thisSubscriber);
    	}

    }


	static void onModification(MessageSubscriber thisSubscriber, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

        if(modificationQueue.isPropertyModified(thisSubscriber, StructrApp.key(MessageSubscriber.class,"topic")) || modificationQueue.isPropertyModified(thisSubscriber, StructrApp.key(MessageSubscriber.class,"topic"))) {
			subscribeOnAllClients(thisSubscriber);
        }

    }


	static RestMethodResult onMessage(MessageSubscriber thisSubscriber, final String topic, final String message, SecurityContext securityContext) throws FrameworkException {

        if (!StringUtils.isEmpty(thisSubscriber.getCallback())) {

            String script = "${" + thisSubscriber.getCallback() + "}";

            Map<String, Object> params = new HashMap<>();
            params.put("topic", topic);
            params.put("message", message);

            ActionContext ac = new ActionContext(securityContext, params);
            Scripting.replaceVariables(ac, thisSubscriber, script);
        }

        return new RestMethodResult(200);
    }

}
