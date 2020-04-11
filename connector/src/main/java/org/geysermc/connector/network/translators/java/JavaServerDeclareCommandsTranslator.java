/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveice10.mc.protocol.data.game.command.CommandNode;
import com.github.steveice10.mc.protocol.data.game.command.CommandType;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDeclareCommandsPacket;
import com.nukkitx.protocol.bedrock.data.CommandData;
import com.nukkitx.protocol.bedrock.data.CommandEnumData;
import com.nukkitx.protocol.bedrock.data.CommandParamData;
import com.nukkitx.protocol.bedrock.packet.AvailableCommandsPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.FileUtils;

import java.io.IOException;
import java.util.*;

@Translator(packet = ServerDeclareCommandsPacket.class)
public class JavaServerDeclareCommandsTranslator extends PacketTranslator<ServerDeclareCommandsPacket> {
    @Override
    public void translate(ServerDeclareCommandsPacket packet, GeyserSession session) {
        GeyserConnector.getInstance().getLogger().info("ServerDeclareCommandsPacket");

        List<CommandNode> rootNodes = new ArrayList<>();
        List<CommandData> commandData = new ArrayList<>();
        Map<Integer, String> commands = new HashMap<>();
        Map<Integer, List<CommandNode>> commandArgs = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        for (CommandNode node : packet.getNodes()) {
            if (node.getType() == CommandType.ROOT) {
                rootNodes.add(node);
            }
        }

        for (CommandNode rootNode : rootNodes) {
            for (int nodeIndex : rootNode.getChildIndices()) {
                CommandNode node = packet.getNodes()[nodeIndex];

                if (node.getChildIndices().length >= 1) {
                    for (int childIndex : node.getChildIndices()) {
                        commandArgs.putIfAbsent(nodeIndex, new ArrayList<>());
                        commandArgs.get(nodeIndex).add(packet.getNodes()[childIndex]);
                    }
                }

                commands.put(nodeIndex, node.getName());
            }
        }



        /* try {
            GeyserConnector.getInstance().getLogger().debug(mapper.writeValueAsString(params));
        } catch (JsonProcessingException e) { } */

        List<CommandData.Flag> flags = new ArrayList<>();
        for (int commandID : commands.keySet()) {
            String commandName = commands.get(commandID);

            CommandEnumData aliases = new CommandEnumData( commandName + "Aliases", new String[] { commandName.toLowerCase() }, false);
            CommandParamData[][] params = new CommandParamData[0][0];

            if (commandArgs.containsKey(commandID)) {
                params = new CommandParamData[1][];
                CommandParamData[] param1 = new CommandParamData[commandArgs.get(commandID).size()];

                int i = 0;
                for (CommandNode paramNode : commandArgs.get(commandID)) {
                    param1[i] = new CommandParamData(paramNode.getName(), false, null, CommandParamData.Type.MESSAGE, null, Collections.emptyList());
                    i++;
                }

                params[0] = param1;
            }

            CommandData data = new CommandData(commandName, "A Java server command", flags, (byte) 0, aliases, params);
            commandData.add(data);
        }

        AvailableCommandsPacket availableCommandsPacket = new AvailableCommandsPacket();
        for (CommandData data : commandData) {
            availableCommandsPacket.getCommands().add(data);
        }

        session.getUpstream().sendPacket(availableCommandsPacket);
    }
}
